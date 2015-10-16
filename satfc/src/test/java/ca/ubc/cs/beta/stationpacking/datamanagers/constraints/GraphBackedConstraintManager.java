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
package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.util.Collections;
import java.util.Set;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;

/**
 * A mock ConstraintManager useful for running tests where the focus is on the shape of interference constraint graphs
 *  between stations.
 * @author pcernek
 */
public class GraphBackedConstraintManager extends AConstraintManager {

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

    public GraphBackedConstraintManager(SimpleGraph<Station, DefaultEdge> coInterferingStationGraph) {
        this(coInterferingStationGraph, new SimpleGraph<Station, DefaultEdge>(DefaultEdge.class));
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
    public Set<Station> getADJplusOneInterferingStations(Station aStation, int aChannel) {
    	if (this.adjInterferingStationGraph.containsVertex(aStation)) {
    		return this.adjInterferingStationNeighborIndex.neighborsOf(aStation);
    	}
    	return Collections.emptySet();
    }
    
	@Override
	public Set<Station> getADJplusTwoInterferingStations(Station aStation, int aChannel) {
		return Collections.emptySet();
	}



    @Override
    public String getConstraintHash() {
        throw new UnsupportedOperationException();
    }

}