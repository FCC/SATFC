package ca.ubc.cs.beta.stationpacking.solver.TAESolver.componentgrouper;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;

public class ConstraintGrouper implements IComponentGrouper{
	
	
	//NA - just assume that at least two feasible channels are adjacent (so that ADJ constraints are relevant).
	public Set<Set<Station>> group(Set<Station> aStations, IConstraintManager aConstraintManager){
		SimpleGraph<Station,DefaultEdge> aConstraintGraph = new SimpleGraph<Station,DefaultEdge>(DefaultEdge.class);
		for(Station aStation : aStations){
			aConstraintGraph.addVertex(aStation);
		}
		for(Station aStation1 : aStations){
			for(Station aStation2 : aConstraintManager.getCOInterferingStations(aStation1)){
				if(aConstraintGraph.containsVertex(aStation2)){
					aConstraintGraph.addEdge(aStation1, aStation2);
				}
			}
			for(Station aStation2 : aConstraintManager.getADJplusInterferingStations(aStation1)){
				if(aConstraintGraph.containsVertex(aStation2)){
					aConstraintGraph.addEdge(aStation1, aStation2);
				}
			}
		}
		ConnectivityInspector<Station, DefaultEdge> aConnectivityInspector = new ConnectivityInspector<Station,DefaultEdge>(aConstraintGraph);
		
		HashSet<Set<Station>> aGroups = new HashSet<Set<Station>>();
		for(Set<Station> aConnectedComponent : aConnectivityInspector.connectedSets())
		{
			if(aConnectedComponent.size()>1)
			{
				aGroups.add(aConnectedComponent);
			}
		}
		return aGroups;
	}

}
