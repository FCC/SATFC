package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import ca.ubc.cs.beta.stationpacking.base.Station;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;

/**
 * A mock ConstraintManager useful for running tests where the focus is on the shape of interference constraint graphs
 *  between stations.
 * @author pcernek
 */
public class GraphBackedConstraintManager implements IConstraintManager {

    private final NeighborIndex<Station, DefaultEdge> coInterferingStationNeighborIndex;
    private final NeighborIndex<Station, DefaultEdge> adjInterferingStationNeighborIndex;
    
    private final SimpleGraph<Station, DefaultEdge> coInterferingStationGraph;
    private final SimpleGraph<Station, DefaultEdge> adjInterferingStationGraph;

    /**
     * Creates a constraint manager backed by the given interference graphs.
     * @param coInterferingStationGraph - the graph representing CO interfering stations.
     * @param adjInterferingStationGraph - the graph representing ADJ interfering stations.
     */
    public GraphBackedConstraintManager(SimpleGraph<Station, DefaultEdge> coInterferingStationGraph,
                                        SimpleGraph<Station, DefaultEdge> adjInterferingStationGraph) {

        this.coInterferingStationGraph = coInterferingStationGraph;
        this.adjInterferingStationGraph = adjInterferingStationGraph;
    	
    	this.coInterferingStationNeighborIndex = new NeighborIndex<>(coInterferingStationGraph);
        this.adjInterferingStationNeighborIndex = new NeighborIndex<>(adjInterferingStationGraph);

    }

    /**
     * Returns the set of all stations neighboring the given station in the underlying graph representing interference 
     * 	constraints between stations on the same channel.
     * @param aStation -the station for which to return the set of CO-interfering stations.
     * @param aChannel - this parameter is ignored, since the constraint graph has been defined explicitly in the construction
     * 			of this constraint manager. It is retained for compatibility with the interface.
     * @return the set of stations which, if they were on the same channel as the given station, would interfere with it.
     */
    @Override
    public Set<Station> getCOInterferingStations(Station aStation, int aChannel) {
    	if (this.coInterferingStationGraph.containsVertex(aStation)) {
    		return this.coInterferingStationNeighborIndex.neighborsOf(aStation);
    	}
    	return Collections.emptySet();
    }

    /**
     * Returns the set of all stations neighboring the given station in the underlying graph representing interference 
     * 	constraints between stations on adjacent channels.
     * @param aStation -the station for which to return the set of ADJ-interfering stations.
     * @param aChannel - this parameter is ignored, since the constraint graph has been defined explicitly in the construction
     * 			of this constraint manager. It is retained for compatibility with the interface.
     * @return the set of stations which, if they were on a channel adjacent to the channel of the given station,
     * 	 would interfere with it.
     */
    @Override
    public Set<Station> getADJplusInterferingStations(Station aStation, int aChannel) {
    	if (this.adjInterferingStationGraph.containsVertex(aStation)) {
    		return this.adjInterferingStationNeighborIndex.neighborsOf(aStation);
    	}
    	return Collections.emptySet();
    }

    @Override
    public boolean isSatisfyingAssignment(Map<Integer, Set<Station>> aAssignment) {

        if (violatesCOconstraints(aAssignment)) return false;

        if (violatesADJConstraints(aAssignment)) return false;

        return true;
    }

    private boolean violatesADJConstraints(Map<Integer, Set<Station>> aAssignment) {
        for (Integer channel: aAssignment.keySet()) {
            for (Station station : aAssignment.get(channel)) {

                if (!stationHasADJconstraints(station)) {
                    continue;
                }

                Set<Station> stationsOnNextChannel = aAssignment.get(channel + 1);

                // If (channel + 1) is not contained in the assignment, then there can be no adjacency interference
                if (stationsOnNextChannel == null) {
                    continue;
                }

                for (Station neighbor : adjInterferingStationNeighborIndex.neighborsOf(station))
                    if (stationsOnNextChannel.contains(neighbor))
                        return true;
            }
        }

        return false;
    }

    private boolean violatesCOconstraints(Map<Integer, Set<Station>> aAssignment) {
        for (Integer channel : aAssignment.keySet()) {
            Set<Station> stationsOnChannel = aAssignment.get(channel);
            for (Station station : stationsOnChannel) {
                if (!stationHasCOconstraints(station)) {
                    continue;
                }
                for (Station neighbor : coInterferingStationNeighborIndex.neighborsOf(station))
                    if (stationsOnChannel.contains(neighbor))
                        return true;
            }
        }

        return false;
    }

    private boolean stationHasCOconstraints(Station station) {
        return this.coInterferingStationGraph.vertexSet().contains(station);
    }

    private boolean stationHasADJconstraints(Station station) {
        return adjInterferingStationGraph.vertexSet().contains(station);
    }

    @Override
    public String getHashCode() {
        return this.toString();
    }

}
