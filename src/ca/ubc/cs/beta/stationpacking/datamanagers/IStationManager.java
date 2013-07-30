package ca.ubc.cs.beta.stationpacking.datamanagers;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datastructures.Station;


public interface IStationManager {

	public Set<Station> getStations();
	
	public Station getStationfromID(Integer aID);
	
}
