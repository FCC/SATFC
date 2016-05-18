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
package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.test.GraphLoader;

/**
 * Created by newmanne on 2015-08-03.
 */
public class AddRandomNeighboursStrategyTest {

    GraphLoader graphLoader;

    @Before
    public void setUp() throws Exception {
        graphLoader = new GraphLoader();
        graphLoader.loadAllGraphs();
    }

    @Test
    public void testMaxExpansionPlowsThroughGraph() throws IOException, URISyntaxException {
        AddRandomNeighboursStrategy addRandomNeighboursStrategy = new AddRandomNeighboursStrategy(Integer.MAX_VALUE);
        Iterable<Set<Station>> stationsToPack = addRandomNeighboursStrategy.getStationsToPack(graphLoader.getLongChainOfNeighbors(), Sets.newHashSet(new Station(0)));
        assertEquals(1, Iterables.size(stationsToPack));
    }

    @Test
    public void testLimit() throws IOException, URISyntaxException {
        AddRandomNeighboursStrategy addRandomNeighboursStrategy = new AddRandomNeighboursStrategy(5);
        Iterable<Set<Station>> stationsToPack = addRandomNeighboursStrategy.getStationsToPack(graphLoader.getLongChainOfNeighbors(), Sets.newHashSet(new Station(0)));
        assertEquals(5, Iterables.size(stationsToPack));
        assertEquals(6, stationsToPack.iterator().next().size());
        assertEquals(26, Iterables.getLast(stationsToPack).size());
    }

    @Test
    public void testFinishLayerBeforeExpandingToNext() throws IOException, URISyntaxException {
        AddRandomNeighboursStrategy addRandomNeighboursStrategy = new AddRandomNeighboursStrategy(3);
        Iterable<Set<Station>> stationsToPack = addRandomNeighboursStrategy.getStationsToPack(graphLoader.getBigConnectedGraph(), Sets.newHashSet(new Station(0)));
        assertEquals(Sets.newHashSet(0,1,2,3,4,10,12).stream().map(Station::new).collect(Collectors.toSet()), Iterables.get(stationsToPack, 1));
    }
    
    @Test
    public void inBetweenLayers() throws IOException, URISyntaxException {
        AddRandomNeighboursStrategy addRandomNeighboursStrategy = new AddRandomNeighboursStrategy(4);
        Iterable<Set<Station>> stationsToPack = addRandomNeighboursStrategy.getStationsToPack(graphLoader.getBigConnectedGraph(), Sets.newHashSet(new Station(0)));
        Set<Station> toPack = Iterables.get(stationsToPack, 1);
        Set<Station> firstLayer = Sets.newHashSet(0,1,2,3,4,10,12).stream().map(Station::new).collect(Collectors.toSet());
        Set<Station> secondLayer = Sets.newHashSet(5,6,8,9,11).stream().map(Station::new).collect(Collectors.toSet());
        assertTrue(toPack.containsAll(firstLayer));
        Set<Station> secondLayerAddedStations = Sets.difference(toPack, firstLayer);
        assertEquals(2, secondLayerAddedStations.size());
        assertTrue(secondLayer.containsAll(secondLayerAddedStations));
    }
    
}