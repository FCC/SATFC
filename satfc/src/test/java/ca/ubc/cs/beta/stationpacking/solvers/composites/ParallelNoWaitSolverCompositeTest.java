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
package ca.ubc.cs.beta.stationpacking.solvers.composites;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.StationPackingTestUtils;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.termination.infinite.NeverEndingTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.collect.ImmutableMap;

@Slf4j
public class ParallelNoWaitSolverCompositeTest {

    /**
     * Run a lot of infinite loop solvers and one auto-return the answer solver
     * The good solver should interrupt the infinite loopers
     */
    @Test(timeout = 3000)
    public void testInterruptOtherSolvers() {
        final List<ISolverFactory> solvers = new ArrayList<>();
        // We need to have enough threads so that the good solver is run concurrently with the bad solvers
        final int nThreads = 11;
        for (int i = 0; i < nThreads - 1; i++) {
            // create many solvers that loop infinitely
            solvers.add(s->(aInstance, aTerminationCriterion, aSeed) -> {
                final Watch watch = Watch.constructAutoStartWatch();
                while (!aTerminationCriterion.hasToStop()) {} // infinite loop
                return SolverResult.createTimeoutResult(watch.getElapsedTime());
            });
        }
        // construct a solver that just returns the answer immediately
        solvers.add(s->(aInstance, aTerminationCriterion, aSeed) -> new SolverResult(SATResult.SAT, 32.0, StationPackingTestUtils.getSimpleInstanceAnswer(), SolvedBy.UNKNOWN));
        final ParallelNoWaitSolverComposite parallelSolverComposite = new ParallelNoWaitSolverComposite(nThreads, solvers);
        final SolverResult solve = parallelSolverComposite.solve(StationPackingTestUtils.getSimpleInstance(), new NeverEndingTerminationCriterion(), 1);
        assertEquals(SATResult.SAT, solve.getResult());
    }

    @Test
    public void everySolverIsQueriedIfNoOneHasAConclusiveAnswer() {
        final List<ISolverFactory> solvers = new ArrayList<>();
        final AtomicInteger numCalls = new AtomicInteger(0);
        final int N_SOLVERS = 10;
        for (int i = 0; i < N_SOLVERS; i++) {
            solvers.add(s->(aInstance, aTerminationCriterion, aSeed) -> {
                numCalls.incrementAndGet();
                return SolverResult.createTimeoutResult(60.0);
            });
        }
        final ParallelNoWaitSolverComposite parallelSolverComposite = new ParallelNoWaitSolverComposite(1, solvers);
        final SolverResult solve = parallelSolverComposite.solve(StationPackingTestUtils.getSimpleInstance(), new NeverEndingTerminationCriterion(), 1);
        assertEquals(numCalls.get(), N_SOLVERS); // every solver should be asked
        assertEquals(solve.getResult(), SATResult.TIMEOUT);
    }

    @Test(timeout = 2000)
    public void notEverySolverIsWaitedForIfOneHasAConclusiveAnswer() {
        final List<ISolverFactory> solvers = new ArrayList<>();
        solvers.add(s->(aInstance, aTerminationCriterion, aSeed) -> new SolverResult(SATResult.SAT, 1.0, ImmutableMap.of(), SolvedBy.UNKNOWN));
        solvers.add(s->(aInstance, aTerminationCriterion, aSeed) -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            return SolverResult.createTimeoutResult(1.0);
        });
        final ParallelNoWaitSolverComposite parallelSolverComposite = new ParallelNoWaitSolverComposite(1, solvers);
        final SolverResult solve = parallelSolverComposite.solve(StationPackingTestUtils.getSimpleInstance(), new NeverEndingTerminationCriterion(), 1);
        assertEquals(SATResult.SAT, solve.getResult());
    }

    @Test(expected = RuntimeException.class)
    public void exceptionsPropagateToMainThread() {
        final List<ISolverFactory> solvers = new ArrayList<>();
        solvers.add(s->(aInstance, aTerminationCriterion, aSeed) -> {
            throw new IllegalArgumentException();
        });
        final ParallelNoWaitSolverComposite parallelSolverComposite = new ParallelNoWaitSolverComposite(1, solvers);
        parallelSolverComposite.solve(StationPackingTestUtils.getSimpleInstance(), new NeverEndingTerminationCriterion(), 1);
    }

}