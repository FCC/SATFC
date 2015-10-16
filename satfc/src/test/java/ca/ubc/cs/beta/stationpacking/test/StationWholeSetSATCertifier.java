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
package ca.ubc.cs.beta.stationpacking.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.IStationSubsetCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Returns SAT when (and only when) all the stations in the instance of a station packing problem are contained in the
 *  "toPackStations" parameter (i.e. when the group of neighbors around a starting point has extended to include every
 *  station in the instance).
 * @author pcernek
 */
public class StationWholeSetSATCertifier implements IStationSubsetCertifier {

    private int numberOfTimesCalled;
    
    private final Map <		ConnectivityInspector<Station, DefaultEdge> ,
    				     	SimpleGraph<Station,DefaultEdge>  				> inspectorGraphMap;
    private final Set<Station> startingStations;

    public StationWholeSetSATCertifier(List<SimpleGraph<Station, DefaultEdge>> graphs, Set<Station> startingStations) {
        this.numberOfTimesCalled = 0;
        this.inspectorGraphMap = new HashMap<>();
        for (SimpleGraph<Station, DefaultEdge> graph : graphs) {
        	this.inspectorGraphMap.put(new ConnectivityInspector<>(graph), graph);
        }
        this.startingStations = startingStations;
    }

    @Override
    public SolverResult certify(StationPackingInstance aInstance, Set<Station> aToPackStations, ITerminationCriterion aTerminationCriterion, long aSeed) {
        numberOfTimesCalled ++;
        boolean allNeighborsIncluded = true;
        for(Station station: startingStations) {
        	for (ConnectivityInspector<Station, DefaultEdge> inspector: inspectorGraphMap.keySet()) {
        		if (inspectorGraphMap.get(inspector).containsVertex(station)) {
        			allNeighborsIncluded &= aToPackStations.containsAll(inspector.connectedSetOf(station));
        		}
        	}
        }
        if (allNeighborsIncluded) {
        	return new SolverResult(SATResult.SAT, 0, new HashMap<>(), SolvedBy.UNKNOWN);
        }
        
        // We return TIMEOUT rather than UNSAT in keeping with the behavior of {@link: StationSubsetSATCertifier}
        return SolverResult.createTimeoutResult(0);
    }

    public int getNumberOfTimesCalled() {
        return numberOfTimesCalled;
    }

}
