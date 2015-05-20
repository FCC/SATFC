package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import ca.ubc.cs.beta.stationpacking.execution.AProblemGenerator;

/**
* Created by newmanne on 12/05/15.
*/
public class SingleProblemFromCommandLineProblemGenerator extends AProblemGenerator {

    private final SATFCFacadeProblem problem;

    public SingleProblemFromCommandLineProblemGenerator(SATFCFacadeProblem problem) {
        this.problem = problem;
    }

    @Override
    public SATFCFacadeProblem getNextProblem() {
        return index == 1 ? problem : null;
    }

}
