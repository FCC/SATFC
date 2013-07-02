package ca.ubc.cs.beta.stationpacking.solver;

import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;

/**
 * Solves station packing problem instance.
 * Usually a (fancy) wrapper around a SAT solver.
 * @author afrechet
 *
 */
public interface ISolver {
	
	public SolverResult solve(StationPackingInstance aInstance, double aCutoff, long aSeed) throws Exception;
	
	/**
	 * Ask the solver to shutdown.
	 */
	public void notifyShutdown();
	
	/*
	public void flagState();
	
	public void resetToFlaggedState();
	
	public void addClause();
	*/
}
