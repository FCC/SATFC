package ca.ubc.cs.beta.stationpacking.data.manager;

import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;


public interface IConstraintManager {
	
	public Set<Constraint> getPairwiseConstraints();
	
	public Map<Station,Set<Integer>> getStationDomains();
	
	public Set<Station> getCOInterferingStations(Station aStation);
	
	public Set<Station> getADJplusInterferingStations(Station aStation);
	
	public Set<Station> getADJminusInterferingStations(Station aStation);
	
	public boolean matchesDomains(IConstraintManager aOtherManager);
	
	public boolean matchesConstraints(IConstraintManager aOtherManager);
	
}
