package ca.ubc.cs.beta.stationpacking.modelcount;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

public interface IModelCountSolver {
    
    /**
     * Solve a station packing instance under the provided CPU time cutoff and given seed.
     * @param aInstance - the instance to solved.
     * @param aTerminationCriterion - the termination criterion for solver execution (usually cutoff time based).
     * @param aSeed - the execution seed.
     * @return
     */
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed);
    
    /**
     * Tries to stop the solve call if implemented, if not throws an UnsupportedOperationException.
     */
    public void interrupt() throws UnsupportedOperationException;
    
    /**
     * Ask the solver to shutdown.
     */
    public void notifyShutdown();

}