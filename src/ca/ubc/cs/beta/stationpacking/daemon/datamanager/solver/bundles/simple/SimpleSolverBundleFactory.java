package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.simple;

import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundleFactory;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;

public class SimpleSolverBundleFactory implements ISolverBundleFactory {

	private final ISolver fSolver;
	
	public SimpleSolverBundleFactory(ISolver aSolver)
	{
		fSolver = aSolver;
	}
	
	@Override
	public ISolverBundle getBundle(IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		
		return new SimpleSolverBundle(fSolver, aStationManager, aConstraintManager);
		
	}

	

}
