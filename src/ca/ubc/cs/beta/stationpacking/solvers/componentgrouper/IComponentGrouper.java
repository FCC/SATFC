package ca.ubc.cs.beta.stationpacking.solvers.componentgrouper;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;


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
	public Set<Set<Station>> group(StationPackingInstance aInstance, IConstraintManager aConstraintManager);
	
	
}
