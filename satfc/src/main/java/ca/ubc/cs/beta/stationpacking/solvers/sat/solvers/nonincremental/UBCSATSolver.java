package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import java.util.HashSet;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.UBCSATLibrary;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.NativeUtils;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * The solver that runs different configurations of UBCSAT, including SATenstein.
 *
 * Created by pcernek on 7/28/15.
 */
@Slf4j
public class UBCSATSolver extends AbstractCompressedSATSolver {

    private UBCSATLibrary fLibrary;
    private String fParameters;
    private Pointer fState;

    public UBCSATSolver(String libraryPath, String parameters) {
        this((UBCSATLibrary) Native.loadLibrary(libraryPath, UBCSATLibrary.class, NativeUtils.NATIVE_OPTIONS), parameters);
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
    public UBCSATSolver(UBCSATLibrary library, String parameters) {
        fLibrary = library;
        fParameters = parameters;

        if (parameters.contains("-seed ")) {
            throw new IllegalArgumentException("The parameter string cannot contain a seed as it is given upon a call to solve!");
        }
        if (!parameters.contains("-alg ")) {
            throw new IllegalArgumentException("Missing required UBCSAT parameter: -alg.");
        }
        if (!fParameters.contains("-cutoff ")) {
            fParameters = fParameters + " -cutoff max";
        }
    }

    @Override
    public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed) {
        return solve(aCNF, null, aTerminationCriterion, aSeed);
    }

    @Override
    public SATSolverResult solve(CNF aCNF, Map<Long, Boolean> aPreviousAssignment, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        fParameters = fParameters + " -seed " + aSeed;

        double preTime =0, runTime = 0, postTime = 0;
        try {
            boolean status = false;

            // create the problem
            if (fState != null) {
                throw new IllegalStateException("Went to solve a new problem, but there is a problem in progress!");
            }
            if (aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }

            fState = fLibrary.initConfig(fParameters);

            if (aTerminationCriterion.hasToStop()) {
                return SATSolverResult.timeout(watch.getElapsedTime());
            }

            status = fLibrary.initProblem(fState, aCNF.toDIMACS(null));
            checkStatus(status,fLibrary, fState);

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
            log.debug("Sending problem to UBCSAT with cutoff time of " + cutoff + "s");

            status = fLibrary.solveProblem(fState, cutoff);
            checkStatus(status, fLibrary, fState);

            runTime = watch.getElapsedTime() - preTime;
            log.debug("Came back from UBCSAT after {}s.", runTime);

            final SATSolverResult result = getSolverResult(fLibrary, fState, runTime);
            return result;
        } finally {
            // Cleanup in the finally block so it always executes: if we instantiated a problem, we make sure that we free it
            if (fState != null) {
                log.debug("Destroying problem");
                fLibrary.destroyProblem(fState);
                fState = null;
            }
            log.debug("Total solver time: {}", watch.getElapsedTime());
            watch.stop();
        }

    }

    private void checkStatus(boolean status, UBCSATLibrary library, Pointer state) {
        if(!status) {
            throw new RuntimeException(library.getErrorMessage(state));
        }
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
        return new SATSolverResult(satResult, runtime, assignment);
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
        log.debug("Interrupting UBCSAT");
        fLibrary.interrupt(fState);
        log.debug("Interrupt sent to UBCSAT");
    }
}
