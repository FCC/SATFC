package ca.ubc.cs.beta.stationpacking.solvers;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
* Created by newmanne on 11/05/15.
*/
public class VoidSolver implements ISolver {
    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        return new SolverResult(SATResult.TIMEOUT, 0.0);
    }

    @Override
    public void interrupt() throws UnsupportedOperationException {

    }

    @Override
    public void notifyShutdown() {

    }
}
