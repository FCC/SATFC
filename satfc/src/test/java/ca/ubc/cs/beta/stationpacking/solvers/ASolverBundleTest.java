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
package ca.ubc.cs.beta.stationpacking.solvers;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import ca.ubc.cs.beta.stationpacking.StationPackingTestUtils;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.execution.Converter;
import ca.ubc.cs.beta.stationpacking.facade.InternalSATFCConfigFile;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder.SATFCLibLocation;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.YAMLBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.ConfigFile;
import ca.ubc.cs.beta.stationpacking.polling.PollingService;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 22/05/15.
 */
@Slf4j
public abstract class ASolverBundleTest {

    private final String INSTANCE_FILE = "data/ASolverBundleTestInstances.csv";

    protected ManagerBundle managerBundle;

    public ASolverBundleTest() {
        try {
			DomainStationManager stationManager = new DomainStationManager(Resources.getResource("data/021814SC3M/Domain.csv").getFile());
			ChannelSpecificConstraintManager constraintManager = new ChannelSpecificConstraintManager(stationManager, Resources.getResource("data/021814SC3M/Interference_Paired.csv").getFile());
			managerBundle = new ManagerBundle(stationManager, constraintManager, Resources.getResource("data/021814SC3M").getPath());
		} catch (FileNotFoundException e) {
			Assert.fail("Could not find constraint set files");
		}
    }

    protected abstract String getBundleName();

    @Test
    public void testSimplestProblemPossible() throws Exception {
        final SATFCFacadeParameter parameter = SATFCFacadeParameter.builder().configFile(new ConfigFile(getBundleName(), true)).claspLibrary(SATFCFacadeBuilder.findSATFCLibrary(SATFCLibLocation.CLASP)).satensteinLibrary(SATFCFacadeBuilder.findSATFCLibrary(SATFCLibLocation.SATENSTEIN)).build();
        try (final ISolverBundle bundle = new YAMLBundle(managerBundle, parameter, new PollingService(), null)) {
    		final StationPackingInstance instance = StationPackingTestUtils.getSimpleInstance();
            final SolverResult solve = bundle.getSolver(instance).solve(instance, new WalltimeTerminationCriterion(60), 1);
            Assert.assertEquals(StationPackingTestUtils.getSimpleInstanceAnswer(), solve.getAssignment()); // There is only one answer to this problem
    	}
    }

    @Test
    public void testAFewSrpks() throws Exception {
        final SATFCFacadeParameter parameter = SATFCFacadeParameter.builder().configFile(new ConfigFile(getBundleName(), true)).claspLibrary(SATFCFacadeBuilder.findSATFCLibrary(SATFCLibLocation.CLASP)).satensteinLibrary(SATFCFacadeBuilder.findSATFCLibrary(SATFCLibLocation.SATENSTEIN)).build();
        try (final ISolverBundle bundle = new YAMLBundle(managerBundle, parameter, new PollingService(), null)) {
            final List<String> lines = Files.readLines(new File(Resources.getResource(INSTANCE_FILE).getFile()), Charset.defaultCharset());
            final Map<String, SATResult> instanceFileToAnswers = new HashMap<>();
            lines.stream().forEach(line -> {
                final List<String> csvParts = Splitter.on(',').splitToList(line);
                instanceFileToAnswers.put(csvParts.get(0), Enum.valueOf(SATResult.class, csvParts.get(1)));
            });
            for (Map.Entry<String, SATResult> entry : instanceFileToAnswers.entrySet()) {
                final Converter.StationPackingProblemSpecs stationPackingProblemSpecs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(Resources.getResource("data/srpks/" + entry.getKey()).getPath());
                final StationPackingInstance instance = StationPackingTestUtils.instanceFromSpecs(stationPackingProblemSpecs, managerBundle.getStationManager());
                log.info("Solving instance " + entry.getKey());
                final SolverResult solverResult = bundle.getSolver(instance).solve(instance, new WalltimeTerminationCriterion(60), 1);
                Assert.assertEquals(entry.getValue(), solverResult.getResult());
            }
    	}
    }

    public static class SATFCSolverBundleTest extends ASolverBundleTest {

		@Override
		protected String getBundleName() {
			return InternalSATFCConfigFile.SATFC_SEQUENTIAL.getFilename();
		}

    }

    public static class SATFCParallelSolverBundleTest extends ASolverBundleTest {

		@Override
		protected String getBundleName() {
			return InternalSATFCConfigFile.SATFC_PARALLEL.getFilename();
		}

    }
    
}
