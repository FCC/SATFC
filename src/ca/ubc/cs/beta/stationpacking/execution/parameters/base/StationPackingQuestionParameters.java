package ca.ubc.cs.beta.stationpacking.execution.parameters.base;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Question Options",description="Parameters necessary to define a station packing question.")
public class StationPackingQuestionParameters extends AbstractOptions {
	
	public static class StationPackingQuestionConverter implements IStringConverter<StationPackingQuestion>
	{
		@Override
		public StationPackingQuestion convert(String value) {
			return new StationPackingQuestion(value);
		}
	}
	
	public static class StationPackingQuestion
	{
		public final Set<Integer> fStationIDs;
		public final Set<Integer> fChannels;
		public final Map<Integer,Integer> fPreviousAssignment;
		
		public final String fData;
		
		public final double fCutoff;
		
		private boolean isInteger(String s)
		{
		    try { 
		        Integer.parseInt(s); 
		    } catch(NumberFormatException e) { 
		        return false; 
		    }
		    return true;
		}
		
		public StationPackingQuestion(String aQuestion)
		{
			Set<Integer> stationIDs = new HashSet<Integer>();
			
			String band = null;
			int highest = 51;
			
			Map<Integer,Integer> previousAssignment = new HashMap<Integer,Integer>();
			
			String data = "default";
			
			Double cutoff = null;
			
			try {
				for(String line : FileUtils.readLines(new File(aQuestion)))
				{
					String[] lineParts = line.trim().split(",");
					
					String key = lineParts[0];
					String value = lineParts[1];
					
					if(isInteger(key))
					{
						int station = Integer.valueOf(key);
						int prevchannel = Integer.valueOf(value);
						stationIDs.add(station);
						if(prevchannel >= 0)
						{
							previousAssignment.put(station, prevchannel);
						}
					}
					else
					{
						try
						{
							switch(key)
							{
							case("FC_TIMEOUT"):
								cutoff = Double.valueOf(value)/1000.0;
								break;
							case("STATION_CONFIG"):
								data = value;
								break;
							case("BAND"):
								band = value;
								break;
							case("HIGHEST"):
								highest = Integer.valueOf(value);
								break;
							default:
								System.out.println("Unused question file key "+key);
								break;
							}
						}
						catch(NumberFormatException e)
						{
							throw new IllegalArgumentException("Could not convert question file line "+line);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Could not read question from file "+aQuestion);
			}
			
			fStationIDs = stationIDs;
			
			if(band == null)
			{
				throw new IllegalArgumentException("Did not find BAND in provided question "+aQuestion);
			}
			switch(band)
			{
			case("LVHF"):
			case("1"):
				fChannels = new HashSet<Integer>(StationPackingUtils.LVHF_CHANNELS);
				break;
			case("2"):
			case("HVHF"):
				fChannels = new HashSet<Integer>(StationPackingUtils.HVHF_CHANNELS);
				break;
			case("3"):
			case("UHF"):
				fChannels = new HashSet<Integer>();
				for(Integer channel : StationPackingUtils.UHF_CHANNELS)
				{
					if(channel <= highest)
					{
						fChannels.add(channel);
					}
				}
				break;
			default:
				throw new IllegalArgumentException("Unrecognized BAND value "+band);
			}
			
			fPreviousAssignment = previousAssignment;
			
			fData = data;
			
			if(cutoff == null)
			{
				throw new IllegalArgumentException("Could not find FC_TIMEOUT in provided question "+aQuestion);
			}
			fCutoff = cutoff;
			
		}
	}
	
	
	@Parameter(names = "-QUESTION", description = "Question file.",required=true,converter=StationPackingQuestionConverter.class)
	private StationPackingQuestion fQuestion; 
	
	public String getData()
	{
		return fQuestion.fData;
	}
	
	public double getCutoff()
	{
		return fQuestion.fCutoff;
	}
	
	public StationPackingInstance getInstance(IStationManager aStationManager)
	{
		Map<Station,Integer> prevAssignment = new HashMap<Station,Integer>();
		Set<Station> stations = new HashSet<Station>();
		
		for(Integer stationID : fQuestion.fStationIDs)
		{
			Station station = aStationManager.getStationfromID(stationID);
			
			stations.add(station);
			
			if(fQuestion.fPreviousAssignment.containsKey(stationID))
			{
				prevAssignment.put(station, fQuestion.fPreviousAssignment.get(stationID));
			}
		}
		
		StationPackingInstance instance = StationPackingInstance.constructUniformDomainInstance(stations, fQuestion.fChannels, prevAssignment);
		
		return instance;
	}
	
	
	
	
	
	
	
}
