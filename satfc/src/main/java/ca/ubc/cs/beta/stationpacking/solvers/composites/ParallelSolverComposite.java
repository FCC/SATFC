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
package ca.ubc.cs.beta.stationpacking.solvers.composites;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.interrupt.InterruptibleTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 13/05/15.
 * A very simple parallel solver that forks and joins (i.e. blocks until every solver completes (though it does try to interrupt them))
 */
@Slf4j
public class ParallelSolverComposite implements ISolver {

    Collection<ISolver> solvers;
    private final ForkJoinPool forkJoinPool;

    public ParallelSolverComposite(int threadPoolSize, List<ISolverFactory> solvers) {
        this.solvers = solvers.stream().map(ISolverFactory::create).collect(Collectors.toList());
        log.debug("Creating a fork join pool with {} threads", threadPoolSize);
        forkJoinPool = new ForkJoinPool(threadPoolSize);
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        Watch watch = Watch.constructAutoStartWatch();
        // Swap out the termination criterion to one that can be interrupted
        final ITerminationCriterion.IInterruptibleTerminationCriterion interruptibleCriterion = new InterruptibleTerminationCriterion(aTerminationCriterion);
        try {
            final SolverResult endResult = forkJoinPool.submit(() -> {
                return solvers.parallelStream()
                        .map(solver -> {
                            final SolverResult solve = solver.solve(aInstance, interruptibleCriterion, aSeed);
                            log.trace("Returned from solver");
                            // Interrupt only if the result is conclusive
                            if (solve.getResult().isConclusive() && interruptibleCriterion.interrupt()) {
                                log.debug("Found a conclusive result {}, interrupting other concurrent solvers", solve);
                                solvers.forEach(ISolver::interrupt);
                            }
                            return solve;
                        })
                        .filter(result -> result.getResult().isConclusive())
                        .findAny();
            }).get().orElse(SolverResult.createTimeoutResult(watch.getElapsedTime()));
            // Note that we don't modify time here, even though you might have to wait a while for the other threads to finish between the time the result is created and the time it can be returned. This solver is used for experimental purposes, to make sure that all threads have "caught up" before the next problem is started
            return endResult;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error processing jobs in parallel!", e);
        }
    }

    @Override
    public void notifyShutdown() {
        solvers.forEach(ISolver::notifyShutdown);
    }

	@Override
	public void interrupt() {
        // Just wait for the individual solvers to terminate
	}

}
