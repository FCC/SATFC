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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
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
public class AddRandomNeighboursStrategy implements IStationAddingStrategy {

    private final int numNeigbhoursToAdd;
    private final Random random;

    public AddRandomNeighboursStrategy(int numNeigbhoursToAdd, long seed) {
        this.numNeigbhoursToAdd = numNeigbhoursToAdd;
        this.random = new Random(seed);
    }

    public AddRandomNeighboursStrategy(int numNeigbhoursToAdd) {
        this(numNeigbhoursToAdd, new Random().nextInt());
    }

    @Override
    public Iterable<Set<Station>> getStationsToPack(SimpleGraph<Station, DefaultEdge> graph, Set<Station> missingStations) {
        Preconditions.checkArgument(missingStations.size() > 0, "Cannot provide empty missing stations");
        log.debug("Building constraint graph.");
        final NeighborIndex<Station, DefaultEdge> neighbourIndex = new NeighborIndex<>(graph);
        return () -> new RandomNeighbourIterator(missingStations, neighbourIndex);
    }

    private class RandomNeighbourIterator extends AbstractIterator<Set<Station>> {

        private final NeighborIndex<Station, DefaultEdge> neighbourIndex;
        final Set<Station> prev;
        final Set<Station> remainingToAddFromPreviousLayer;

        public RandomNeighbourIterator(Set<Station> missingStations, NeighborIndex<Station, DefaultEdge> neighbourIndex) {
            this.remainingToAddFromPreviousLayer = new HashSet<>();
            this.prev = new HashSet<>(missingStations);
            this.neighbourIndex = neighbourIndex;
        }


        @Override
        protected Set<Station> computeNext() {
            // Start by seeing if there are "leftovers" that you can use
            final Set<Station> toAdd = uniformlyRandomPick(remainingToAddFromPreviousLayer, numNeigbhoursToAdd);
            remainingToAddFromPreviousLayer.removeAll(toAdd);
            prev.addAll(toAdd);
            if (toAdd.size() == numNeigbhoursToAdd) { // we found enough in the leftovers
                return prev;
            } else if (toAdd.size() == 0 && getNoneAddedNeighbours(prev).isEmpty()) { // we're done
                return endOfData();
            } else {
                int remainingToAdd = numNeigbhoursToAdd - toAdd.size();
                while (remainingToAdd > 0) {
                    // Expand a new layer, if necessary
                    if (remainingToAddFromPreviousLayer.isEmpty()) {
                        remainingToAddFromPreviousLayer.addAll(getNoneAddedNeighbours(prev));
                        if (remainingToAddFromPreviousLayer.isEmpty()) { // can't expand anymore
                            break;
                        }
                    }
                    final Set<Station> newToAdd = uniformlyRandomPick(remainingToAddFromPreviousLayer, remainingToAdd);
                    remainingToAddFromPreviousLayer.removeAll(newToAdd);
                    remainingToAdd -= newToAdd.size();
                    prev.addAll(newToAdd);
                }
                return prev;
            }
        }

        private Set<Station> uniformlyRandomPick(Set<Station> choices, int num) {
            final List<Station> choiceList = new ArrayList<>(choices);
            Collections.shuffle(choiceList, random);
            return new HashSet<>(choiceList.subList(0, Math.min(num, choiceList.size())));
        }

        private Set<Station> getNoneAddedNeighbours(Set<Station> stations) {
            return Sets.difference(stations.stream().map(neighbourIndex::neighborsOf).flatMap(Collection::stream).collect(Collectors.toSet()), stations);
        }

    }

}
