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
package ca.ubc.cs.beta.stationpacking.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.SATFCFacadeTests;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.CSVStationDB;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.base.InstanceParameters;
import ca.ubc.cs.beta.stationpacking.facade.AutoAugmentOptions;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 29/06/15.
 */
@Slf4j
public class AugmentIntegrationTest {

    final int clearingTarget = 38;
    final int numStartingStations = 670;
    final String interference = "/ubc/cs/research/arrow/satfc/public/interference/021814SC3M";
    final String popFile = "/ubc/cs/research/arrow/satfc/public/interference/021814SC3M/Station_Info.csv";

    @Test
    @Ignore
    public void driver() throws Exception {
        final SATFCFacadeBuilder b = new SATFCFacadeBuilder();
        try (SATFCFacade facade = b.build()) {
            final IStationManager manager = new DomainStationManager(interference + File.separator + DataManager.DOMAIN_FILE);
            log.info("Finding starting state with {} stations", numStartingStations);
            final SATFCAugmentState state = getState(manager, facade);
            facade.augment(getDomains(manager), state.getAssignment(), new CSVStationDB(popFile), interference, 60.0);
        }
    }

    @Ignore
    @Test
    public void testAutoAugment() throws Exception {
        final SATFCFacadeBuilder b = new SATFCFacadeBuilder();
        b.setServerURL("http://localhost:8040/satfcserver");
        b.setAutoAugmentOptions(AutoAugmentOptions.builder()
                .augment(true)
                .augmentCutoff(15.0)
                .augmentStationConfigurationFolder(interference)
                .idleTimeBeforeAugmentation(10.0)
                .pollingInterval(1.0)
                .build());
        try (SATFCFacade facade = b.build()) {
            TimeUnit.MINUTES.sleep(2);
            int i = 0;
            for (Map.Entry<InstanceParameters, Pair<SATResult, Double>> entry : SATFCFacadeTests.TEST_CASES.entrySet()) {
                i++;
                log.info("Starting problem " + i);
                InstanceParameters testCase = entry.getKey();
                Pair<SATResult, Double> expectedResult = entry.getValue();

                SATFCResult result = facade.solve(testCase.getDomains(), testCase.getPreviousAssignment(), testCase.Cutoff, testCase.Seed, testCase.fDataFoldername);

                assertEquals(expectedResult.getFirst(), result.getResult());
                if (result.getRuntime() > expectedResult.getSecond()) {
                    log.warn("[WARNING] Test case " + testCase.toString() + " took more time (" + result.getRuntime() + ") than expected (" + expectedResult.getSecond() + ").");
                }
            }
        }
        TimeUnit.MINUTES.sleep(1);
    }

    Map<Integer, Set<Integer>> getDomains(IStationManager manager) {
        final Map<Integer, Set<Integer>> domains = new HashMap<>();
        manager.getStations().stream().forEach(s -> {
            final Set<Integer> collect = manager.getDomain(s).stream().filter(c -> c >= StationPackingUtils.UHFmin && c <= clearingTarget).collect(Collectors.toSet());
            if (!collect.isEmpty()) {
                domains.put(s.getID(), collect);
            }
        });
        return domains;
    }

    private SATFCAugmentState getState(IStationManager manager, SATFCFacade facade) {
        while (true) {
            List<Station> stations = new ArrayList<>(manager.getStations());
            Collections.shuffle(stations);
            final List<Integer> stationsInStartingState = stations.subList(0, numStartingStations).stream().map(Station::getID).collect(Collectors.toList());
            // Solve the initial problem
            final Map<Integer, Set<Integer>> integerSetMap = Maps.filterKeys(getDomains(manager), new Predicate<Integer>() {
                @Override
                public boolean apply(Integer input) {
                    return stationsInStartingState.contains(input);
                }
            });
            final SATFCResult solve = facade.solve(integerSetMap, new HashMap<>(), 60.0, 1, interference);
            if (solve.getResult().equals(SATResult.SAT)) {
                log.info("Can use this as starting state");
                return new SATFCAugmentState(integerSetMap, solve.getWitnessAssignment());
            } else {
                log.info("Do over");
            }
        }

    }

    @Data
    public static class SATFCAugmentState {
        private final Map<Integer, Set<Integer>> domains;
        private final Map<Integer, Integer> assignment;
    }

}
