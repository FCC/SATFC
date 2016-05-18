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
package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Resources;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.Converter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.UBCSATLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.EncodingType;
import ca.ubc.cs.beta.stationpacking.polling.IPollingService;
import ca.ubc.cs.beta.stationpacking.polling.PollingService;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ubcsat.UBCSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * @author pcernek
 */
public class UBCSATSolverTest {

    private static final String libraryPath = SATFCFacadeBuilder.findSATFCLibrary(SATFCFacadeBuilder.SATFCLibLocation.SATENSTEIN);

    private static SATEncoder.CNFEncodedProblem unsatProblem;
    private static SATEncoder.CNFEncodedProblem easyProblem;
    private static SATEncoder.CNFEncodedProblem moderateProblem;
    private static SATEncoder.CNFEncodedProblem otherProblem;
    final IPollingService pollingService = new PollingService();

    @BeforeClass
    public static void init() throws IOException {
        final IStationManager stationManager = new DomainStationManager(Resources.getResource("data/021814SC3M/Domain.csv").getFile());
        final IConstraintManager manager = new ChannelSpecificConstraintManager(stationManager, Resources.getResource("data/021814SC3M/Interference_Paired.csv").getFile());
        ISATEncoder aSATEncoder = new SATCompressor(manager, EncodingType.DIRECT);

        unsatProblem = parseSRPK(Resources.getResource("data/srpks/2469-2483_1671735211211766343_33.srpk").getFile(), aSATEncoder, stationManager);
        easyProblem = parseSRPK(Resources.getResource("data/srpks/2469-2483_1582973565573889317_33.srpk").getFile(), aSATEncoder, stationManager);
        moderateProblem = parseSRPK(Resources.getResource("data/srpks/2469-2483_4310537143272356051_107.srpk").getFile(), aSATEncoder, stationManager);
        otherProblem = parseSRPK(Resources.getResource("data/srpks/2469-2483_1853993831659981677_29.srpk").getFile(), aSATEncoder, stationManager);

    }

    @Test
    public void testSolveEasy() throws Exception {
        UBCSATSolver solver = new UBCSATSolver(libraryPath, UBCSATLibSATSolverParameters.DEFAULT_SATENSTEIN, pollingService);
        SATEncoder.CNFEncodedProblem currentProblem = easyProblem;
        ITerminationCriterion terminationCriterion = new CPUTimeTerminationCriterion(1.0);
        SATSolverResult result = solver.solve(currentProblem.getCnf(), currentProblem.getInitialAssignment(), terminationCriterion, 1);

        assertEquals(SATResult.SAT, result.getResult());
        checkSolutionMakesSense(result, currentProblem.getCnf().getVariables().size());
    }

    @Test
    public void testTimeout() throws Exception {
        UBCSATSolver solver = new UBCSATSolver(libraryPath, UBCSATLibSATSolverParameters.DEFAULT_SATENSTEIN, pollingService);

        Watch watch = Watch.constructAutoStartWatch();

        ITerminationCriterion terminationCriterion = new WalltimeTerminationCriterion(0.5);
        SATSolverResult result = solver.solve(unsatProblem.getCnf(), unsatProblem.getInitialAssignment(), terminationCriterion, 1);
        double solvingTime = watch.getElapsedTime();
        System.out.println("Solving time: " + solvingTime);
        watch.stop();

        assertEquals(SATResult.TIMEOUT, result.getResult());
        assertTrue(solvingTime < 2);
    }

    @Test
    public void testSolveMultipleInstances() throws Exception {
        UBCSATSolver solver = new UBCSATSolver(libraryPath, UBCSATLibSATSolverParameters.DEFAULT_SATENSTEIN, pollingService);

        // Problem 1
        SATEncoder.CNFEncodedProblem currentProblem = easyProblem;
        ITerminationCriterion terminationCriterion = new CPUTimeTerminationCriterion(10.0);
        SATSolverResult result = solver.solve(currentProblem.getCnf(), currentProblem.getInitialAssignment(), terminationCriterion, 1);
        assertEquals(SATResult.SAT, result.getResult());
        checkSolutionMakesSense(result, currentProblem.getCnf().getVariables().size());

        // Problem 2
        currentProblem = moderateProblem;
        terminationCriterion = new CPUTimeTerminationCriterion(10.0);
        result = solver.solve(currentProblem.getCnf(), currentProblem.getInitialAssignment(), terminationCriterion, 1);
        assertEquals(SATResult.SAT, result.getResult());
        checkSolutionMakesSense(result, currentProblem.getCnf().getVariables().size());

        // Problem 3
        currentProblem = otherProblem;
        terminationCriterion = new CPUTimeTerminationCriterion(10.0);
        result = solver.solve(currentProblem.getCnf(), currentProblem.getInitialAssignment(), terminationCriterion, 1);
        assertEquals(SATResult.SAT, result.getResult());
        checkSolutionMakesSense(result, currentProblem.getCnf().getVariables().size());

    }

    @Test
    public void testSolveMultipleConfigsSameInstance() {
        SATEncoder.CNFEncodedProblem currentProblem = easyProblem;
        ITerminationCriterion terminationCriterion;
        SATSolverResult result;

        UBCSATSolver solver = new UBCSATSolver(libraryPath, UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS, pollingService);
        terminationCriterion = new CPUTimeTerminationCriterion(1.0);
        result = solver.solve(currentProblem.getCnf(), currentProblem.getInitialAssignment(), terminationCriterion, 1);
        assertEquals(SATResult.SAT, result.getResult());
        checkSolutionMakesSense(result, currentProblem.getCnf().getVariables().size());

        solver = new UBCSATSolver(libraryPath, UBCSATLibSATSolverParameters.STEIN_R3SAT_PARAMILS, pollingService);
        terminationCriterion = new CPUTimeTerminationCriterion(1.0);
        result = solver.solve(currentProblem.getCnf(), currentProblem.getInitialAssignment(), terminationCriterion, 1);
        assertEquals(SATResult.SAT, result.getResult());
        checkSolutionMakesSense(result, currentProblem.getCnf().getVariables().size());

        solver = new UBCSATSolver(libraryPath, UBCSATLibSATSolverParameters.STEIN_FAC_PARAMILS, pollingService);
        terminationCriterion = new CPUTimeTerminationCriterion(1.0);
        result = solver.solve(currentProblem.getCnf(), currentProblem.getInitialAssignment(), terminationCriterion, 1);
        assertEquals(SATResult.SAT, result.getResult());
        checkSolutionMakesSense(result, currentProblem.getCnf().getVariables().size());
    }

    @Test
    public void testSameProblemHundredTimes() {
//        SATEncoder.CNFEncodedProblem currentProblem = otherProblem;
        SATEncoder.CNFEncodedProblem currentProblem = easyProblem;

        ITerminationCriterion terminationCriterion;
        UBCSATSolver solver;
        SATSolverResult result;

        for (int i=0; i < 100; i++) {
            solver = new UBCSATSolver(libraryPath, UBCSATLibSATSolverParameters.STEIN_FAC_PARAMILS, pollingService);
            terminationCriterion = new CPUTimeTerminationCriterion(1.0);
            result = solver.solve(currentProblem.getCnf(), currentProblem.getInitialAssignment(), terminationCriterion, 1);
            assertEquals(SATResult.SAT, result.getResult());
            checkSolutionMakesSense(result, currentProblem.getCnf().getVariables().size());
        }
    }

    @Test
    public void testInterrupt() throws Exception {
        UBCSATSolver solver = new UBCSATSolver(libraryPath, UBCSATLibSATSolverParameters.DEFAULT_SATENSTEIN, pollingService);

        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(200);
                solver.interrupt();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Watch watch = new Watch();
        watch.start();

        ITerminationCriterion terminationCriterion = new CPUTimeTerminationCriterion(5);
        interrupter.start();
        SATSolverResult result = solver.solve(unsatProblem.getCnf(), unsatProblem.getInitialAssignment(), terminationCriterion, 1);

        watch.stop();
        interrupter.join();
        assertEquals(SATResult.INTERRUPTED, result.getResult());
        assertTrue(watch.getElapsedTime() < 0.5);
    }

    private static SATEncoder.CNFEncodedProblem parseSRPK(String srpkFilePath, ISATEncoder encoder, IStationManager stationManager) throws IOException {
        Converter.StationPackingProblemSpecs problemSpecs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(srpkFilePath);
        StationPackingInstance instance = new StationPackingInstance(problemSpecs.getDomains().entrySet().stream().collect(Collectors.toMap(e -> stationManager.getStationfromID(e.getKey()), Map.Entry::getValue)), problemSpecs.getPreviousAssignment().entrySet().stream().collect(Collectors.toMap(e -> stationManager.getStationfromID(e.getKey()), Map.Entry::getValue)));
        return encoder.encodeWithAssignment(instance);
    }

    private static void checkSolutionMakesSense(SATSolverResult result, int numVars) {
        assertEquals(numVars, result.getAssignment().size());

        for (int i=1; i <= numVars; i++) {
            boolean containsPositiveLit = result.getAssignment().contains(new Literal(i, true));
            boolean containsNegativeLit = result.getAssignment().contains(new Literal(i, false));
            assertTrue( containsPositiveLit != containsNegativeLit );
        }
    }
}