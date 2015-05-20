/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
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

/**
 * Parses smooth ladder simulator feasibility checking instances in "question" format.
 * @author afrechet
 */
@UsageTextField(title="FCC Station Packing Packing Question Options",description="Parameters necessary to define a station packing question.")
public class QuestionInstanceParameters extends AbstractOptions implements IInstanceParameters {
	
    /**
     * Station packing question built from a question filename.
     * @author afrechet
     */
	public static class StationPackingQuestion
	{
		/**
		 * Set of station IDs in instance.
		 */
		public final Set<Integer> fStationIDs;
		/**
		 * Set of channels to pack into.
		 */
		public final Set<Integer> fChannels;
		/**
		 * Valid previous assignment.
		 */
		public final Map<Integer,Integer> fPreviousAssignment;
		
		/**
		 * Interference data foldername.
		 */
		public final String fData;
		
		/**
		 * Instance cutoff time (s).
		 */
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
		
		/**
		 * Construct a station packing question from the corresponding question filename.
		 * @param aQuestion
		 */
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
	
    /**
     * Converts a string question filename to the corresponding station packing question.
     * @author afrechet
     */
    public static class StationPackingQuestionConverter implements IStringConverter<StationPackingQuestion>
    {
        @Override
        public StationPackingQuestion convert(String value) {
            if(value == null)
            {
                return null;
            }
            else
            {
                return new StationPackingQuestion(value);    
            }
        }
    }
	@Parameter(names = "-QUESTION", description = "Question file.",converter=StationPackingQuestionConverter.class)
	private StationPackingQuestion fQuestion; 

	
	@Override
    public String getInterferenceData()
	{
		return fQuestion.fData;
	}
	
	@Override
    public double getCutoff()
	{
		return fQuestion.fCutoff;
	}
	
	@Override
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
