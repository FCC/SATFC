package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.sequential;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundleFactory;

/**
 * Creates a sequential solver bundle from a list of solver bundle factories.
 * @author afrechet
 */
public class SequentialSolverBundleFactory implements ISolverBundleFactory{

	private final List<ISolverBundleFactory> fSolverBundleFactories;
	
	/**
	 * @param aSolverBundleFactories - the list of solver bundle factories to execute in line. 
	 */
	public SequentialSolverBundleFactory(List<ISolverBundleFactory> aSolverBundleFactories)
	{
		fSolverBundleFactories = aSolverBundleFactories;
	}
	
	@Override
	public ISolverBundle getBundle(IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		
		List<ISolverBundle> bundles = new ArrayList<ISolverBundle>();
		
		for(ISolverBundleFactory bundleFactory : fSolverBundleFactories)
		{
			bundles.add(bundleFactory.getBundle(aStationManager, aConstraintManager));
		}
		
		return new SequentialSolverBundle(aStationManager, aConstraintManager, bundles);
		
	}



}
