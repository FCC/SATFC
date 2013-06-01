package ca.ubc.cs.beta.stationpacking.execution.incremental.ClauseAdder;

import ca.ubc.cs.beta.stationpacking.experiment.instance.*;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;

public interface IClauseAdder {

	public SolverResult updateAndSolve(IInstance aInstance,Boolean priceCheck);
	
}
