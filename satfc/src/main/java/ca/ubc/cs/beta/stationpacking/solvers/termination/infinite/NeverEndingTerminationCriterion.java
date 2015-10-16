package ca.ubc.cs.beta.stationpacking.solvers.termination.infinite;

import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Created by newmanne on 29/07/15.
 */
public class NeverEndingTerminationCriterion implements ITerminationCriterion {

    @Override
    public double getRemainingTime() {
        return 10800; // arbitrary big number
    }

    @Override
    public boolean hasToStop() {
        return false;
    }

    @Override
    public void notifyEvent(double aTime) {

    }
}
