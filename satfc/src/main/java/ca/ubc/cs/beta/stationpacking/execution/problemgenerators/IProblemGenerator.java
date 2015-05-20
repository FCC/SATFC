package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;

/**
* Created by newmanne on 12/05/15.
* Abstraction around how SATFC gets the next problem to solve
*/
public interface IProblemGenerator {

    /**
     * @return The next problem to solve, or null if there are no more problems to solve
     */
    SATFCFacadeProblem getNextProblem();

    /**
     * Call this method after solving a problem. It handles cleanup that may be required (e.g. deleting from redis processing queue)
     * @param problem The problem that was just solved
     * @param result The result of the problem
     */
    void onPostProblem(SATFCFacadeProblem problem, SATFCResult result);

    /**
     * Call this when all problems have been exhausted
     */
    default void onFinishedAllProblems() {};
}
