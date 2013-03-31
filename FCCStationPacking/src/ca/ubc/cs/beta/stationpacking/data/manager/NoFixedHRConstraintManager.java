package ca.ubc.cs.beta.stationpacking.data.manager;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;

import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.StationChannelPair;

public class NoFixedHRConstraintManager implements IConstraintManager{
	
	Set<Constraint> fPairwiseConstraints;
	HashMap<Station,Set<Integer>> fStationDomains;
	
	public NoFixedHRConstraintManager(String aAllowedChannelsFilename, String aPairwiseConstraintsFilename, Set<Station> aFixedStations) throws IOException
	{
		fStationDomains = new HashMap<Station,Set<Integer>>();
		try(CSVReader aReader = new CSVReader(new FileReader(aAllowedChannelsFilename)))
		{
			//Skip header
			aReader.readNext();
			
			String[] aLine;
			while((aLine = aReader.readNext())!=null)
			{
				Integer aID = Integer.valueOf(aLine[1]);
				Station aStation = new Station(aID);
				
				if(!aFixedStations.contains(aStation))
				{
					HashSet<Integer> aChannelDomain = new HashSet<Integer>();
					for(int i=2;i<aLine.length;i++)
					{
						Integer aChannel = Integer.valueOf(aLine[i]);
						aChannelDomain.add(aChannel);
					}
					fStationDomains.put(aStation, aChannelDomain);
				}
			}
		}
		
		fPairwiseConstraints = new HashSet<Constraint>();
		
		try(CSVReader aReader = new CSVReader(new FileReader(aPairwiseConstraintsFilename)))
		{
			//Skip header
			aReader.readNext();
			
			String[] aLine;
			while((aLine = aReader.readNext())!=null)
			{
				Integer aID1 = Integer.valueOf(aLine[0]);
				Station aStation1 = new Station(aID1);
				Integer aChannel1 = Integer.valueOf(aLine[1]);
				
				Integer aID2 = Integer.valueOf(aLine[2]);
				Station aStation2 = new Station(aID2);
				Integer aChannel2 = Integer.valueOf(aLine[3]);
				
				if(aFixedStations.contains(aStation1))
				{	
					fStationDomains.get(aStation2).remove(aChannel2);
				}
				else if(aFixedStations.contains(aStation2))
				{
					fStationDomains.get(aStation1).remove(aChannel1);
				}
				else
				{
					fPairwiseConstraints.add(new Constraint(new StationChannelPair(aStation1,aChannel1),new StationChannelPair(aStation2,aChannel2)));
				}	
			}
		}
		
		//Proofread constraints
		Iterator<Constraint> aConstraintIterator = fPairwiseConstraints.iterator();
		while(aConstraintIterator.hasNext())
		{
			Constraint aConstraint = aConstraintIterator.next();
			StationChannelPair aProtectedPair = aConstraint.getProtectedPair();
			Station aProtectedStation = aProtectedPair.getStation();
			Integer aProtectedChannel = aProtectedPair.getChannel();
			
			StationChannelPair aInterferingPair = aConstraint.getInterferingPair();
			Station aInterferingStation = aInterferingPair.getStation();
			Integer aInterferingChannel = aInterferingPair.getChannel();
			
			if(!fStationDomains.get(aProtectedStation).contains(aProtectedChannel))
			{
				aConstraintIterator.remove();
			}
			else if(!fStationDomains.get(aInterferingStation).contains(aInterferingChannel))
			{
				aConstraintIterator.remove();
			}
		}
		
	}
	
	
	@Override
	public Set<Constraint> getPairwiseConstraints() {
		return fPairwiseConstraints;
	}

	@Override
	public Map<Station, Set<Integer>> getStationDomains() {
		return fStationDomains;
	}
}
