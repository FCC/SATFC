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
package ca.ubc.cs.beta.stationpacking.solvers;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.execution.Converter;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.SATFCParallelSolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.SATFCSolverBundle;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.StationPackingTestUtils;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterion;

import com.google.common.io.Resources;

/**
 * Created by newmanne on 22/05/15.
 */
public abstract class ASolverBundleTest {

    private final String INSTANCE_FILE = "data/ASolverBundleTestInstances.csv";

    protected IStationManager stationManager;
    protected IConstraintManager constraintManager;

    public ASolverBundleTest() throws FileNotFoundException {
        stationManager = new DomainStationManager(Resources.getResource("data/021814SC3M/Domain.csv").getFile());
        constraintManager = new ChannelSpecificConstraintManager(stationManager, Resources.getResource("data/021814SC3M/Interference_Paired.csv").getFile());
    }

    protected abstract ISolverBundle getBundle();

    @Test
    public void testSimplestProblemPossible() {
        ISolverBundle bundle = getBundle();
        final StationPackingInstance instance = StationPackingTestUtils.getSimpleInstance();
        final SolverResult solve = bundle.getSolver(instance).solve(instance, new CPUTimeTerminationCriterion(60.0), 1);
        Assert.assertEquals(StationPackingTestUtils.getSimpleInstanceAnswer(), solve.getAssignment()); // There is only one answer to this problem
    }

    @Test
    public void testAFewSrpks() throws Exception {
        ISolverBundle bundle = getBundle();
        final List<String> lines = Files.readLines(new File(Resources.getResource(INSTANCE_FILE).getFile()), Charset.defaultCharset());
        final Map<String, SATResult> instanceFileToAnswers = new HashMap<>();
        lines.stream().forEach(line -> {
            final List<String> csvParts = Splitter.on(',').splitToList(line);
            instanceFileToAnswers.put(csvParts.get(0), Enum.valueOf(SATResult.class, csvParts.get(1)));
        });
        for (Map.Entry<String, SATResult> entry : instanceFileToAnswers.entrySet()) {
            final Converter.StationPackingProblemSpecs stationPackingProblemSpecs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(Resources.getResource("data/srpks/" + entry.getKey()).getPath());
            final StationPackingInstance instance = StationPackingTestUtils.instanceFromSpecs(stationPackingProblemSpecs, stationManager);
            final SolverResult solverResult = bundle.getSolver(instance).solve(instance, new CPUTimeTerminationCriterion(60.0), 1);
            Assert.assertEquals(entry.getValue(), solverResult.getResult());
        }
    }

    public static class SATFCSolverBundleTest extends ASolverBundleTest {

        public SATFCSolverBundleTest() throws FileNotFoundException {
        }

        @Override
        protected ISolverBundle getBundle() {
            return new SATFCSolverBundle(SATFCFacadeBuilder.findSATFCLibrary(), stationManager, constraintManager, null, true, true, true, null);
        }

    }

    public static class SATFCParallelSolverBundleTest extends ASolverBundleTest {

        public SATFCParallelSolverBundleTest() throws FileNotFoundException {
        }

        @Override
        protected ISolverBundle getBundle() {
            return new SATFCParallelSolverBundle(SATFCFacadeBuilder.findSATFCLibrary(), stationManager, constraintManager, null, true, true, true, null, Runtime.getRuntime().availableProcessors());
        }

    }

}
