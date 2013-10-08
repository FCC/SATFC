package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractSATSolver;

public class CompleteProblemIncrementalClaspSATWrapper extends AbstractSATSolver{

	private final static double INITIAL_CUTOFF = 2;
	private IncrementalClaspSATSolver fIncrementalSATSolver;
	
	public CompleteProblemIncrementalClaspSATWrapper(String libraryPath, String parameters, long seed, CNF aCompleteProblem)
	{
		fIncrementalSATSolver = new IncrementalClaspSATSolver(libraryPath, parameters, seed);
		//Set the incremental 
		fIncrementalSATSolver.solve(aCompleteProblem, INITIAL_CUTOFF, seed);
	}

	@Override
	public SATSolverResult solve(CNF aCNF, double aCutoff, long aSeed) {
		
		
		return fIncrementalSATSolver.solve(aCNF, aCutoff, aSeed);
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
