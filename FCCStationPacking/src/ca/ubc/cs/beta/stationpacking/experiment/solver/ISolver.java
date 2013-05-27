package ca.ubc.cs.beta.stationpacking.experiment.solver;

import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;

/**
 * Solves station packing problem instance.
 * Usually a (fancy) wrapper around a SAT solver.
 * @author afrechet
 *
 */
public interface ISolver {
	
	public SolverResult solve(IInstance aInstance, double aCutoff) throws Exception;
		
}
