package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

/**
 * A solver bundle that holds solvers for a specific problem domain.
 * Also performs solver selection when queried with a problem instance.
 * @author afrechet
 */
public interface ISolverBundle extends AutoCloseable {
	
	/**
	 * @param aInstance - the instance for which a solver is needed. 
	 * @return the solver contained in the bundle for the given instance.
	 */
	public ISolver getSolver(StationPackingInstance aInstance);
	
	/**
	 * @return the station manager contained in the bundle.
	 */
	public IStationManager getStationManager();
	
	/**
	 * Returns the constraint manager contained in the bundle.
	 * @return the constraint manager contained in the bundle.
	 */
	public IConstraintManager getConstraintManager();
	
}
