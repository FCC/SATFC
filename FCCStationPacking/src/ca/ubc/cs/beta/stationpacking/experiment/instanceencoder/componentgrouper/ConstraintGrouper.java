package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.componentgrouper;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.IConstraintManager;

public class ConstraintGrouper implements IComponentGrouper{
	
	private static IConstraintManager fConstraintManager;
	
	public ConstraintGrouper(IConstraintManager aConstraintManager){
		fConstraintManager = aConstraintManager;
	}
	
	//NA - just assume that at least two feasible channels are adjacent (so that ADJ constraints are relevant).
	public Set<Set<Station>> group(Set<Station> aStations){
		SimpleGraph<Station,DefaultEdge> aConstraintGraph = new SimpleGraph<Station,DefaultEdge>(DefaultEdge.class);
		for(Station aStation : aStations){
			aConstraintGraph.addVertex(aStation);
		}
		for(Station aStation1 : aStations){
			for(Station aStation2 : fConstraintManager.getCOInterferingStations(aStation1)){
				if(aConstraintGraph.containsVertex(aStation2)){
					aConstraintGraph.addEdge(aStation1, aStation2);
				}
			}
			for(Station aStation2 : fConstraintManager.getADJplusInterferingStations(aStation1)){
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
