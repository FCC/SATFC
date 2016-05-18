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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.math.util.FastMath;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 15/10/15.
 * Wait for some time, then proceed
 */
@Slf4j
public class DelayedSolverDecorator extends ASolverDecorator {

    private final long time;
    private final long noise;
    private volatile CountDownLatch latch;

    /**
     * @param time time to wait before proceeding, in seconds
     * @param noise random noise 0 - noise is added
     */
    public DelayedSolverDecorator(ISolver aSolver, double time, double noise) {
        super(aSolver);
        this.noise = (long) (noise * 1000);
        this.time = (long) (time * 1000);
        // meaningless initial assignment to prevent null
        latch = new CountDownLatch(1);
    }

    public DelayedSolverDecorator(ISolver aSolver, double time) {
        this(aSolver, time, 0);
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        latch = new CountDownLatch(1);
        long remainingTime = (long) aTerminationCriterion.getRemainingTime() * 1000;
        long timeToWait = FastMath.min(time + RandomUtils.nextLong(0, noise), remainingTime);
        if (timeToWait > 0) {
            try {
                log.debug("Waiting {} ms", timeToWait);
                latch.await(timeToWait, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted while waiting for delayed solver", e);
            }
        }
        return SolverResult.relabelTime(fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed), watch.getElapsedTime());
    }

    @Override
    public void interrupt() {
        log.debug("Interrupting");
        latch.countDown();
        super.interrupt();
    }

}
