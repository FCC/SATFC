package ca.ubc.cs.beta.stationpacking.solvers.componentgrouper;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;


/**
 * Partition station sets so that each member can be solved separately.
 * @author afrechet
 */
public interface IComponentGrouper {
	
	/**
	 * @param aInstance instance to partition.
	 * @param aConstraintManager constraint manager for that instance.
	 * @return A partition of the input station sets (such that each station can be encoded separately).
	 */
	public Set<Set<Station>> group(StationPackingInstance aInstance, IConstraintManager aConstraintManager);
	
	
}
