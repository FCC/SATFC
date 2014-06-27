package ca.ubc.cs.beta.stationpacking.solvers.base;

import java.io.Serializable;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;

/**
 * Enum for the result type of a SAT solver on a SAT instance.
 * @author afrechet
 */
public enum SATResult implements Serializable{
	SAT,UNSAT,TIMEOUT,CRASHED,KILLED,INTERRUPTED;
	
	public SATResult fromRunResult(RunStatus aRunResult)
	{	
		return SATResult.valueOf(aRunResult.toString());
	}
}


