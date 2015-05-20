package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Collections2;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.test.GraphLoader;

import static org.junit.Assert.assertTrue;

public class GraphBackedConstraintManagerTest {
	
	GraphLoader graphLoader;
	int ARBITRARY_CHANNEL = 42;
	
	@Before
	public void setUp() throws Exception {
		graphLoader = new GraphLoader();
		graphLoader.loadAllGraphs();
	}

	/**
	 * Ensure that a station with no neighbors has no interfering stations.
	 */
	@Test
	public void testNoNeighborsHasNoInterferingStations () throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getNoNeighbors(), graphLoader.getNoNeighbors());
		assertTrue(cm.getCOInterferingStations(new Station(0), ARBITRARY_CHANNEL).isEmpty());
		assertTrue(cm.getADJplusInterferingStations(new Station(0), ARBITRARY_CHANNEL).isEmpty());
	}
	
	@Test
	public void testGetCOInterferingStations() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getBigConnectedGraph(), graphLoader.getBigConnectedGraph());

		// Ensure that output is what we expect for a toy example
		Set<Station> neighbors = new HashSet<>(Arrays.asList(new Station(6), new Station(8), new Station(0)));
		assertTrue(cm.getCOInterferingStations(new Station(2), ARBITRARY_CHANNEL).equals(neighbors));
		
		// Ensure that the empty set is correctly returned 
		//  for stations that are not in the interference graph being queried
		assertTrue(cm.getCOInterferingStations(new Station(99), ARBITRARY_CHANNEL).isEmpty());
	}
	
	@Test
	public void testGetADJInterferingStations() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getBigConnectedGraph(), graphLoader.getBigConnectedGraph());

		// Ensure that output is what we expect for a toy example
		Set<Station> neighbors = new HashSet<>(Arrays.asList(new Station(6), new Station(8), new Station(0)));
		assertTrue(cm.getADJplusInterferingStations(new Station(2), ARBITRARY_CHANNEL).equals(neighbors));
		
		// Ensure that the empty set is correctly returned 
		//  for stations that are not in the interference graph being queried
		assertTrue(cm.getADJplusInterferingStations(new Station(99), ARBITRARY_CHANNEL).isEmpty());
	}
	
	/**
	 * CO graph only: In the most basic case, an assignment that puts two neighbors on the same channel is not SAT.
	 */
	@Test
	public void testNeighborsOnSameChannel() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getHubAndSpoke(), graphLoader.getEmptyGraph());
		Map<Integer, Set<Station>> assignment = new HashMap<>();
		
		HashSet<Station> interferingStations = new HashSet<>(Arrays.asList(new Station(0), new Station(1)));
		HashSet<Station> nonInterferingStations = new HashSet<>(Arrays.asList(new Station(2), new Station(3), new Station(4), new Station(5)));

		assignment.put(13, interferingStations);
		assignment.put(14, nonInterferingStations);

		assertTrue(!cm.isSatisfyingAssignment(assignment));
	}
	
	/**
	 * Even when the CO graph is fully connected, if each station is on a separate channel, then the assignment is SAT.   
	 */
	@Test
	public void testAllDifferentChannels() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getClique(), graphLoader.getEmptyGraph());
		Map<Integer, Set<Station>> assignment = new HashMap<>();

		assignment.put(11, Collections.singleton(new Station(0)));
		assignment.put(12, Collections.singleton(new Station(1)));
		assignment.put(13, Collections.singleton(new Station(2)));
		assignment.put(14, Collections.singleton(new Station(3)));
		assignment.put(15, Collections.singleton(new Station(4)));
		assignment.put(16, Collections.singleton(new Station(5)));
		
		assertTrue(cm.isSatisfyingAssignment(assignment));
	}
	
	/**
	 * CO graph only: Two non-neighbors on the same channel should be SAT.
	 */
	@Test
	public void testNoNeighborsOnSameChannel() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getHubAndSpoke(), graphLoader.getEmptyGraph());
		Map<Integer, Set<Station>> assignment = new HashMap<>();
		
		HashSet<Station> sameChannelButNotNeighbors = new HashSet<>(Arrays.asList(new Station(1), new Station(2)));
		assignment.put(13, sameChannelButNotNeighbors);
		
		assignment.put(11, Collections.singleton(new Station(0)));
		assignment.put(14, Collections.singleton(new Station(3)));
		assignment.put(15, Collections.singleton(new Station(4)));
		assignment.put(16, Collections.singleton(new Station(5)));

		assertTrue(cm.isSatisfyingAssignment(assignment));
	}
	
	/**
	 * ADJ graph only: Neighbors assigned to the bottom channel and the next channel up should fail.
	 */
	@Test
	public void testNeighborsOnBottomTwoChannels() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getEmptyGraph(), graphLoader.getHubAndSpoke());
		Map<Integer, Set<Station>> assignment = new HashMap<>();

		assignment.put(11, Collections.singleton(new Station(0)));
		assignment.put(12, Collections.singleton(new Station(1)));
		assignment.put(14, Collections.singleton(new Station(2)));
		assignment.put(16, Collections.singleton(new Station(3)));
		assignment.put(18, Collections.singleton(new Station(4)));
		assignment.put(20, Collections.singleton(new Station(5)));
		
		assertTrue(!cm.isSatisfyingAssignment(assignment));
	}
	
	/**
	 * ADJ graph only: Neighbors assigned to the top channel and the next channel down should fail.
	 */
	@Test
	public void testNeighborsOnTopTwoChannels() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getEmptyGraph(), graphLoader.getHubAndSpoke());
		Map<Integer, Set<Station>> assignment = new HashMap<>();

		assignment.put(22, Collections.singleton(new Station(0)));
		assignment.put(23, Collections.singleton(new Station(1)));
		assignment.put(14, Collections.singleton(new Station(2)));
		assignment.put(16, Collections.singleton(new Station(3)));
		assignment.put(18, Collections.singleton(new Station(4)));
		assignment.put(20, Collections.singleton(new Station(5)));
		
		assertTrue(!cm.isSatisfyingAssignment(assignment));
	}
	
	/**
	 * ADJ graph only: Even in a fully connected graph, if all neighbors are at least two channels apart, 
	 * 	then the assignment is SAT.
	 */
	@Test
	public void testNeighborsTwoChannelsApart() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getEmptyGraph(), graphLoader.getClique());
		Map<Integer, Set<Station>> assignment = new HashMap<>();

		assignment.put(10, Collections.singleton(new Station(0)));
		assignment.put(12, Collections.singleton(new Station(1)));
		assignment.put(14, Collections.singleton(new Station(2)));
		assignment.put(16, Collections.singleton(new Station(3)));
		assignment.put(18, Collections.singleton(new Station(4)));
		assignment.put(20, Collections.singleton(new Station(5)));
		
		assertTrue(cm.isSatisfyingAssignment(assignment));
	}
	
	/**
	 * An edgeless graph should recognize every assignment as SAT, even when every station is on the same channel.
	 */
	@Test
	public void edgelessGraphOnSameChannel() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getNoNeighbors(), graphLoader.getNoNeighbors());
		Map<Integer, Set<Station>> assignment = new HashMap<>();

		Station station0 = new Station(0);
		Station station1 = new Station(1);
		Station station2 = new Station(2);
		Station station3 = new Station(3);
		
		assignment.put(11, new HashSet<>(Arrays.asList(station0, station1, station2, station3)));
		assertTrue(cm.isSatisfyingAssignment(assignment));
	}
	
	/**
	 * An edgeless graph should always be SAT, even when the stations are all 1 channel apart.
	 * This method tests every possible configuration of 4 stations being 1 channel apart.
	 */
	@Test
	public void edgelessGraphOnSameChannelPlus() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getNoNeighbors(), graphLoader.getNoNeighbors());

		List<Station> stations = new ArrayList<>(Arrays.asList(new Station(0), new Station(1), new Station(2), new Station(3)));
		List<Integer> channels = new ArrayList<>(Arrays.asList(11,12,13,14));
		
		for (List<Integer> channelPermutation : Collections2.orderedPermutations(channels)) {
			Map<Integer, Set<Station>> assignment = new HashMap<>();
			for (int i=0; i < 4; i++) {
				assignment.put(channelPermutation.get(i), Collections.singleton(stations.get(i)));
			}
			assertTrue(cm.isSatisfyingAssignment(assignment));
		}
		
	}

	/**
	 * An empty assignment should pass trivially, regardless of the graph
	 */
	@Test
	public void testEmptyAssignment() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getBigConnectedGraph(), graphLoader.getDisconnectedComponents());
		Map<Integer, Set<Station>> assignment = new HashMap<>();
		
		assertTrue(cm.isSatisfyingAssignment(assignment));
	}
	
	/**
	 * An empty graph should pass any assignment trivially, even when all stations are on same channel.
	 */
	@Test
	public void emptyConstraintGraph() throws Exception {
		GraphBackedConstraintManager cm = new GraphBackedConstraintManager(graphLoader.getEmptyGraph(), graphLoader.getEmptyGraph());
		Map<Integer, Set<Station>> assignment = new HashMap<>();

		Station station0 = new Station(0);
		Station station1 = new Station(1);
		Station station2 = new Station(2);
		Station station3 = new Station(3);
		
		assignment.put(11, new HashSet<>(Arrays.asList(station0, station1, station2, station3)));
		assertTrue(cm.isSatisfyingAssignment(assignment));
	}
	
	
}
