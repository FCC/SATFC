package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.Clasp3Library;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.collect.ImmutableSet;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

@Slf4j
public class Clasp3SATSolver extends AbstractCompressedSATSolver {

    private Clasp3Library fClaspLibrary;
    private String fParameters;
    private final ScheduledExecutorService fTimerService = Executors.newScheduledThreadPool(1, new SequentiallyNamedThreadFactory("Clasp SAT Solver Timers", true));

    public Clasp3SATSolver(String libraryPath, String parameters) {
        fClaspLibrary = (Clasp3Library) Native.loadLibrary(libraryPath, Clasp3Library.class);
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

    /**
     * Integer flag that we use to keep track of our current request, cutoff and timer threads will only execute if
     * this matches the id when they started.
     */
    private final AtomicLong currentRequestID = new AtomicLong(1);

    /*
     * (non-Javadoc)
     * NOT THREAD SAFE!
     * @see ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver#solve(ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF, double, long)
     */
    @Override
    public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed) {
        Watch watch = Watch.constructAutoStartWatch();

        final long MY_REQUEST_ID = currentRequestID.incrementAndGet();
        final int seed = Math.abs(new Random(aSeed).nextInt());
        final String params = fParameters + " --seed=" + seed;
        Pointer problem = null;
        try  {
            // create the problem - config params have already been validated in the constructor, so this should work
            problem = fClaspLibrary.initConfig(params);
            fClaspLibrary.initProblem(problem, aCNF.toDIMACS(null));

            watch.stop();
            double preTime = watch.getElapsedTime();

            watch.reset();
            watch.start();

            final double cutoff = aTerminationCriterion.getRemainingTime();
            if (cutoff <= 0) {
                log.debug("All time spent.");
                return new SATSolverResult(SATResult.TIMEOUT, preTime, ImmutableSet.of());
            }

            //launches a suicide SATFC time that just kills everything if it finishes and we're still on the same job.
            final int SUICIDE_GRACE_IN_SECONDS = 5 * 60;
            Future<?> suicideFuture = fTimerService.schedule(
                    () -> {
                        if (MY_REQUEST_ID == currentRequestID.get()) {
                            log.error("Clasp has spent {} more seconds than expected ({}) on current run, killing everything (i.e. System.exit(1) ).", SUICIDE_GRACE_IN_SECONDS, cutoff);
                            System.exit(AEATKReturnValues.OH_THE_HUMANITY_EXCEPTION);
                        }
                    }, (long) cutoff + SUICIDE_GRACE_IN_SECONDS, TimeUnit.SECONDS);

            // Start solving
            log.debug("Send problem to clasp cutting off after " + cutoff + "s");

            fClaspLibrary.solveProblem(problem, cutoff);
            log.debug("Came back from clasp.");

            watch.stop();
            final double runtime = watch.getElapsedTime();
            watch.reset();
            watch.start();

            final ClaspResult claspResult = getSolverResult(fClaspLibrary, problem, runtime);

            log.trace("Post time to clasp result obtained: {} s.", watch.getElapsedTime());

            final HashSet<Literal> assignment = parseAssignment(claspResult.getAssignment());
            log.trace("Post time to to assignment obtained: {} s.", watch.getElapsedTime());

            watch.stop();
            final double postTime = watch.getElapsedTime();

            log.trace("Total post time: {} s.", postTime);
            if (postTime > 60) {
                log.error("Clasp SAT solver post solving time was greater than 1 minute, something wrong must have happened.");
            }

            log.debug("Incrementing job srpkToCnfIndex.");
            currentRequestID.incrementAndGet();

            log.debug("Cancelling suicide future.");
            suicideFuture.cancel(true);

            final SATSolverResult output = new SATSolverResult(claspResult.getSATResult(), claspResult.getRuntime() + preTime + postTime, assignment);
            log.trace("Returning result: {}.", output);
            return output;
        } finally {
            if (problem != null) {
                log.trace("Destroying problem");
                fClaspLibrary.destroyProblem(problem);
            }
        }
    }

    @Override
    public void interrupt() throws UnsupportedOperationException {
        // the code for this to occur is there in the fClaspLibrary, but there are synchronization concerns on the java side (keeping a reference to the problem currently being solved, making sure the reference is valid / not destroyed when you interrupt, etc.) that need to be considered before this truly works.
        throw new RuntimeException("Interrupt not yet implemented");
    }

    @Override
    public void notifyShutdown() {
        //No shutdown necessary.
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