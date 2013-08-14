package ca.ubc.cs.beta.stationpacking.daemon.simple.datamanager;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Bundle object containing a station and a constraint manager.
 */
public class ManagerBundle {

	private IStationManager fStationManager;
	private IConstraintManager fConstraintManager;
	
	/**
	 * Creates a new bundle containing the given station and constraint manager.
	 * @param stationManager station manager to be bundled.
	 * @param constraintManager constraint manager to be bundled.
	 */
	public ManagerBundle(IStationManager stationManager, IConstraintManager constraintManager) {
		fStationManager = stationManager;
		fConstraintManager = constraintManager;
	}

	/**
	 * Returns the bundled station manager.
	 * @return the bundled station manager.
	 */
	public IStationManager getStationManager()
	{
		return fStationManager;
	}
	
	/**
	 * Returns the bundled constraint manager.
	 * @return the bundled station manager.
	 */
	public IConstraintManager getConstraintManager()
	{
		return fConstraintManager;
	}
	
}
