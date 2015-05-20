package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import java.io.IOException;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.math3.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.Converter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterion;

import com.google.common.io.Resources;

@Slf4j
public class Clasp3SATSolverTest {

    private static CNF hardCNF;

    @BeforeClass
    public static void init() throws IOException {
        final IStationManager stationManager = new DomainStationManager(Resources.getResource("data/021814SC3M/Domain.csv").getFile());
        final IConstraintManager manager = new ChannelSpecificConstraintManager(stationManager, Resources.getResource("data/021814SC3M/Interference_Paired.csv").getFile());
        Converter.StationPackingProblemSpecs specs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(Resources.getResource("data/srpks/2469-2483_4310537143272356051_107.srpk").getFile());
        final StationPackingInstance instance = new StationPackingInstance(specs.getDomains().entrySet().stream().collect(Collectors.toMap(e -> stationManager.getStationfromID(e.getKey()), e -> e.getValue())), specs.getPreviousAssignment().entrySet().stream().collect(Collectors.toMap(e -> stationManager.getStationfromID(e.getKey()), e-> e.getValue())));
        ISATEncoder aSATEncoder = new SATCompressor(manager);
        Pair<CNF, ISATDecoder> aEncoding = aSATEncoder.encode(instance);
        hardCNF = aEncoding.getKey();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailOnInvalidParameters() {
        final String libraryPath = SATFCFacadeBuilder.findSATFCLibrary();
        final String parameters = "these are not valid parameters";
        log.info(libraryPath);
        new Clasp3SATSolver(libraryPath, parameters);
    }

    // Verify that clasp respects the timeout we send it by sending it a hard CNF with a very low cutoff and making sure it doesn't stall
    @Test(timeout = 3000)
    public void testTimeout() {
        final String libraryPath = SATFCFacadeBuilder.findSATFCLibrary();
        final String parameters = ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1;
        final Clasp3SATSolver clasp3SATSolver = new Clasp3SATSolver(libraryPath, parameters);
        final ITerminationCriterion terminationCriterion = new CPUTimeTerminationCriterion(1.0);
        clasp3SATSolver.solve(hardCNF, terminationCriterion, 1);
    }

}