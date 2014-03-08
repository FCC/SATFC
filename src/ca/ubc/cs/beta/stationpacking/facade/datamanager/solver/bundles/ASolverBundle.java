package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

public abstract class ASolverBundle implements ISolverBundle{

	private final IStationManager fStationManager;
	private final IConstraintManager fConstraintManager;
	
	public ASolverBundle(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		fStationManager = aStationManager;
		fConstraintManager = aConstraintManager;
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
	

}
