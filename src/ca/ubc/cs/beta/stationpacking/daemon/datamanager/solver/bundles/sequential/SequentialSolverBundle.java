package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.sequential;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ASolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.composites.SequentialSolversComposite;

public class SequentialSolverBundle extends ASolverBundle {

	private final List<ISolverBundle> fSolverBundles;
	
	public SequentialSolverBundle(IStationManager aStationManager, IConstraintManager aConstraintManager, List<ISolverBundle> aSolverBundles)
	{
		super(aStationManager,aConstraintManager);
		
		fSolverBundles = aSolverBundles;
	}
	
	@Override
	public ISolver getSolver(StationPackingInstance aInstance) {
		List<ISolver> solvers = new ArrayList<ISolver>();
		
		for(ISolverBundle bundle : fSolverBundles)
		{
			solvers.add(bundle.getSolver(aInstance));
		}
		
		return new SequentialSolversComposite(solvers);
	}

	@Override
	public void notifyShutdown() {
		for(ISolverBundle bundle : fSolverBundles)
		{
			bundle.notifyShutdown();
		}
	}

}
