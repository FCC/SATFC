package ca.ubc.cs.beta.stationpacking.daemon.simple.datamanager;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

/**
 * Bundle containing a ISolver, IStationManager and an IConstraintManager.
 */
public class SolverBundle {

	private ISolver fSolver;
	private IStationManager fStationManager;
	private IConstraintManager fConstraintManager;
	
	/**
	 * Creates a bundle containing an ISolver and the managers used to create it.
	 * @param solver solver bundled.
	 * @param stationManager station manager used by the solver.
	 * @param constraintManager constraint manager used by the solver.
	 */
	public SolverBundle(ISolver solver, IStationManager stationManager, IConstraintManager constraintManager)
	{
		fSolver = solver;
		fStationManager = stationManager;
		fConstraintManager = constraintManager;
	}
	
	/**
	 * Return the solver contained in the bundle.
	 * @return the solver contained in the bundle
	 */
	public ISolver getSolver()
	{
		return fSolver;
	}
	
	/**
	 * Returns the station manager contained in the bundle.
	 * @return the station manager contained in the bundle.
	 */
	public IStationManager getStationManager()
	{
		return fStationManager;
	}
	
	/**
	 * Returns the constraint manager contained in the bundle.
	 * @return the constraint manager contained in the bundle.
	 */
	public IConstraintManager getConstraintManager()
	{
		return fConstraintManager;
	}
}
