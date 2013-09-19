package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

public class ClaspSATSolverSelectorBundleFactory implements ISolverBundleFactory {

	private final String fClaspLibrary;
	
	public ClaspSATSolverSelectorBundleFactory(String aClaspSATLibrary)
	{
		fClaspLibrary = aClaspSATLibrary;
	}
	
	@Override
	public ISolverBundle getBundle(IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		return new ClaspSATSolverSelectorBundle(fClaspLibrary, aStationManager, aConstraintManager);
	}



}
