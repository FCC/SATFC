package ca.ubc.cs.beta.stationpacking.datamanagers.stations;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;


public interface IStationManager {

	/**
	 * @return - all the stations represented by the station manager.
	 */
	public Set<Station> getStations();
	
	/**
	 * 
	 * @param a station ID.
	 * @return the station for the particular ID.
	 */
	public Station getStationfromID(Integer aID);
	
}
