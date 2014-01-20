require 'socket'

module Tokens
  # SecureRandom.hex+SecureRandom.hex
  # For FCBroker / job caster
  TOKEN = "a052a4001fddb5f13686cfce7325e2b94a93061328c4abb1ac60d6463df1b377"
  @@job_caster_host = nil
  @@job_caster_port = nil
  
  def self.find_job_caster!
    return if @@job_caster_host && @@job_caster_port
    base_url = ENV['JOB_CASTER_URL']
    unless base_url
      raise %{You must export JOB_CASTER_URL from your environment.  If you're writing a local test or similar, then writing `ENV['JOB_CASTER_URL'] = "localhost:6379"` at the top may serve you well. Or you can invoke this way: \n\nJOB_CASTER_URL="localhost:6379" thin start\n\n.}
    end
    
    uri = URI.parse("redis://#{base_url}")
    @@job_caster_host = uri.host
    @@job_caster_port = uri.port || JobCaster::DEFAULT_REDIS_PORT
  end
  
  def self.job_caster_host
    find_job_caster!
    @@job_caster_host
  end
  
  def self.job_caster_port
    find_job_caster!
    @@job_caster_port
  end
    
  # For Web API
  API_TOKEN = "8cc283ce2005a86f2691624fd65a8ec90598e50fe59fb470eb2898d798bdba56"
end