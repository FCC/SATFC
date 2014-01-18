require 'redis'

# A proxy that retries Redis requests whenever there is a Errno::ECONNREFUSED
# error.
#
# Based on https://github.com/mrduncan/redis-retry/blob/master/lib/redis/retry.rb.
class RedisRetry < BasicObject
  # The number of times a command will be retried if a connection cannot be
  # made to Redis.  If zero, retry forever.
  attr_accessor :tries
  
  # The number of seconds to wait before retrying a command.
  attr_accessor :wait
  
  # A Proc invoked with self whenever a retry is required.
  attr_accessor :callback
  
  def initialize(options = {})
    @tries = options[:tries] || 0
    @wait  = options[:wait] || 5
    @callback = options[:callback]
    @raise_errno_econnrefused_without_retrying_again = false
    @redis = ::Redis.new( # ::Redis since we're in BasicObject.
      host: options[:host], port: options[:port]
    )
  end
  
  # Cause the next Redis call to *not* attempt to retry thereby
  # raising Errno::ECONNREFUSED on the next failed attempt.
  # When received from callback, raises Errno::ECONNREFUSED immediately.
  def raise_errno_econnrefused_without_retrying_again
    @raise_errno_econnrefused_without_retrying_again = true
  end
  
  def respond_to?(method, include_private = false)
    super || @redis.send(method, include_private)
  end
  
  def method_missing(method, *args, &block)    
    try = 0
    while true # since #loop method is not defined.
      begin
        return @redis.send(method, *args, &block)
      rescue ::Errno::ECONNREFUSED
        try += 1
        callback && callback[self]
        if @raise_errno_econnrefused_without_retrying_again ||
            !tries.zero? && try > tries
          @redis.send(:raise)
        end
        @redis.send(:sleep, @wait)
      end
    end
  end
end