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
package ca.ubc.cs.beta.stationpacking.solvers.underconstrained;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ConstraintKey;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.TestConstraint;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.TestConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.termination.infinite.NeverEndingTerminationCriterion;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class UnderconstrainedStationFinderTest {

    final Station s1 = new Station(1);
    final Station s2 = new Station(2);
    final Station s3 = new Station(3);
    final Set<Station> stations = Sets.newHashSet(s1, s2, s3);

    @Test
    public void testGetUnderconstrainedStations() throws Exception {
    	List<TestConstraint> testConstraints = new ArrayList<>();
    	stations.stream().forEach(station -> {
            // Everyone co-interferes with everyone on the first two channels, no adj constraints
            IntStream.rangeClosed(1, 2).forEach(channel -> {
            	testConstraints.add(new TestConstraint(ConstraintKey.CO, channel, station, Sets.difference(stations, Sets.newHashSet(station))));
         });
    	});
        IConstraintManager constraintManager = new TestConstraintManager(testConstraints);
        IUnderconstrainedStationFinder finder = new HeuristicUnderconstrainedStationFinder(constraintManager, true);

        final Map<Station, Set<Integer>> domains = ImmutableMap.<Station, Set<Integer>>builder()
                .put(s1, ImmutableSet.of(1, 2, 3))
                .put(s2, ImmutableSet.of(1, 2))
                .put(s3, ImmutableSet.of(1, 2))
                .build();

        final Set<Station> underconstrainedStations = finder.getUnderconstrainedStations(domains, new NeverEndingTerminationCriterion());
        // Regardless of how stations 2 and 3 rearrange themselves, station 1 always has a good channel
        assertEquals(ImmutableSet.of(s1), underconstrainedStations);
    }

}