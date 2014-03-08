package ca.ubc.cs.beta.stationpacking.datamanagers.stations;

import java.util.Collection;
import java.util.HashSet;
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
	 * @throws IllegalArgumentException - if the provided ID cannot be found in the stations.
	 */
	public Station getStationfromID(Integer aID) throws IllegalArgumentException;
	
	/**
	 * 
	 * @param aIDs - a collection of station IDs.
	 * @return the set of stations with provided IDs.
	 */
	public HashSet<Station> getStationsfromID(Collection<Integer> aIDs);
	
}
