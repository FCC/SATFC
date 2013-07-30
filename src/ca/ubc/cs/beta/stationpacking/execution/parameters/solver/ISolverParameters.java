package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

public interface ISolverParameters {

	public ISolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager);
	
}
