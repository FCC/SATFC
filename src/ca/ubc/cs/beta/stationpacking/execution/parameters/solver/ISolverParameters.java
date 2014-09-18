package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

/**
 * Parameters to construct an ISolver.
 * @author afrechet
 */
public interface ISolverParameters {
    
    /**
     * @param aStationManager - station manager to use in ISolver.
     * @param aConstraintManager - constraint manager to use in ISolver.
     * @return ISolver constructed from parameters.
     */
	public ISolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager);
	
}
