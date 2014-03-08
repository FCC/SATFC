package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.simplebounderpresolver;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundleFactory;

public class SimpleBounderPresolverBundleFactory implements ISolverBundleFactory {

	private final ISolverBundleFactory fSolverBundleFactory;
	
	public SimpleBounderPresolverBundleFactory(ISolverBundleFactory aSolverBundleFactory)
	{
		fSolverBundleFactory = aSolverBundleFactory;
	}
	
	@Override
	public ISolverBundle getBundle(IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		
		return new SimpleBounderPresolverBundle(aStationManager, aConstraintManager, fSolverBundleFactory.getBundle(aStationManager, aConstraintManager));
		
	}



}
