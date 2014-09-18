package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat;

import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;

/**
 * Parameters to create a SAT solver.
 * @author afrechet
 */
public interface ISATSolverParameters {
    
    /**
     * @return a constructed SAT solver from parameters.
     */
	public ISATSolver getSATSolver();		
	
}
