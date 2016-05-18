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

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.math.util.FastMath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.google.common.collect.AbstractIterator;

import ca.ubc.cs.beta.stationpacking.base.Station;

/**
* Created by newmanne on 27/07/15.
* Iterate through an IStationAddingStrategy, starting with quick cutoffs and then looping back with larger ones
*/
public class IterativeDeepeningConfigurationStrategy implements IStationPackingConfigurationStrategy {

    // Strategy for adding the next set of stations to pack
    private final IStationAddingStrategy stationAddingStrategy;
    // Whether or not to cycle over the IStationAddingStrategy (or stop once it ends)
    private final boolean loop;
    private final double baseCutoff;
    private final double scalingFactor;

    // Create a non-iterative version
    public IterativeDeepeningConfigurationStrategy(IStationAddingStrategy stationAddingStrategy, double baseCutoff) {
        this(stationAddingStrategy, baseCutoff, 0);
    }

    public IterativeDeepeningConfigurationStrategy(IStationAddingStrategy stationAddingStrategy, double baseCutoff, double scalingFactor) {
        this.stationAddingStrategy = stationAddingStrategy;
        this.baseCutoff = baseCutoff;
        this.scalingFactor = scalingFactor;
        this.loop = scalingFactor != 0;
    }

    @Override
    public Iterable<StationPackingConfiguration> getConfigurations(SimpleGraph<Station, DefaultEdge> graph, Set<Station> missingStations) {
        final Iterable<Set<Station>> stationsToPackIterable = stationAddingStrategy.getStationsToPack(graph, missingStations);
        return () -> new AbstractIterator<StationPackingConfiguration>() {

            Iterator<Set<Station>> stationsToPackIterator = stationsToPackIterable.iterator();
            int iteration = 0;

            @Override
            protected StationPackingConfiguration computeNext() {
                if (!stationsToPackIterator.hasNext()) {
                    if (!loop) {
                        return endOfData();
                    } else {
                        stationsToPackIterator = stationsToPackIterable.iterator();
                        iteration++;
                        if (!stationsToPackIterator.hasNext()) { // 0 element iterator
                            return endOfData();
                        }
                    }
                }
                final Set<Station> toPack = stationsToPackIterator.next();
                final double cutoff = baseCutoff + FastMath.pow(scalingFactor, iteration);
                return new StationPackingConfiguration(cutoff, toPack);
            }
        };
    }

}