package ca.ubc.cs.beta.stationpacking.experiment.instance;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;

/**
 * A station packing problem instance container object.
 * @author afrechet, narnosti
 *
 */
public interface IInstance {
	
	/* Returns the set of stations associated with the instance.
	 * Never null, though it may be empty.
	 */
	public Set<Station> getStations();
	
	/* Adds aStation to the set of stations.
	 * Returns true if the set did not previously contain aStation.
	 */
	public boolean addStation(Station aStation);
	
	/* Adds aStation to the set of stations.
	 * Returns true if the set previously contained aStation.
	 */
	public boolean removeStation(Station aStation);
	
	/* Returns the set of channels associated with the instance.
	 */
	public Set<Integer> getChannelRange();
	
	/*
	 * 
	 */
	
	public Station getStation(Integer aID);
	
	@Override
	public String toString();

	public int getNumberofStations();
	
	
}
