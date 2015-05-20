package ca.ubc.cs.beta.stationpacking.test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

/**
 * <p>
 *     Builds a graph that represents a set of stations, where an edge between two stations
 *     indicates an interference constraint between them.
 * </p>
 * @author pcernek
 */
public interface IStationGraphFileParser {

    /**
     * Provide a graph of stations that represents the interference constraints between them.
     * @return - a graph of stations.
     */
    SimpleGraph<Station, DefaultEdge> getStationGraph();

}
