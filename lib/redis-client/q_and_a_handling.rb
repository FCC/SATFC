# Simple module to read in question.csv and answer.csv files

module QAndAHandling
  
  require 'csv'
  
  Question = Struct.new(:station_config, :highest, :band, :assignment)
  Answer = Struct.new(:answer, :assignment)
  
  def load_file_with_assignment filename, sink, &block
    File.open(filename) do |file|
      file.each do |line|
        data = CSV.parse_line(line)
        key = data[0].strip
        val = data[1] && data[1].strip
        # let block try to handle data first
        if !block.call(key, val)
          if key.to_i > 0 
            # Looks like part of the assignment
            sink.assignment ||= {}
            sink.assignment[key.to_i] = val.to_i
          end
        end
      end
    end
  end
  
  def load_question question_fn
    q = Question.new

    load_file_with_assignment(question_fn, q) do |key, val|
      case key
      when /STATION_CONFIG/i
        q.station_config = val
      when /BAND/i
        # Note: HVHF is the correct name, but historically we have also used "UVHF".
        #       For now we allow either but at some point we should enforce "HVHF".
        q.band = {"LVHF" => 1, "UVHF" => 2, "HVHF" => 2, "UHF" => 3}[val.upcase]
      when /HIGHEST/i
        q.highest = val.to_i
      else
        false
      end
    end
    q
  end
  
  def load_answer answer_fn
    a = Answer.new
    load_file_with_assignment(answer_fn, a) do |key, val|
      case key
      when /ANSWER/i
        a.answer = val
      else
        false
      end
    end
    a
  end
  
  
  def write_question_file csv_file, timeout, fc_config_strings, station_data, band, highest_channel, tentative_assignment, new_station
    csv_file << [:FC_TIMEOUT, timeout]
    fc_config_strings.each do |config|
      csv_file << [:FC_CONFIG, config]
    end if fc_config_strings
    csv_file << [:STATION_CONFIG, station_data]
    

    csv_file << [:BAND, band]
    if band == "UHF"
      csv_file << [:HIGHEST, highest_channel]
    end
    
    tentative_assignment.each do |station, channel|
      csv_file << [station, channel]
    end
    csv_file << [new_station, -1]
  end
end
