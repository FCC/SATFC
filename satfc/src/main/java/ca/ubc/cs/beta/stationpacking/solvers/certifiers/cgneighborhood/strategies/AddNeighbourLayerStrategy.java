package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Sets;

/**
* Created by newmanne on 27/07/15.
*/
@Slf4j
public class AddNeighbourLayerStrategy implements IStationAddingStrategy {

    private final int maxLayers;

    public AddNeighbourLayerStrategy(int maxLayers) {
        Preconditions.checkArgument(maxLayers > 0, "max layers must be > 0");
        this.maxLayers = maxLayers;
    }

    public AddNeighbourLayerStrategy() {
        this(Integer.MAX_VALUE);
    }

    @Override
    public Iterable<Set<Station>> getStationsToPack(SimpleGraph<Station, DefaultEdge> graph, Set<Station> missingStations) {
        Preconditions.checkArgument(missingStations.size() > 0, "Cannot provide empty missing stations");
        final NeighborIndex<Station, DefaultEdge> neighbourIndex = new NeighborIndex<>(graph);
        return () -> new NeighbourLayerIterator(missingStations, neighbourIndex);
    }

    private class NeighbourLayerIterator extends AbstractIterator<Set<Station>> {

        int currentLayer = 1;
        Set<Station> prev;
        final NeighborIndex<Station, DefaultEdge> neighbourIndex;

        NeighbourLayerIterator(Set<Station> missingStations, NeighborIndex<Station, DefaultEdge> neighbourIndex) {
            this.prev = new HashSet<>(missingStations);
            this.currentLayer = 1;
            this.neighbourIndex = neighbourIndex;
        }

        @Override
        protected Set<Station> computeNext() {
            if (currentLayer > maxLayers) {
                return endOfData();
            }
            Set<Station> newToPack = Sets.union(prev, getNeighbours(prev));
            // If nothing new was added, we are done. Make sure we iterate at least once though, because the newly added station could be an island
            if (prev.equals(newToPack) && currentLayer > 1) {
                return endOfData();
            }
            currentLayer++;
            prev = newToPack;
            return newToPack;
        }

        private Set<Station> getNeighbours(Set<Station> stations) {
            return stations.stream().map(neighbourIndex::neighborsOf).flatMap(Collection::stream).collect(Collectors.toSet());
        }
    }

}
