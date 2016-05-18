/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * Created by newmanne on 15/10/15.
 * Run a solver for a fixed amount of time, then continue down the decorator tree if it fails
 */
public class TimeBoundedSolverDecorator extends ASolverDecorator {

    private final ISolver timeBoundedSolver;
    private final double timeBound;

    public TimeBoundedSolverDecorator(ISolver solverToDecorate, ISolver timeBoundedSolver, double timeBound) {
        super(solverToDecorate);
        this.timeBoundedSolver = timeBoundedSolver;
        this.timeBound = timeBound;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        final ITerminationCriterion newCriterion = new DisjunctiveCompositeTerminationCriterion(aTerminationCriterion, new WalltimeTerminationCriterion(timeBound));
        final SolverResult solve = timeBoundedSolver.solve(aInstance, newCriterion, aSeed);
        if (aTerminationCriterion.hasToStop() || solve.isConclusive()) {
            return solve;
        } else {
            return SolverResult.relabelTime(super.solve(aInstance, aTerminationCriterion, aSeed), watch.getElapsedTime());
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        timeBoundedSolver.interrupt();
    }
}
