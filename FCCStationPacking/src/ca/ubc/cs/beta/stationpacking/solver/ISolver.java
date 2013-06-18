package ca.ubc.cs.beta.stationpacking.solver;

import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;

/**
 * Solves station packing problem instance.
 * Usually a (fancy) wrapper around a SAT solver.
 * @author afrechet
 *
 */
public interface ISolver {
	
	public SolverResult solve(Instance aInstance, double aCutoff) throws Exception;
	
	/*
	public void flagState();
	
	public void resetToFlaggedState();
	
	public void addClause();
	*/
}
