package ca.ubc.cs.beta.stationpacking.datamanagers;

import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datastructures.Station;


public interface IConstraintManager {
	
	//public Set<Constraint> getPairwiseConstraints(Set<Integer> aChannels);
		
	public Set<Station> getCOInterferingStations(Station aStation, Set<Integer> aChannelRange);
	
	public Set<Station> getADJplusInterferingStations(Station aStation, Set<Integer> aChannelRange);
	
	//public Set<Station> getADJminusInterferingStations(Station aStation);
	
	public Boolean isSatisfyingAssignment(Map<Integer,Set<Station>> aAssignment);
	
	//public Set<Set<Station>> group(Set<Station> aStations);
	
	//public Map<Station,Set<Integer>> getStationDomains();
	
	//public boolean matchesDomains(IConstraintManager aOtherManager);
	
	//public boolean matchesConstraints(IConstraintManager aOtherManager);
	
}
