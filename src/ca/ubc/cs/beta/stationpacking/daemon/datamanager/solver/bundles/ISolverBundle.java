package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

/**
 * A solver bundle that holds solvers for a specific problem domain.
 * Also performs solver selection when queried with a problem instance.
 * @author afrechet
 *
 */
public interface ISolverBundle {
	
	/**
	 * Return the solver contained in the bundle.
	 * @return the solver contained in the bundle
	 */
	public ISolver getSolver(StationPackingInstance aInstance);
	
	/**
	 * Returns the station manager contained in the bundle.
	 * @return the station manager contained in the bundle.
	 */
	public IStationManager getStationManager();
	
	/**
	 * Returns the constraint manager contained in the bundle.
	 * @return the constraint manager contained in the bundle.
	 */
	public IConstraintManager getConstraintManager();
	
	/**
	 * Shutdown the solver in the bundle.
	 */
	public void notifyShutdown();
}
