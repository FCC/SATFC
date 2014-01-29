# A module to add simple error-handling support to JSON functionality

module JsonParsing
  
  # Parse the given source using JSON.parse, handling any exception that may arise.
  #
  # The exception, backtrace, and source string are output to the logs.  Further handling
  # is controlled by the value of the :error_handling option.  Once :error_handling has been
  # removed from it, the opts hash is passed to JSON.parse.
  #
  # :return_nil [default]: simply return nil after the exception has been handled.
  # **ADD FURTHER FUNCTIONALITY AS NEEDED**
  def json_parse(source, opts = {})
    opts = opts.clone
    handling = opts[:error_handling] || :return_nil
    opts.delete(:error_handling)
    begin
      JSON.parse(source, opts)
    rescue Exception => e
      # we assume the including context can use the Log system
      error "json", ["Error parsing JSON: #{e.message}", *(e.backtrace), "Source: #{source}"]
      case handling
      when :return_nil
        nil
      else
        raise "Error in JSON parsing, but do not understand the error handling instructions #{handling || 'nil'}."
      end
    end
  end
  
end