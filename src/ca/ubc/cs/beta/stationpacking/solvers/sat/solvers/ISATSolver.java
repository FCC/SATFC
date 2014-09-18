package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers;

import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Interface for a SAT solver.
 * @author afrechet
 */
public interface ISATSolver {

	/**
	 * @param aCNF - a CNF to solve.
	 * @param aTerminationCriterion - the criterion dictating when to stop execution of solver.
	 * @param aSeed - the seed for the execution.
	 */
	public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed);
	
	/**
	 * Tries to stop the solve call if implemented, if not throws an UnsupportedOperationException.
	 */
	public void interrupt() throws UnsupportedOperationException;
	
	public void notifyShutdown();
}
