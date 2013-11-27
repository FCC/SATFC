package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.clasp;

import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundleFactory;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

public class ClaspSATSolverBundleFactory implements ISolverBundleFactory {

	private final String fClaspLibrary;
	
	public ClaspSATSolverBundleFactory(String aClaspSATLibrary)
	{
		fClaspLibrary = aClaspSATLibrary;
	}
	
	@Override
	public ISolverBundle getBundle(IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		return new ClaspSATSolverBundle(fClaspLibrary, aStationManager, aConstraintManager);
	}



}
