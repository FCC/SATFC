# Abstraction for the Redis-based job multicaster.  Client code doesn't need to know about Redis.
#
# For the design see
#
# https://www.powerauctions.com/redmine/projects/fcctv/wiki/Design_for_fast_redis-based_messaging_for_SATFC
#
# The ladder/algo process sends problems to be solved, and workers pick up the problems

require_relative 'json_parsing'
require_relative 'redis_retry'
begin
  require_relative '../../tokens'
rescue LoadError # has alternative location in fcc-station-packing/lib/redis-client.
  require_relative 'tokens'
end

class JobCaster  
  include JsonParsing
  
  class ProblemSet
    attr_reader :band, :highest, :constraint_set, :fc_config, :fc_approach, :timeout,  :tentative_assignment, :testing_flag
    
    def initialize(band, highest, constraint_set, fc_config, fc_approach, timeout, tentative_assignment, testing_flag = nil)
      @band = band
      @highest = highest.to_i
      @constraint_set = constraint_set
      @fc_config = fc_config
      @fc_approach = fc_approach
      @timeout = timeout.to_i
      @tentative_assignment = tentative_assignment
      @testing_flag = testing_flag
    end
    
    def to_json(*a)
      {
        "json_class"   => self.class.name,
        "data"         => [band, highest, constraint_set, fc_config, fc_approach, timeout, tentative_assignment, testing_flag]
      }.to_json(*a)
    end

    def self.json_create(o)
      new(*o["data"])
    end
  end

  DEFAULT_REDIS_HOST = "localhost"
  DEFAULT_REDIS_PORT = 6379
  JOB_BLOCK_TIMEOUT = 5 # seconds
  CLIENT_STATUS_REPORT_INTERVAL = 5 # seconds
  CLIENT_STATUS_REPORT_EXPIRATION = 5 * 60 # seconds
  JOB_CLIENT_CPU_FACTOR = 2
  
  STOP_WAITING_REASONS = [:all_solved, :hard_timeout, :waited_after_backlog]
  
  attr_reader :host, :port, :redis
  
  def initialize job_caster_host, job_caster_port, options = {}
    password = Tokens::TOKEN
    
    @redis = RedisRetry.new(host: job_caster_host, port: job_caster_port,
      callback: options[:callback])
    begin
      @redis.auth(password)
    rescue
      raise unless $!.message == "ERR Client sent AUTH, but no password is set"
      specifics = if host == "localhost" || host == nil
        "If you are running a test, be sure start Redis using ./redis-server-with-auth from fcctv/trunk."
      else
        "If you are running against an EC2 broker, be sure that /etc/redis/redis.conf has requirepass set to #{Tokens::TOKEN}."
      end
      raise $!.class, "#{$!.message} on #{host}.  #{specifics}"
    end
    @redis
  end
  
  def alive?
    redis.ping == "PONG"
  end
  
  #################################################
  # Form and parse problem ids and problem_set_ids
  
  
  def self.problem_set_id_prefix ip_address, area_name, iteration, band_name
    "#{ip_address}-#{area_name}-#{iteration}-#{band_name}"
  end
  
  def self.parse_problem_set_id problem_set_id
    # Need to allow dashes in area names.
    problem_set_id =~ /([^-]*?)-(.*)-([^-]*?)-([^-]*?)-([^-]*?)/
    ip_address = $1
    area_name = $2
    iteration = $3
    band_name = $4
    sequence_value = $5
    [ip_address, area_name, iteration, band_name, sequence_value]
  end
  
  def self.problem_id station_id, problem_set_id
    "#{station_id}:#{problem_set_id}"
  end
  
  def self.parse_problem_id problem_id
    station, problem_set_id = problem_id.split(":")
    [station] + parse_problem_set_id(problem_set_id)
  end
  
  # End parsing
  ##############

  ###################################
  # Methods for the problem producer

  
  # Return the problem set id
  def send_problem_set problem_set_json, new_station_ids, uniq_id = "no_context"
    return nil if new_station_ids.empty?
    
    ps_id = get_new_problem_set_id uniq_id
    redis.pipelined {
      redis.set problem_set_key_for(ps_id), problem_set_json
      for s_id in new_station_ids
        redis.lpush jobs_list_key, JobCaster.problem_id(s_id, ps_id)
      end
    }
    ps_id
  end
  
  # Wait for the given problem set to be solved by the cloud.
  #
  # Arguments:
  #  problem_set_id     - the problem set to wait for
  #  expected_size      - how many problems are in the problem set
  #  timeout_s          - a hard cap on the amount of time to wait (in seconds)
  #  problem_timeout_ms - the per-problem timeout that the solvers are working with (in milliseconds)
  # 
  # We stop waiting when the first of these things happens:
  #
  # 1) all of the problems have been solved
  # 2) we have waited timeout_s seconds
  # 3) we have waited JOB_CLIENT_CPU_FACTOR * problem_timeout_ms after the point the last problem
  #    in the problem set was picked up by a solver.
  #
  # Returns <status>, <satfc_time>, <actual_time>, <answer_count>, <stop_reason>
  #
  # where
  #   <status> is :ok or :timeout
  #   <satfc_time> is the maximum time we expect could be spent in SATFC, in seconds
  #   <actual_time> is the actual time we waited, in seconds
  #   <answer_count> is the number of problems actually solved
  #   <stop_reason> is the reason we stopped waiting:
  #            - :all_solved            all the problems were solved
  #            - :waited_after_backlog  we waited JOB_CLIENT_CPU_FACTOR * problem_timeout_ms after the backlog cleared but not all problems were answered
  #            - :hard_timeout          we waited for the full timeout_s, but not all problems were answered.
  #
  # Note, we assume that all problems currently in the backlog have the same problem_timeout as this
  # problem set.
  def wait_for_problem_set problem_set_id, expected_size, timeout_s, problem_timeout_ms
    problem_timeout_s = problem_timeout_ms / 1000.0
    interval_s_to_check_backlog = problem_timeout_s / 5 < 1 ? 1 : 5
    time_to_wait_after_backlog_cleared = JOB_CLIENT_CPU_FACTOR * problem_timeout_s
    
    # Use maximum_satfc_time for reporting purposes only.
    client_count = get_client_count
    client_count = 1 if client_count == 0
    problems_per_client = 2 + (backlog_size / client_count) # +1 to round up the ratio, +1 for the problem the client is already working on.
    maximum_satfc_time = problem_timeout_s * problems_per_client
    
    start = Time.now
    timeout = start + timeout_s
    current_answer_count = 0
    time_when_all_jobs_should_have_finished = nil
    next_time_to_check_backlog = start + interval_s_to_check_backlog
    stop_reason = :hard_timeout
    until (now = Time.now) > timeout
      current_answer_count = redis.llen(answer_list_key_for(problem_set_id))
      if current_answer_count >= expected_size
        stop_reason = :all_solved
        break
      end
      
      if time_when_all_jobs_should_have_finished
        if now > time_when_all_jobs_should_have_finished
          stop_reason = :waited_after_backlog
          break
        end
      elsif now > next_time_to_check_backlog
        if problem_set_backlogged?(problem_set_id)
          next_time_to_check_backlog += interval_s_to_check_backlog
        else
          time_when_all_jobs_should_have_finished = now + time_to_wait_after_backlog_cleared
        end
      end
      
      if block_given?
        yield
      else
        sleep 0.05 # 50ms ping time is perfectly reasonable, Redis is fast.
      end
    end
    
    # Don't want to require must_be gem for try_job_client.
    #stop_reason.must_be_in(STOP_WAITING_REASONS)
    unless STOP_WAITING_REASONS.include?(stop_reason)
      raise "Invalid stop_reason #{stop_reason}.  Must be one of #{STOP_WAITING_REASONS}"
    end
    
    if current_answer_count == expected_size
      return [:ok, maximum_satfc_time, Time.now - start, current_answer_count, stop_reason]
    elsif current_answer_count > expected_size
      raise "more answers than questions!"
    else
      return [:timeout, maximum_satfc_time, Time.now - start, current_answer_count, stop_reason] 
    end
  end
  
  def get_answers_for problem_set_id
    redis.lrange answer_list_key_for(problem_set_id), 0, -1
  end
  
  def get_assignment_for station_id, problem_set_id
    redis.get assignment_key_for(problem_set_id, station_id)
  end
  
  # Once we are done with it we can delete the answer data
  def delete_answer_data_for problem_set_id
    # NOTE: this is strongly discouraged by the Redis documentation
    #
    #    http://redis.io/commands/keys
    #
    # Better is to remember these key names in a Redis set and retrieve
    # them now.  Then we just delete the keys and the set itself.
    #
    # However, since we never want more than a few hundred keys in Redis
    # at once, it turns out to not be a major cost.
    #
    keys = redis.keys assignment_key_for(problem_set_id, "*")
    keys <<
      answer_list_key_for(problem_set_id) <<
      problem_set_key_for(problem_set_id)
    
    redis.del *keys
  end
  
  def get_client_statuses
    client_ids = redis.smembers CLIENT_IDS_SET
    
    # TODO: put this loop back into a redis multi.  I tried it and started get weird
    #       errors.  Need to work out what I was doing wrong.
    statuses = []
    for id in client_ids
      statuses << redis.get(client_status_key_for(id))
    end
    statuses.compact.map{|v| json_parse(v)}.compact
  end
  
  def get_client_count
    keys = redis.keys(client_status_key_for("*"))
    (keys && keys.size) || 0
  end
  
  # End producer methods
  #######################
  
  ################################
  # Methods for problem consumers
  
  # Note that we pop from the right and push onto the left (above in send_problem_set).
  # This makes a queue rather than a stack.
  def block_for_job
    list_name, job = redis.brpop jobs_list_key, JOB_BLOCK_TIMEOUT
    job
  end
  
  def get_problem_set problem_set_id
    redis.get problem_set_key_for(problem_set_id)
  end 
  
  def send_assignment problem_set_id, new_station_id, witness_assignment_json
    redis.set assignment_key_for(problem_set_id, new_station_id), witness_assignment_json
  end
  
  def send_answer problem_set_id, answer_json
    redis.lpush answer_list_key_for(problem_set_id), answer_json 
  end
  
  def get_new_client_id
    get_new_seq_val(CLIENT_ID_SEQ)
  end
  
  def report_status client_id, status
    redis.pipelined do
      redis.sadd CLIENT_IDS_SET, client_id
      # Set the status and give it an expriation time.
      redis.set client_status_key_for(client_id), status.to_json 
      redis.expire client_status_key_for(client_id), CLIENT_STATUS_REPORT_EXPIRATION
    end
  end
  
  def report_error error_msg
    redis.lpush CLIENT_ERROR_KEY, error_msg
  end
  
  def client_errors limit = 1000
    redis.lrange(CLIENT_ERROR_KEY, 0, limit).map{|v| json_parse(v)}.compact
  end
  
  # List of problems in the queue.  Optionally filter by problem_set
  def backlog problem_set_id = nil
    backlog = redis.lrange(jobs_list_key, 0, -1)
    if problem_set_id
      backlog.select{|j| j.split(":")[1] == problem_set_id}
    else
      backlog
    end
  end

  # End consumer methods
  #######################
  
  private
  
  PROBLEM_SET_ID_SEQ = "problem_set_id_seq"
  CLIENT_ID_SEQ = "client_id_seq"
  CLIENT_IDS_SET = "client_ids"
  
  JOBS_LIST = "jobs"
  PROBLEM_SET_PREFIX = "problem_set"
  ANSWER_PREFIX = "answer"
  CLIENT_STATUS_PREFIX = "client.status"
  CLIENT_ERROR_KEY = "client_errors"
  
  def ensure_seq_key key
    if !redis.get(key)
      redis.set(key, 0)
    end
  end
  
  # Guaranteed to be unique for the Redis server
  def get_new_seq_val key
    ensure_seq_key(key)
    redis.multi do
      redis.incr(key)
      redis.get(key)
    end.last
  end
  
  def get_new_problem_set_id uniq_id
    "#{uniq_id}-#{get_new_seq_val(PROBLEM_SET_ID_SEQ)}"
  end
  
  def jobs_list_key
    JOBS_LIST
  end
  
  def problem_set_key_for problem_set_id
    "#{PROBLEM_SET_PREFIX}.#{problem_set_id}"
  end
  
  def answer_list_key_for problem_set_id
    "#{ANSWER_PREFIX}.#{problem_set_id}.answers"
  end
  
  def assignment_key_for problem_set_id, station_id
    "#{ANSWER_PREFIX}.#{problem_set_id}.#{station_id}"
  end
  
  def client_status_key_for client_id
    "#{CLIENT_STATUS_PREFIX}.#{client_id}"
  end
  
  def backlog_size problem_set_id = nil
    backlog(problem_set_id).size
  end
  
  # Are any jobs for the problem set still in the JOBS queue?
  def problem_set_backlogged? problem_set_id
    backlog_size(problem_set_id) > 0
  end

end