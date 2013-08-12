package ca.ubc.cs.beta.stationpacking.solvers.componentgrouper;

import java.util.HashSet;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;

/**
 * Does not group anything. Usually for testing purpose.
 * @author afrechet
 *
 */
public class NoGrouper implements IComponentGrouper {

	@Override
	public Set<Set<Station>> group(StationPackingInstance aInstance,
			IConstraintManager aConstraintManager) {
		HashSet<Set<Station>> aGroups = new HashSet<Set<Station>>();
		
		aGroups.add(aInstance.getStations());
		
		return aGroups;
	}



}
