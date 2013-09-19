package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

public interface ISolverBundleFactory {
	
	/**
	 * @param aStationManager - a station manager.
	 * @param aConstraintManager - a constraint manager.
	 * @return a solver bundle for the provided constraint managers.
	 */
	public ISolverBundle getBundle(IStationManager aStationManager, IConstraintManager aConstraintManager);
	
}
