package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

/**
 * Bundle containing a ISolver, IStationManager and an IConstraintManager.
 * Does not perform any solver selection.
 */
public class SimpleSolverBundle implements ISolverBundle{

	private final ISolver fSolver;
	private final IStationManager fStationManager;
	private final IConstraintManager fConstraintManager;
	
	/**
	 * Creates a bundle containing an ISolver and the managers used to create it.
	 * @param solverselector a per-instance solver selector.
	 * @param stationManager station manager used by the solver.
	 * @param constraintManager constraint manager used by the solver.
	 */
	public SimpleSolverBundle(ISolver solver, IStationManager stationManager, IConstraintManager constraintManager)
	{
		fSolver = solver;
		fStationManager = stationManager;
		fConstraintManager = constraintManager;
	}
	
	@Override
	public ISolver getSolver(StationPackingInstance aInstance)
	{
		return fSolver;
	}
	
	@Override
	public IStationManager getStationManager()
	{
		return fStationManager;
	}
	
	@Override
	public IConstraintManager getConstraintManager()
	{
		return fConstraintManager;
	}
	
	@Override
	public void notifyShutdown()
	{
		fSolver.notifyShutdown();
	}
}
