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
package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Before;
import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.GraphBackedConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.test.GraphLoader;
import ca.ubc.cs.beta.stationpacking.test.StationWholeSetSATCertifier;

/**
 * @author pcernek
 */
public class ConstraintGraphNeighborhoodPresolverTest {

	GraphLoader graphLoader;
	
    private static final int ARBITRARY_CHANNEL = 42;
    private static ITerminationCriterion mockTerminationCriterion;
    private static long arbitrarySeed;


    @Before
    public void setUp() throws Exception {
    	
        // This mock termination criterion is never met.
        mockTerminationCriterion = mock(ITerminationCriterion.class);
        when(mockTerminationCriterion.hasToStop()).thenReturn(false);

        // Completely random seed; value is not actually used since we never use any actual solvers here
        arbitrarySeed = 17;

        graphLoader = new GraphLoader();
        graphLoader.loadAllGraphs();

    }

	@Test
    public void testEmptyGraph() throws Exception {
    	// We expect the solver to return immediately since the previous assignment will be empty.
    	testGraph(graphLoader.getEmptyGraph(), Collections.emptySet(), 0, SATResult.TIMEOUT);
    }

	@Test
	public void testMaxNumberOfNeighborLayers() throws Exception {
		testGraph(graphLoader.getBigConnectedGraph(), graphLoader.getEmptyGraph(), Collections.singleton(new Station(0)),
				0, 0, SATResult.TIMEOUT);
		testGraph(graphLoader.getBigConnectedGraph(), graphLoader.getEmptyGraph(), Collections.singleton(new Station(0)),
				1, 1, SATResult.TIMEOUT);
		testGraph(graphLoader.getBigConnectedGraph(), graphLoader.getEmptyGraph(), Collections.singleton(new Station(0)),
				2, 2, SATResult.TIMEOUT);
		testGraph(graphLoader.getBigConnectedGraph(), graphLoader.getEmptyGraph(), Collections.singleton(new Station(0)),
				3, 3, SATResult.TIMEOUT);
		testGraph(graphLoader.getBigConnectedGraph(), graphLoader.getEmptyGraph(), Collections.singleton(new Station(0)),
				4, 4, SATResult.SAT);
	}

	@Test
    public void testSingletonGraph() throws Exception {
    	SimpleGraph<Station, DefaultEdge> singleton = new SimpleGraph<>(DefaultEdge.class);
    	Station singleStation = new Station(0);
    	singleton.addVertex(singleStation);
    	// A single-node graph should be trivially satisfiable.
    	testGraph(singleton, singleStation, 1, SATResult.SAT);
    }
    
    @Test
	public void testNoNeighbors() throws Exception {
    	// When the graph consists of multiple isolated nodes, we expect the neighbor search to run only one layer deep.
	    testGraph(graphLoader.getNoNeighbors(), new Station(0), 1);
	    // We expect similar behavior even when we start from two nodes simultaneously
	    HashSet<Station> startingStations = new HashSet<>(Arrays.asList(new Station(0), new Station(1)));
		testGraph(graphLoader.getNoNeighbors(), startingStations, 1);
	}

	@Test
	public void testBigConnectedGraph() throws Exception {
		// This serves as a "normal" test case, with one central starting node.
	    testGraph(graphLoader.getBigConnectedGraph(), new Station(0), 4);
	    // We also test the case where we start from two nodes at once.
	    HashSet<Station> startingStations = new HashSet<>(Arrays.asList(new Station(0), new Station(14)));
	    testGraph(graphLoader.getBigConnectedGraph(), startingStations, 2);
	}

	@Test
    public void testClique() throws Exception {
		// Testing a fully connected graph ensures that the neighbor search excludes any nodes that have already been added.
        testGraph(graphLoader.getClique(), new Station(0), 1);
        // We verify that our method doesn't care about whether the graph specified is a CO or ADJ interference graph 
        testGraph(graphLoader.getEmptyGraph(), graphLoader.getClique(), Collections.singleton(new Station(0)),
				ConstraintGraphNeighborhoodPresolver.UNLIMITED_NEIGHBOR_LAYERS, 1, SATResult.SAT);
    }
	
	@Test
	public void testHubAndSpoke() throws Exception {
		/*
		 * Similar to the oneRingOfNeighbors case, except each of the peripheral nodes is connected only to the central node.
		 * In this case we test the scenario in which each of the "peripheral" stations
		 *  is a "new" station, and they all converge to the same neighbor. This should still run only a single iteration of 
		 *  the neighbor search.
		 */
		HashSet<Station> startingStations = new HashSet<>(Arrays.asList(new Station(1), new Station(2), new Station(3),
				new Station(4), new Station(5)));
		testGraph(graphLoader.getHubAndSpoke(), startingStations, 1);
		// Starting from one of the peripheral edges should give two layers of search
		testGraph(graphLoader.getHubAndSpoke(), new Station(1), 2);
	}
	
	@Test
	public void testLongChainOfNeighbors() throws Exception {
		// Visiting the neighbors of a long chain should be the same as visiting each member of that chain individually...
		testGraph(graphLoader.getLongChainOfNeighbors(), new Station(0), 25);
		// ...whether we start from the front or the back.
		testGraph(graphLoader.getLongChainOfNeighbors(), new Station(25), 25);
	}

	@Test
	public void testBipartiteGraph() throws Exception {
		
		SimpleGraph<Station, DefaultEdge> bipartiteGraph = graphLoader.getBipartiteGraph();
		
		// Regardless of the station from which we start, we should only ever travel 2 layers deep
		testGraph(bipartiteGraph, new Station(0), 2);
		testGraph(bipartiteGraph, new Station(1), 2);
		testGraph(bipartiteGraph, new Station(2), 2);
		testGraph(bipartiteGraph, new Station(3), 2);
		
		// Starting from two nodes in the same cluster should yield the same result
		HashSet<Station> startingStations = new HashSet<>(Arrays.asList(new Station(0), new Station(2)));
		testGraph(bipartiteGraph, startingStations, 2);
		
		// Starting from two nodes in opposite clusters should reduce it to a single iteration
		startingStations = new HashSet<>(Arrays.asList(new Station(1), new Station(2)));
		testGraph(bipartiteGraph, startingStations, 1);
	}
	
	@Test
	public void testDisconnectedComponents() throws Exception {
		
		SimpleGraph<Station, DefaultEdge> disconnectedComponents = graphLoader.getDisconnectedComponents();
		
		// When starting from a single node in one of the components, the neighbor search should be unaffected by the 
		//  other component
		testGraph(disconnectedComponents, new Station(0), 2);
		testGraph(disconnectedComponents, new Station(5), 3);
		testGraph(disconnectedComponents, new Station(10), 1);
		
		// When starting from multiple nodes in separate components, the neighbor search should go as deep as the
		//  component with the greatest number of neighbors
		HashSet<Station> startingStations = new HashSet<>(Arrays.asList(new Station(0), new Station(5)));
		testGraph(disconnectedComponents, startingStations, 3);
		startingStations.add(new Station(10));
		testGraph(disconnectedComponents, startingStations, 3);
	}
	
	private StationPackingInstance initializeInstance(SimpleGraph<Station, DefaultEdge> coGraph, 
			SimpleGraph<Station, DefaultEdge> adjGraph, Set<Station> newStations) 
	{
	    Set<Station> allStations = new HashSet<>(coGraph.vertexSet());
	    allStations.addAll(adjGraph.vertexSet());
	
	    Map<Station, Set<Integer>> domains = new HashMap<>();
	    Map<Station, Integer> previousAssignment = new HashMap<>();
	
	    /*
	     * Domains and previous assignment are set to two arbitrary but consecutive channels for all stations;
	     *  we don't particularly care about channels for this test, but the method ConstraintGrouper.getConstraintGraph
	     *  does require both channels to be in a station's domain to function properly.
	     */
	    for(Station station: allStations) {
	        domains.put(station, new HashSet<>(Arrays.asList(ARBITRARY_CHANNEL, ARBITRARY_CHANNEL + 1)));
	        if (!newStations.contains(station)) {
	        	previousAssignment.put(station, ARBITRARY_CHANNEL);
	        	previousAssignment.put(station, ARBITRARY_CHANNEL + 1);
	        }
	    }
	
	    return new StationPackingInstance(domains, previousAssignment);
	}

	private void testGraph(SimpleGraph<Station, DefaultEdge> graph,	Set<Station> startingStations, int numberOfTimesToCall)
	{
		testGraph(graph, startingStations, numberOfTimesToCall, SATResult.SAT);
		
	}

	private void testGraph(SimpleGraph<Station, DefaultEdge> graph,
			Station station, int numberOfTimesToCall, SATResult result)
	{
		testGraph(graph, Collections.singleton(station), numberOfTimesToCall, result);
		
	}

	private void testGraph(SimpleGraph<Station, DefaultEdge> graph, Station startingStation, int numberOfTimesToCall)
	{
		testGraph(graph, Collections.singleton(startingStation), numberOfTimesToCall);
	}
	
	private void testGraph(SimpleGraph<Station, DefaultEdge> graph, Set<Station> startingStations, 
			int numberOfTimesToCall, SATResult expectedResult) 
	{
		testGraph(graph, graphLoader.getEmptyGraph(), startingStations,
				ConstraintGraphNeighborhoodPresolver.UNLIMITED_NEIGHBOR_LAYERS, numberOfTimesToCall, expectedResult);
	}
	
	/**
	 * @param coGraph - the graph representing interference constraints between stations on the same channel.
	 * @param adjGraph - the graph representing interference constraints between stations on adjacent channels.
	 * @param startingStations - the set of stations that is being "added" relative to a previous assignment.
	 * @param expectedNumberOfLayers - the number of layers of neighbors we expect to explore; corresponds to the number
	 * 				of times the certifier is called.
	 * @param expectedResult - the SATResult we expect to receive from the solver.
	 */
	private void testGraph(SimpleGraph<Station, DefaultEdge> coGraph, SimpleGraph<Station, DefaultEdge> adjGraph, 
			Set<Station> startingStations, int maxLayersOfNeighbors, int expectedNumberOfLayers, SATResult expectedResult )
	{
		IConstraintManager constraintManager = new GraphBackedConstraintManager(coGraph, adjGraph);
        
        StationPackingInstance instance = initializeInstance(coGraph, adjGraph, startingStations);
        StationWholeSetSATCertifier certifier = new StationWholeSetSATCertifier(Arrays.asList(coGraph, adjGraph), startingStations);
        List<IStationSubsetCertifier> certifierList = Collections.singletonList(certifier);

        ConstraintGraphNeighborhoodPresolver presolver =
				new ConstraintGraphNeighborhoodPresolver(constraintManager, certifierList, maxLayersOfNeighbors);
        SolverResult result = presolver.solve(instance, mockTerminationCriterion, arbitrarySeed);
        assertEquals(expectedNumberOfLayers, certifier.getNumberOfTimesCalled());
        assertEquals(expectedResult, result.getResult());
	}
}