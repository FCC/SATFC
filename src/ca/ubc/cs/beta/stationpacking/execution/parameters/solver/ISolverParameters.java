package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

public interface ISolverParameters {

	public ISolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager);
	
}
