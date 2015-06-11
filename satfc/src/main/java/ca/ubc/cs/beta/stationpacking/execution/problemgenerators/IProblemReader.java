/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;

/**
* Created by newmanne on 12/05/15.
* Abstraction around how SATFC gets the next problem to solve
*/
public interface IProblemReader {

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
