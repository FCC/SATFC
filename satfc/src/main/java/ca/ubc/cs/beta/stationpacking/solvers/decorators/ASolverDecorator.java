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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Abstract {@link ISolver} decorator that passes the basic methods to the decorated solver.
 *
 * @author afrechet
 */
public abstract class ASolverDecorator implements ISolver {

    protected final ISolver fDecoratedSolver;

    /**
     * @param aSolver - decorated ISolver.
     */
    public ASolverDecorator(ISolver aSolver) {
        fDecoratedSolver = aSolver;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        return aTerminationCriterion.hasToStop() ? SolverResult.createTimeoutResult(0.0) : fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
    }

    @Override
    public void notifyShutdown() {
        fDecoratedSolver.notifyShutdown();
    }

    @Override
    public void interrupt() {
        fDecoratedSolver.interrupt();
    }
}
