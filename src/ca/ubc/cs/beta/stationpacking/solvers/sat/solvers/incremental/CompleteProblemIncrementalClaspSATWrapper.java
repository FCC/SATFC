package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.CPUTimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

public class CompleteProblemIncrementalClaspSATWrapper extends AbstractSATSolver{

	private final static double INITIAL_CUTOFF = 2;
	private IncrementalClaspSATSolver fIncrementalSATSolver;
	
	public CompleteProblemIncrementalClaspSATWrapper(String libraryPath, String parameters, long seed, CNF aCompleteProblem)
	{
		fIncrementalSATSolver = new IncrementalClaspSATSolver(libraryPath, parameters, seed);
		//Set the incremental 
		fIncrementalSATSolver.solve(aCompleteProblem, new CPUTimeTerminationCriterion(INITIAL_CUTOFF), seed);
	}

	@Override
	public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed) {
		
		
		return fIncrementalSATSolver.solve(aCNF, aTerminationCriterion, aSeed);
	}

	@Override
	public void interrupt() throws UnsupportedOperationException {
		fIncrementalSATSolver.interrupt();
	}

	@Override
	public void notifyShutdown() {
		fIncrementalSATSolver.notifyShutdown();
	}

}
