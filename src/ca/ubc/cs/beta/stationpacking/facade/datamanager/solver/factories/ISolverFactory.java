package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;


/**
 * Factory used to create ISolvers.
 */
public interface ISolverFactory {

	/**
	 * Returns a Solver using the given constraint and station manager.
	 * @param stationManager station manager to be used by the solver.
	 * @param constraintManager constraint manager to be used by the solver.
	 * @return a Solver using the given constraint and station manager.
	 */
	ISolver create(IStationManager stationManager, IConstraintManager constraintManager);
	
}
