package ca.ubc.cs.beta.stationpacking.solver.IncrementalSolver.ClauseAdder;

import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;

public interface IClauseAdder {

	public SolverResult updateAndSolve(Instance aInstance,Boolean priceCheck);
	
}
