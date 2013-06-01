package ca.ubc.cs.beta.stationpacking.data.manager;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;


public interface IStationManager {

	public Set<Station> getStations();
	
	public Station get(Integer aID);
	//public Set<Station> getFixedStations();
	
	//public Set<Station> getUnfixedStations();
	
	//public HashMap<Station,Integer> getStationPopulation();
}
