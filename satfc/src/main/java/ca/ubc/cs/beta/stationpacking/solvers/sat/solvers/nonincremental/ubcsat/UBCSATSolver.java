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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ubcsat;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Preconditions;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import ca.ubc.cs.beta.stationpacking.polling.IPollingService;
import ca.ubc.cs.beta.stationpacking.polling.ProblemIncrementor;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.UBCSATLibrary;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.NativeUtils;
import ca.ubc.cs.beta.stationpacking.utils.Watch;
import lombok.extern.slf4j.Slf4j;

/**
 * The solver that runs different configurations of UBCSAT, including SATenstein.
 *
 * Created by pcernek on 7/28/15.
 */
@Slf4j
public class UBCSATSolver extends AbstractCompressedSATSolver {

    private UBCSATLibrary fLibrary;
    private final int seedOffset;
    private final String fParameters;
    private Pointer fState;
    private final Lock lock = new ReentrantLock();
    // boolean represents whether or not a solve is in progress, so that it is safe to do an interrupt
    private final AtomicBoolean isCurrentlySolving = new AtomicBoolean(false);
    private final ProblemIncrementor problemIncrementor;
    private final String nickname;

    public UBCSATSolver(String libraryPath, String parameters, IPollingService pollingService) {
        this((UBCSATLibrary) Native.loadLibrary(libraryPath, UBCSATLibrary.class, NativeUtils.NATIVE_OPTIONS), parameters, 0, pollingService, null);
    }

    /**
     * @param library - the UBCSATLibrary object that will be used to make the calls over JNA.
     * @param parameters - a well-formed string of UBCSAT parameters. This constructor checks a couple basic things:
     *                   1) that the parameter string contains the -alg flag.
     *                   2) that the parameter string does not contain the -seed flag. (This is passed explicitly in {@link UBCSATSolver#solve(CNF, ITerminationCriterion, long)}.
     *                   3) if the -cutoff flag is not present, this constructor appends "-cutoff max" to the parameter string, which
     *                      means that the algorithm will run for as long as possible until the time limit specified in {@link UBCSATSolver#solve(CNF, ITerminationCriterion, long)} is reached.
     *
     *                   Other than this, all parameter checking happens in UBCSAT native code, so if an illegal parameter string
     *                   is passed, it will crash the JVM.
     *
     *                   Please consult the documentation for UBCSAT and for SATenstein for information regarding legal parameter
     *                   strings. Alternatively, a simple way to test the legality of a parameter string is to run UBCSAT from
     *                   the command line with that parameter string and specifying a sample .cnf file via the "-inst" flag.
     */
    public UBCSATSolver(UBCSATLibrary library, String parameters, int seedOffset, IPollingService pollingService, String nickname) {
        this.nickname = nickname;
        fLibrary = library;
        this.seedOffset = seedOffset;
        log.debug("Using config {} for UBCSAT", parameters);
        String mutableParameters = parameters;
        if (mutableParameters.contains("-seed ")) {
            throw new IllegalArgumentException("The parameter string cannot contain a seed as it is given upon a call to solve!" + System.lineSeparator() + mutableParameters);
        }
        if (!mutableParameters.contains("-alg ")) {
            throw new IllegalArgumentException("Missing required UBCSAT parameter: -alg." + System.lineSeparator() + mutableParameters);
        }
        if (!mutableParameters.contains("-cutoff ")) {
            mutableParameters = mutableParameters + " -cutoff max";
        }
        String testParameters = mutableParameters + " -seed 1";
        Pointer jnaProblem = fLibrary.initConfig(testParameters);
        fLibrary.destroyProblem(jnaProblem);
        
        fParameters = mutableParameters;
        problemIncrementor = new ProblemIncrementor(pollingService, this);
    }

    @Override
    public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed) {
        return solve(aCNF, null, aTerminationCriterion, aSeed);
    }

    @Override
    public SATSolverResult solve(CNF aCNF, Map<Long, Boolean> aPreviousAssignment, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        final int seed = Math.abs(new Random(aSeed + seedOffset).nextInt());
        final String seededParameters = fParameters + " -seed " + seed;

        final double preTime;
        final double runTime;
        try {
            Preconditions.checkState(fState == null, "Went to solve a new problem, but there is a problem in progress!");
            boolean status;
            if (aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }

            problemIncrementor.scheduleTermination(aTerminationCriterion);
            fState = fLibrary.initConfig(seededParameters);

            if (aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }

            status = fLibrary.initProblem(fState, aCNF.toDIMACS(null));


            // We lock this variable so that the interrupt code will only execute if there is a valid problem to interrupt
            lock.lock();
            isCurrentlySolving.set(true);
            lock.unlock();

            checkStatus(status, fLibrary, fState);

            if (aPreviousAssignment != null) {
                setPreviousAssignment(aPreviousAssignment);
            }

            preTime = watch.getElapsedTime();
            log.debug("PreTime: {}", preTime);

            final double cutoff = aTerminationCriterion.getRemainingTime();
            if (cutoff <= 0 || aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }

            // Start solving
            log.debug("Sending problem to UBCSAT with cutoff time of {} s", cutoff);

            status = fLibrary.solveProblem(fState, cutoff);
            log.trace("Back from solving problem. Acquiring lock");
            lock.lock();
            isCurrentlySolving.set(false);
            lock.unlock();
            log.trace("Checking status");
            checkStatus(status, fLibrary, fState);

            runTime = watch.getElapsedTime() - preTime;
            log.debug("Came back from UBCSAT after {}s (initial cutoff was {} s).", runTime, cutoff);
            if (!(runTime < cutoff + 5)) {
                log.warn("Runtime {} greatly exceeded cutoff {}!", runTime, cutoff);
            }

            return getSolverResult(fLibrary, fState, runTime);
        } finally {
            problemIncrementor.jobDone();
            // Cleanup in the finally block so it always executes: if we instantiated a problem, we make sure that we free it
            if (fState != null) {
                log.debug("Destroying problem");
                lock.lock();
                isCurrentlySolving.set(false);
                lock.unlock();
                fLibrary.destroyProblem(fState);
                fState = null;
            }
            log.debug("Total solver time: {}", watch.getElapsedTime());
        }

    }

    private void checkStatus(boolean status, UBCSATLibrary library, Pointer state) {
        Preconditions.checkState(status, library.getErrorMessage(state));
    }

    private SATSolverResult getSolverResult(UBCSATLibrary fLibrary, Pointer fState, double runtime) {
        final SATResult satResult;
        int resultState = fLibrary.getResultState(fState);
        HashSet<Literal> assignment = null;
        if (resultState == 1) {
            satResult = SATResult.SAT;
            assignment = getAssignment(fLibrary, fState);
        }
        else if (resultState == 2) {
            satResult = SATResult.TIMEOUT;
        }
        else if (resultState == 3) {
            satResult = SATResult.INTERRUPTED;
        }
        else {
            satResult = SATResult.CRASHED;
            log.error("UBCSAT crashed!");
        }
        if(assignment == null) {
            assignment = new HashSet<>();
        }
        return new SATSolverResult(satResult, runtime, assignment, SolvedBy.SATENSTEIN, nickname);
    }

    private HashSet<Literal> getAssignment(UBCSATLibrary fLibrary, Pointer fState) {
        HashSet<Literal> assignment = new HashSet<>();
        IntByReference pRef = fLibrary.getResultAssignment(fState);
        int numVars = pRef.getValue();
        int[] tempAssignment = pRef.getPointer().getIntArray(0, numVars + 1);
        for (int i = 1; i <= numVars; i++) {
            int intLit = tempAssignment[i];
            int var = Math.abs(intLit);
            boolean sign = intLit > 0;
            Literal aLit = new Literal(var, sign);
            assignment.add(aLit);
        }

        return assignment;
    }

    private void setPreviousAssignment(Map<Long, Boolean> aPreviousAssignment) {
        long[] assignment = new long[aPreviousAssignment.size()];
        int i = 0;
        for (Long varID : aPreviousAssignment.keySet()) {
            if (aPreviousAssignment.get(varID)) {
                assignment[i] = varID;
            } else {
                assignment[i] = -varID;
            }
            i++;
        }
        fLibrary.initAssignment(fState, assignment, assignment.length);
    }

    @Override
    public void notifyShutdown() {

    }

    @Override
    public void interrupt() {
        log.trace("Acquiring lock to interrupt UBCSAT");
        lock.lock();
        log.trace("Lock acquired");
        if (isCurrentlySolving.get()) {
            log.debug("Interrupting UBCSAT");
            fLibrary.interrupt(fState);
            log.debug("Interrupt sent to UBCSAT");
        }
        lock.unlock();
    }
}
