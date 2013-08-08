package ca.ubc.cs.beta.stationpacking.solver;

import java.io.Serializable;

import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;

/**
 * Enum for the result type of a SAT solver on a SAT instance.
 * @author afrechet
 */
public enum SATResult implements Serializable{
	SAT,UNSAT,TIMEOUT,CRASHED,KILLED;
	
	public SATResult fromRunResult(RunResult aRunResult)
	{	
		return SATResult.valueOf(aRunResult.toString());
	}
}


