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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class UnderconstrainedStationFinderTest {

    @Test
    public void testGetUnderconstrainedStations() {
        IConstraintManager constraintManager = mock(IConstraintManager.class);
        UnderconstrainedStationFinder finder = new UnderconstrainedStationFinder(constraintManager);

        final ImmutableList<Station> stations = IntStream.rangeClosed(1, 3).mapToObj(Station::new).collect(GuavaCollectors.toImmutableList());
        final Map<Station, Set<Integer>> domains = ImmutableMap.<Station, Set<Integer>>builder()
                .put(stations.get(0), ImmutableSet.of(1, 2, 3))
                .put(stations.get(1), ImmutableSet.of(1, 2))
                .put(stations.get(2), ImmutableSet.of(1, 2))
                .build();
        stations.forEach(station -> {
            // Everyone co-interferes with everyone on the first two channels, no adj constraints
            IntStream.rangeClosed(1, 2).forEach(channel -> {
                when(constraintManager.getCOInterferingStations(station, channel)).thenReturn(Sets.difference(ImmutableSet.copyOf(stations), ImmutableSet.of(station)));
            });
        });
        final Set<Station> underconstrainedStations = finder.getUnderconstrainedStations(domains);
        // Regardless of how stations 2 and 3 rearrange themselves, station 1 always has a good channel
        assertEquals(ImmutableSet.of(stations.get(0)), underconstrainedStations);
    }
}