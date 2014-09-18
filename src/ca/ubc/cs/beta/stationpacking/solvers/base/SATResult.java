package ca.ubc.cs.beta.stationpacking.solvers.base;

import java.io.Serializable;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;

/**
 * Enum for the result type of a SAT solver on a SAT instance.
 * @author afrechet
 */
public enum SATResult implements Serializable{
	
    /**
     * The problem is satisfiable. 
     */
    SAT,
    /**
     * The problem is unsatisfiable. 
     */
    UNSAT,
    /**
     * A solution to the problem could not be found in the allocated time.
     */
    TIMEOUT,
    /**
     * Run crashed while solving problem.
     */
    CRASHED,
    /**
     * Run was killed while solving problem.
     */
    KILLED,
    /**
     * Run was interrupted while solving problem.
     */
    INTERRUPTED;
	
	/**
	 * @param aRunResult - a runresult.
	 * @return the given RunStatus converted to a SATResult.
	 */
	public static SATResult fromRunResult(RunStatus aRunResult)
	{	
		return SATResult.valueOf(aRunResult.toString());
	}
}


