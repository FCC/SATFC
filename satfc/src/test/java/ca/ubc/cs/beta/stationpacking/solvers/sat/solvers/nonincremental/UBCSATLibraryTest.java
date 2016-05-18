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

import org.apache.commons.math3.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Resources;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.Converter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.UBCSATLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.EncodingType;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.UBCSATTestHelperLibrary;
import ca.ubc.cs.beta.stationpacking.utils.NativeUtils;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * In adherence to the principle of RAII (Resource Acquisition Is Initialization), and to allow for flexibility in running
 *  different configurations in each test case, each test case instantiates and initializes its own UBCSATLibrary, and
 *  then destroys it.
 *
 * @author pcernek
 */
public class UBCSATLibraryTest {

    private static UBCSATTestHelperLibrary library;
    private Pointer state;


    String dummyCNF = "p cnf 4 20\n-1 2 3 0\n1 -2 4 0\n1 3 4 0\n2 3 -4 0\n-1 -2 -4 0\n1 -2 3 0\n2 -3 4 0\n-2 3 4 0\n-1 3 -4 0\n2 3 4 0\n1 2 -3 0\n1 -3 -4 0\n1 3 -4 0\n-1 2 4 0\n-1 -2 -3 0\n1 2 0\n-2 4 0\n-3 4 0\n2 3 0\n1 2 3 4 0\n";
    String miniUnsatCNF = "p cnf 4 8\n-1 2 4 0\n-2 3 4 0\n1 -3 4 0\n1 -2 -4 0\n2 -3 -4 0\n-1 3 -4 0\n1 2 3 0\n-1 -2 -3 0";

    // This one should be solvable within a few seconds
    String sampleSRPKPath = Resources.getResource("data/srpks/2469-2483_1582973565573889317_33.srpk").getFile();

    private CNF loadSRPKasCNF(String srpkPath) throws IOException {
        final IStationManager stationManager = new DomainStationManager(Resources.getResource("data/021814SC3M/Domain.csv").getFile());
        final IConstraintManager manager = new ChannelSpecificConstraintManager(stationManager, Resources.getResource("data/021814SC3M/Interference_Paired.csv").getFile());
        Converter.StationPackingProblemSpecs specs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(srpkPath);
        final StationPackingInstance instance = new StationPackingInstance(specs.getDomains().entrySet().stream().collect(Collectors.toMap(e -> stationManager.getStationfromID(e.getKey()), Map.Entry::getValue)), specs.getPreviousAssignment().entrySet().stream().collect(Collectors.toMap(e -> stationManager.getStationfromID(e.getKey()), Map.Entry::getValue)));
        ISATEncoder aSATEncoder = new SATCompressor(manager, EncodingType.DIRECT);
        Pair<CNF, ISATDecoder> aEncoding = aSATEncoder.encode(instance);
        return aEncoding.getKey();
    }

    @BeforeClass
    static public void init() {
        String libraryPath = SATFCFacadeBuilder.findSATFCLibrary(SATFCFacadeBuilder.SATFCLibLocation.SATENSTEIN);
        library = (UBCSATTestHelperLibrary) Native.loadLibrary(libraryPath, UBCSATTestHelperLibrary.class, NativeUtils.NATIVE_OPTIONS);
    }

    @Test
    public void testInitProblem() {
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS);
        assertTrue(library.initProblem(state, dummyCNF));

        assertEquals(4, library.getNumVars());
        assertEquals(20, library.getNumClauses());
        library.destroyProblem(state);
    }

    @Test
    public void testInitAssignment() {
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS);
        assertTrue(library.initProblem(state, dummyCNF));

        long[] assignment = new long[]{1, 2, -3};

        assertTrue(library.initAssignment(state, assignment, 3));
        library.runInitData();

        for(int i=0; i < assignment.length; i++) {
            assertEquals(assignment[i] > 0, library.getVarAssignment(i + 1));
        }
        library.destroyProblem(state);
    }

    @Test
    public void testSolveProblemDefaultConfig() throws IOException {
        state = library.initConfig(UBCSATLibSATSolverParameters.DEFAULT_SATENSTEIN);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));

        assertEquals(1, library.getResultState(state));

        IntByReference pRef = library.getResultAssignment(state);
        int size = pRef.getValue();
        int[] assignment = pRef.getPointer().getIntArray(0, size + 1);

        assertEquals(true, assignment[1] > 0);
        assertEquals(false, assignment[2] > 0);
        assertEquals(true, assignment[3] > 0);
        assertEquals(true, assignment[4] > 0);
        library.destroyProblem(state);
    }

    @Test
    public void testSolveProblemConfig1() throws IOException {
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));

        assertEquals(1, library.getResultState(state));

        IntByReference pRef = library.getResultAssignment(state);
        int size = pRef.getValue();
        int[] assignment = pRef.getPointer().getIntArray(0, size + 1);

        assertEquals(true, assignment[1] > 0);
        assertEquals(false, assignment[2] > 0);
        assertEquals(true, assignment[3] > 0);
        assertEquals(true, assignment[4] > 0);
        library.destroyProblem(state);
    }

    @Test
    public void testSolveProblemConfig2() throws IOException {
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_R3SAT_PARAMILS);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));

        assertEquals(1, library.getResultState(state));

        IntByReference pRef = library.getResultAssignment(state);
        int size = pRef.getValue();
        int[] assignment = pRef.getPointer().getIntArray(0, size + 1);

        assertEquals(true, assignment[1] > 0);
        assertEquals(false, assignment[2] > 0);
        assertEquals(true, assignment[3] > 0);
        assertEquals(true, assignment[4] > 0);
        library.destroyProblem(state);
    }

    @Test
    public void testSolveProblemConfig3() throws IOException {
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_FAC_PARAMILS);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));

        assertEquals(1, library.getResultState(state));

        IntByReference pRef = library.getResultAssignment(state);
        int size = pRef.getValue();
        int[] assignment = pRef.getPointer().getIntArray(0, size + 1);

        assertEquals(true, assignment[1] > 0);
        assertEquals(false, assignment[2] > 0);
        assertEquals(true, assignment[3] > 0);
        assertEquals(true, assignment[4] > 0);
        library.destroyProblem(state);
    }

    @Test
    public void testSolveTwoDifferentProblems() throws IOException {
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS);
        CNF sampleSRPK = loadSRPKasCNF(sampleSRPKPath);
        assertTrue( library.initProblem(state, sampleSRPK.toDIMACS(new String[]{})) );
        assertTrue(library.solveProblem(state, 60));
        assertEquals(1, library.getResultState(state));

        library.destroyProblem(state);

        state = library.initConfig(UBCSATLibSATSolverParameters.DEFAULT_SATENSTEIN);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 5));
        assertEquals(1, library.getResultState(state));
        library.destroyProblem(state);
    }

    @Test
    public void testSameProblemDifferentConfigs() {
        /* The following strings of parameters were found to perform well for the SATenstein paper on other (non-SATFC)
            instance distributions */
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));
        assertEquals(1, library.getResultState(state));
        library.destroyProblem(state);

        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_R3SAT_PARAMILS);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));
        assertEquals(1, library.getResultState(state));
        library.destroyProblem(state);

        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_FAC_PARAMILS);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));
        assertEquals(1, library.getResultState(state));
        library.destroyProblem(state);
    }

    @Test
    public void testSameProblemHundredTimes() {
        for (int i=0; i < 100; i++) {
            state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS);
            assertTrue(library.initProblem(state, dummyCNF));
            assertTrue(library.solveProblem(state, 0.5));

            assertEquals(1, library.getResultState(state));

            IntByReference pRef = library.getResultAssignment(state);
            int size = pRef.getValue();
            int[] assignment = pRef.getPointer().getIntArray(0, size + 1);

            assertEquals(true, assignment[1] > 0);
            assertEquals(false, assignment[2] > 0);
            assertEquals(true, assignment[3] > 0);
            assertEquals(true, assignment[4] > 0);
            library.destroyProblem(state);
        }
    }

    @Test
    public void testDCCA() {
        String dccaParams = "-alg dcca";
        state = library.initConfig(dccaParams);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));

        assertEquals(1, library.getResultState(state));

        IntByReference pRef = library.getResultAssignment(state);
        int size = pRef.getValue();
        int[] assignment = pRef.getPointer().getIntArray(0, size + 1);

        assertEquals(true, assignment[1] > 0);
        assertEquals(false, assignment[2] > 0);
        assertEquals(true, assignment[3] > 0);
        assertEquals(true, assignment[4] > 0);
        library.destroyProblem(state);
    }

    @Test
    public void testsparrow() {
        String sparrowParams = "-alg sparrow";
        state = library.initConfig(sparrowParams);
        assertTrue(library.initProblem(state, dummyCNF));
        assertTrue(library.solveProblem(state, 0.5));

        assertEquals(1, library.getResultState(state));

        IntByReference pRef = library.getResultAssignment(state);
        int size = pRef.getValue();
        int[] assignment = pRef.getPointer().getIntArray(0, size + 1);

        assertEquals(true, assignment[1] > 0);
        assertEquals(false, assignment[2] > 0);
        assertEquals(true, assignment[3] > 0);
        assertEquals(true, assignment[4] > 0);
        library.destroyProblem(state);
    }

    @Test
    public void testInterrupt() throws IOException, InterruptedException {
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS);
        library.initProblem(state, miniUnsatCNF);


        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(200);
                library.interrupt(state);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        interrupter.start();

        Watch watch = Watch.constructAutoStartWatch();
        library.solveProblem(state, 60);
        watch.stop();

        interrupter.join();

        assertTrue(watch.getElapsedTime() < 1);
        assertEquals(3, library.getResultState(state));
        library.destroyProblem(state);
    }

    @Test
    public void testTimeout() throws IOException {
        Watch watch = Watch.constructAutoStartWatch();
        state = library.initConfig(UBCSATLibSATSolverParameters.STEIN_QCP_PARAMILS);

        library.initProblem(state, miniUnsatCNF);
        library.solveProblem(state, 0.5);

        watch.stop();

        assertTrue(watch.getElapsedTime() < 1);
        assertEquals(2, library.getResultState(state));
        library.destroyProblem(state);
    }

}