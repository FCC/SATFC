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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Sets;

import ca.ubc.cs.beta.stationpacking.base.Station;
import lombok.extern.slf4j.Slf4j;

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
