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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.Clasp3Library;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.NativeUtils;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.base.Preconditions;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

@Slf4j
public class Clasp3SATSolver extends AbstractCompressedSATSolver {

    private Clasp3Library fClaspLibrary;
    private String fParameters;
    private final Lock lock = new ReentrantLock();
    private Pointer currentProblemPointer;
    // boolean represents whether or not a solve is in progress, so that it is safe to do an interrupt
    private final AtomicBoolean isCurrentlySolving = new AtomicBoolean(false);
    private final int fSeedOffset;

    public Clasp3SATSolver(String libraryPath, String parameters) {
        this((Clasp3Library) Native.loadLibrary(libraryPath, Clasp3Library.class, NativeUtils.NATIVE_OPTIONS), parameters);
    }

    public Clasp3SATSolver(Clasp3Library library, String parameters) {
        this(library, parameters, 0);
    }

    public Clasp3SATSolver(Clasp3Library library, String parameters, int seedOffset) {
        fSeedOffset = seedOffset;
        fClaspLibrary = library;
        fParameters = parameters;
        // set the info about parameters, throw an exception if seed is contained.
        if (parameters.contains("--seed")) {
            throw new IllegalArgumentException("The parameter string cannot contain a seed as it is given upon a call to solve!");
        }
        // make sure the configuration is valid
        String params = fParameters + " --seed=1";
        Pointer jnaProblem = fClaspLibrary.initConfig(params);
        try {
            int status = fClaspLibrary.getConfigState(jnaProblem);
            if (status == 2) {
                throw new IllegalArgumentException(fClaspLibrary.getConfigErrorMessage(jnaProblem));
            }
        } finally {
            fClaspLibrary.destroyProblem(jnaProblem);
        }
    }

    /*
     * (non-Javadoc)
     * NOT THREAD SAFE!
     * @see ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver#solve(ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF, double, long)
     */
    @Override
    public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        final int seed = Math.abs(new Random(aSeed + fSeedOffset).nextInt());
        final String params = fParameters + " --seed=" + seed;
        try {
            // create the problem - config params have already been validated in the constructor, so this should work
            Preconditions.checkState(currentProblemPointer == null, "Went to solve a new problem, but there is a problem in progress!");
            if (aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }

            currentProblemPointer = fClaspLibrary.initConfig(params);
            fClaspLibrary.initProblem(currentProblemPointer, aCNF.toDIMACS(null));

            if (aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }

            // We lock this variable so that the interrupt code will only execute if there is a valid problem to interrupt
            lock.lock();
            isCurrentlySolving.set(true);
            lock.unlock();

            final double cutoff = aTerminationCriterion.getRemainingTime();
            if (cutoff <= 0 || aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }

            // Start solving
            log.debug("Send problem to clasp cutting off after " + cutoff + "s");
            final Watch runtime = Watch.constructAutoStartWatch();
            fClaspLibrary.solveProblem(currentProblemPointer, cutoff);
            log.debug("Came back from clasp after {}s.", runtime.getElapsedTime());
            lock.lock();
            isCurrentlySolving.set(false);
            lock.unlock();

            if (aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }
            
            final Watch postTime = Watch.constructAutoStartWatch();
            final ClaspResult claspResult = getSolverResult(fClaspLibrary, currentProblemPointer, runtime.getElapsedTime());
            log.trace("Time to parse clasp result: {} s.", postTime.getElapsedTime());
            final HashSet<Literal> assignment = parseAssignment(claspResult.getAssignment());
            log.trace("Total post time (parsing result + assignment): {} s.", postTime.getElapsedTime());
            if (postTime.getElapsedTime() > 60) {
                log.error("Clasp SAT solver post solving time was greater than 1 minute, something wrong must have happened.");
            }

            final SATSolverResult output = new SATSolverResult(claspResult.getSATResult(), watch.getElapsedTime(), assignment);
            log.debug("Returning result: {}, {}s.", output.getResult(), output.getRuntime());
            log.trace("Full result: {}", output);
            return output;
        } finally {
            // Cleanup in the finally block so it always executes: if we instantiated a problem, we make sure that we free it
            if (currentProblemPointer != null) {
                log.trace("Destroying problem");
                lock.lock();
                isCurrentlySolving.set(false);
                lock.unlock();
                fClaspLibrary.destroyProblem(currentProblemPointer);
                currentProblemPointer = null;
            }
        }
    }

    @Override
    public void notifyShutdown() {

    }

    @Override
    public void interrupt() {
    	lock.lock();
        if (isCurrentlySolving.get()) {
            log.debug("Interrupting clasp");
            fClaspLibrary.interrupt(currentProblemPointer);
            log.debug("Back from interrupting clasp");
        }
        lock.unlock();
    }

    private HashSet<Literal> parseAssignment(int[] assignment) {
        HashSet<Literal> set = new HashSet<>();
        for (int i = 1; i < assignment[0]; i++) {
            int intLit = assignment[i];
            int var = Math.abs(intLit);
            boolean sign = intLit > 0;
            Literal aLit = new Literal(var, sign);
            set.add(aLit);
        }
        return set;
    }

    /**
     * Extract solver result from JNA Clasp library.
     */
    public static ClaspResult getSolverResult(Clasp3Library library,
                                              Pointer problem,
                                              double runtime) {

        final SATResult satResult;
        int[] assignment = {0};
        int state = library.getResultState(problem);
        if (state == 0) {
            satResult = SATResult.UNSAT;
        } else if (state == 1) {
            satResult = SATResult.SAT;
            IntByReference pRef = library.getResultAssignment(problem);
            int size = pRef.getValue();
            assignment = pRef.getPointer().getIntArray(0, size);
        } else if (state == 2) {
            satResult = SATResult.TIMEOUT;
        } else if (state == 3) {
            satResult = SATResult.INTERRUPTED;
        } else {
            satResult = SATResult.CRASHED;
            log.error("Clasp crashed!");
        }

        return new ClaspResult(satResult, assignment, runtime);
    }

}