package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

public class ChannelSpecificConstraintManager implements IConstraintManager{
	
	private static Logger log = LoggerFactory.getLogger(ChannelSpecificConstraintManager.class);
	
	/*
	 * Map taking subject station to map taking channel to interfering station that cannot be
	 * on channel concurrently with subject station. 
	 */
	private final Map<Station,Map<Integer,Set<Station>>> fCOConstraints;
	
	/*
	 * Map taking subject station to map taking channel to interfering station that cannot be
	 * on channel+1 concurrently with subject station.
	 */
	private final Map<Station,Map<Integer,Set<Station>>> fADJp1Constraints;
	
	/**
	 * Type of possible constraints.
	 * @author afrechet
	 */
	private enum ConstraintKey
	{
		//Co-channel constraints,
		CO,
		//ADJ+1 channel constraints,
		ADJp1,
		//ADJ-1 channel constraints (should not appear in new format),
		ADJm1;
	}
	
	/**
	 * Add the constraint to the constraint manager represented by subject station, target station, subject channel and constraint key.
	 * @param aSubjectStation
	 * @param aTargetStation
	 * @param aSubjectChannel
	 * @param aConstraintKey
	 */
	private void addConstraint(Station aSubjectStation,
			Station aTargetStation,
			Integer aSubjectChannel,
			ConstraintKey aConstraintKey)
	{
		Map<Integer,Set<Station>> subjectStationConstraints;
		Set<Station> interferingStations;
		
		switch(aConstraintKey)
		{
			case CO:
				
				/*
				 * Switch subject station for target station depending on the ID of the stations 
				 * to remove possible duplicate CO interference clauses. 
				 */
				if(aSubjectStation.getID()>aTargetStation.getID())
				{
					Station tempStation = aSubjectStation;
					aSubjectStation = aTargetStation;
					aTargetStation = tempStation;
				}
				
				subjectStationConstraints = fCOConstraints.get(aSubjectStation);
				if(subjectStationConstraints == null)
				{
					subjectStationConstraints = new HashMap<Integer,Set<Station>>();
				}
				
				interferingStations = subjectStationConstraints.get(aSubjectChannel);
				if(interferingStations == null)
				{
					interferingStations = new HashSet<Station>();
				}
				
				interferingStations.add(aTargetStation);
				
				subjectStationConstraints.put(aSubjectChannel, interferingStations);
				fCOConstraints.put(aSubjectStation, subjectStationConstraints);
			break;
				
			case ADJp1:
				
				//Add CO constraints;
				addConstraint(aSubjectStation, aTargetStation, aSubjectChannel, ConstraintKey.CO);
				addConstraint(aSubjectStation, aTargetStation, aSubjectChannel+1, ConstraintKey.CO);
				
				//Add +1 constraint;
				subjectStationConstraints = fADJp1Constraints.get(aSubjectStation);
				if(subjectStationConstraints == null)
				{
					subjectStationConstraints = new HashMap<Integer,Set<Station>>();
				}
				
				interferingStations = subjectStationConstraints.get(aSubjectChannel);
				if(interferingStations == null)
				{
					interferingStations = new HashSet<Station>();
				}
				
				interferingStations.add(aTargetStation);
				
				subjectStationConstraints.put(aSubjectChannel, interferingStations);
				fADJp1Constraints.put(aSubjectStation, subjectStationConstraints);

			break;
			
			case ADJm1:
				//Add corresponding reverse ADJ+1 constraint.
				addConstraint(aTargetStation, aSubjectStation, aSubjectChannel-1, ConstraintKey.ADJp1);
			break;
			
			default:
				throw new IllegalStateException("Unrecognized constraint key "+aConstraintKey);
		}
	}
	
	/**
	 * Construct a Channel Specific Constraint Manager from a station manager and an interference constraints filename.
	 * @param aStationManager - station manager.
	 * @param aInterferenceConstraintsFilenamer - name of the file containing interference constraints.
	 * @throws FileNotFoundException - if indicated file cannot be found.
	 */
	public ChannelSpecificConstraintManager(IStationManager aStationManager, String aInterferenceConstraintsFilename) throws FileNotFoundException
	{
		fCOConstraints = new HashMap<Station,Map<Integer,Set<Station>>>();
		fADJp1Constraints = new HashMap<Station,Map<Integer,Set<Station>>>();
		
		try 
		{
			try(CSVReader reader = new CSVReader(new FileReader(aInterferenceConstraintsFilename)))
			{
				String[] line;
				while((line = reader.readNext())!=null)
				{
					String key = line[0].trim();
					ConstraintKey constraintKey;
					if(key.equals("CO"))
					{
						constraintKey = ConstraintKey.CO;
					}
					else if(key.equals("ADJ+1"))
					{
						constraintKey = ConstraintKey.ADJp1;
					}
					else if(key.equals("ADJ-1"))
					{
						constraintKey = ConstraintKey.ADJm1;
					}
					else
					{
						throw new IllegalArgumentException("Unrecognized constraint key "+key);
					}
					
					int lowChannel = Integer.valueOf(line[1].trim());
					int highChannel = Integer.valueOf(line[2].trim());
					if(lowChannel > highChannel)
					{
						throw new IllegalStateException("Low channel greater than high channel.");
					}
					
					int subjectStationID = Integer.valueOf(line[3].trim());
					Station subjectStation = aStationManager.getStationfromID(subjectStationID);
					
					for(int subjectChannel = lowChannel; subjectChannel<=highChannel;subjectChannel++)
					{
						for(int i=4;i<line.length;i++)
						{
							int targetStationID = Integer.valueOf(line[i].trim());
							Station targetStation = aStationManager.getStationfromID(targetStationID);
							
							addConstraint(subjectStation, targetStation, subjectChannel, constraintKey);
						}
					}
					
				}
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
			throw new IllegalArgumentException("Could not read interference constraints filename.");
		}
		
	}
	
	@Override
	public Boolean isSatisfyingAssignment(Map<Integer, Set<Station>> aAssignment) {
		
		Set<Station> allStations = new HashSet<Station>();
		
		for(Integer channel : aAssignment.keySet())
		{
			Set<Station> channelStations = aAssignment.get(channel);
			
			for(Station station1 : channelStations)
			{
				//Check if we have already seen station1
				if(allStations.contains(station1))
				{
					log.error("Station {} is assigned to multiple channels.");
					return false;
				}
				
				//Make sure current station does not CO interfere with other stations.
				Collection<Station> coInterferingStations = getCOInterferingStations(station1, channel);
				for(Station station2 : channelStations)
				{
					if(coInterferingStations.contains(station2))
					{
						log.error("Station {} and {} share channel {} on which they CO interfere.",station1,station2,channel);
						return false;
					}
				}
				
				//Make sure current station does not ADJ+1 interfere with other stations.
				Collection<Station> adjInterferingStations = getADJplusInterferingStations(station1, channel);
				int channelp1 = channel+1;
				Set<Station> channelp1Stations = aAssignment.get(channelp1);
				if(channelp1Stations!=null)
				{
					for(Station station2 : channelp1Stations)
					{
						if(adjInterferingStations.contains(station2))
						{
							log.error("Station {} is on channel {}, and station {} is on channel {}, causing ADJ+1 interference.",station1,channel,station2,channelp1);
							return false;
						}
					}
				}
			}
			allStations.addAll(channelStations);
		}
		return true;
	}

	@Override
	public Set<Station> getCOInterferingStations(Station aStation, int aChannel) {
		
		Map<Integer,Set<Station>> subjectStationConstraints = fCOConstraints.get(aStation);
		//No constraint for this station.
		if(subjectStationConstraints == null)
		{
			return new HashSet<Station>();
		}
		
		Set<Station> interferingStations = subjectStationConstraints.get(aChannel);
		//No constraint for this station on this channel.
		if(interferingStations == null)
		{
			return new HashSet<Station>();
		}
		return interferingStations;
	}

	@Override
	public Set<Station> getADJplusInterferingStations(Station aStation,
			int aChannel) {
		Map<Integer,Set<Station>> subjectStationConstraints = fADJp1Constraints.get(aStation);
		//No constraint for this station.
		if(subjectStationConstraints == null)
		{
			return new HashSet<Station>();
		}
		
		Set<Station> interferingStations = subjectStationConstraints.get(aChannel);
		//No constraint for this station on this channel.
		if(interferingStations == null)
		{
			return new HashSet<Station>();
		}
		return interferingStations;
	}

	

}
