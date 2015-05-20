package ca.ubc.cs.beta.stationpacking.execution;

import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.IProblemGenerator;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.SATFCFacadeProblem;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;

/**
 * Created by newmanne on 12/05/15.
 */
public abstract class AProblemGenerator implements IProblemGenerator {

    protected int index = 1;

    public abstract SATFCFacadeProblem getNextProblem();

    @Override
    public void onPostProblem(SATFCFacadeProblem problem, SATFCResult result) {
        index++;
    }
}
