package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies;

import java.util.Set;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;

/**
* Created by newmanne on 27/07/15.
*/
public interface IStationAddingStrategy {

    Iterable<Set<Station>> getStationsToPack(SimpleGraph<Station,DefaultEdge> graph, Set<Station> missingStations);

}
