package ca.ubc.cs.beta.stationpacking.solver.TAESolver.componentgrouper;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datastructures.Station;


/**
 * Partition station sets so that each member can be encoded in a CNF separately (i.e. without missing any constraints).
 * @author afrechet
 *
 */
public interface IComponentGrouper {
	
	/**
	 * @param aStations - a set of stations.
	 * @return A partition of the input station sets (such that each station can be encoded separately).
	 */
	public Set<Set<Station>> group(Set<Station> aStations);
	
	
}
