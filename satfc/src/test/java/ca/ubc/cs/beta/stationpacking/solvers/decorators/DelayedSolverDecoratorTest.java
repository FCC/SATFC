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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.StationPackingTestUtils;
import ca.ubc.cs.beta.stationpacking.solvers.VoidSolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

public class DelayedSolverDecoratorTest {

    @Test
    public void testDelay() throws Exception {
        final DelayedSolverDecorator delayedSolverDecorator = new DelayedSolverDecorator(new VoidSolver(), 0.1);
        final Watch watch = Watch.constructAutoStartWatch();
        delayedSolverDecorator.solve(StationPackingTestUtils.getSimpleInstance(), new WalltimeTerminationCriterion(10), 1);
        assertTrue("Watch time was " + watch.getElapsedTime(), watch.getElapsedTime() >= 0.1);
    }

    @Test
    public void testInterrupt() throws Exception {
        final Watch watch = Watch.constructAutoStartWatch();
        final DelayedSolverDecorator delayedSolverDecorator = new DelayedSolverDecorator(new VoidSolver(), 90);
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                delayedSolverDecorator.interrupt();
            }
        }.start();
        final SolverResult solve = delayedSolverDecorator.solve(StationPackingTestUtils.getSimpleInstance(), new WalltimeTerminationCriterion(90), 1);
        final double time = watch.getElapsedTime();
        assertTrue("Watch time was " + time + " expected less since interruption", time <= 1);
    }

}