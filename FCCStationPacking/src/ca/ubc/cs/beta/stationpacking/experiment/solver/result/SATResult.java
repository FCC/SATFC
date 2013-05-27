package ca.ubc.cs.beta.stationpacking.experiment.solver.result;

import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;

/**
 * Enum for the result type of a SAT solver on a SAT instance.
 * @author afrechet
 */
public enum SATResult {
	SAT,UNSAT,TIMEOUT,CRASHED;
	
	public SATResult fromRunResult(RunResult aRunResult)
	{	
		return SATResult.valueOf(aRunResult.toString());
	}
}


