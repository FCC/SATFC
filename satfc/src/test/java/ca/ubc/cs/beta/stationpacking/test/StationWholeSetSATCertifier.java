package ca.ubc.cs.beta.stationpacking.test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.IStationSubsetCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

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
        if (allNeighborsIncluded)
        	return new SolverResult(SATResult.SAT, 0, new HashMap<>());
        
        // We return TIMEOUT rather than UNSAT in keeping with the behavior of {@link: StationSubsetSATCertifier}
        return new SolverResult(SATResult.TIMEOUT, 0);
    }

    @Override
    public void interrupt() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void notifyShutdown() {
        throw new UnsupportedOperationException();
    }

    public int getNumberOfTimesCalled() {
        return numberOfTimesCalled;
    }
}
