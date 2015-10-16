package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.math.util.FastMath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;

import com.google.common.collect.AbstractIterator;

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