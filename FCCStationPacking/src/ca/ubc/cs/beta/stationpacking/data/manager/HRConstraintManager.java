package ca.ubc.cs.beta.stationpacking.data.manager;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.StationChannelPair;

import au.com.bytecode.opencsv.CSVReader;


public class HRConstraintManager implements IConstraintManager{

	Set<Constraint> fPairwiseConstraints;
	HashMap<Station,Set<Integer>> fStationDomains;
	
	
	public HRConstraintManager(String aAllowedChannelsFilename, String aPairwiseConstraintsFilename) throws IOException
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
				HashSet<Integer> aChannelDomain = new HashSet<Integer>();
				for(int i=2;i<aLine.length;i++)
				{
					Integer aChannel = Integer.valueOf(aLine[i]);
					aChannelDomain.add(aChannel);
				}
				fStationDomains.put(aStation, aChannelDomain);
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
				Integer aChannel1 = Integer.valueOf(aLine[1]);
				Integer aID2 = Integer.valueOf(aLine[2]);
				Integer aChannel2 = Integer.valueOf(aLine[3]);
				
				fPairwiseConstraints.add(new Constraint(new StationChannelPair(new Station(aID1),aChannel1),new StationChannelPair(new Station(aID2),aChannel2)));
				
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
	
	//NA - stubs to implement the new IConstraintManager interface
	public Set<Station> getCOInterferingStations(Station aStation){
		return null;
	}
	
	public Set<Station> getADJplusInterferingStations(Station aStation){
		return null;
	}
	
	public Set<Station> getADJminusInterferingStations(Station aStation){
		return null;
	}
	
	public boolean matchesDomains(IConstraintManager aOtherManager){
		return this.getStationDomains().equals(aOtherManager.getStationDomains());
	}
	
	public boolean matchesConstraints(IConstraintManager aOtherManager){
		return this.getPairwiseConstraints().equals(aOtherManager.getPairwiseConstraints());
	}

}
