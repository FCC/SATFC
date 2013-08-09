package ca.ubc.cs.beta.stationpacking.solvers.componentgrouper;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;

public class ConstraintGrouper implements IComponentGrouper{
	
	
	//NA - just assume that at least two feasible channels are adjacent (so that ADJ constraints are relevant).
	public Set<Set<Station>> group(StationPackingInstance aInstance, IConstraintManager aConstraintManager){
		Set<Station> aStations = aInstance.getStations();
		Set<Integer> aInstanceDomain = aInstance.getChannels();
		SimpleGraph<Station,DefaultEdge> aConstraintGraph = new SimpleGraph<Station,DefaultEdge>(DefaultEdge.class);
		for(Station aStation : aStations){
			aConstraintGraph.addVertex(aStation);
		}
		HashSet<Set<Station>> aGroups = new HashSet<Set<Station>>();
		try{
			for(Station aStation1 : aStations){
				for(Station aStation2 : aConstraintManager.getCOInterferingStations(aStation1, aInstanceDomain)){
					if(aConstraintGraph.containsVertex(aStation2)){
						aConstraintGraph.addEdge(aStation1, aStation2);
					}
				}
				for(Station aStation2 : aConstraintManager.getADJplusInterferingStations(aStation1,aInstanceDomain)){
					if(aConstraintGraph.containsVertex(aStation2)){
						aConstraintGraph.addEdge(aStation1, aStation2);
					}
				}
			}
			ConnectivityInspector<Station, DefaultEdge> aConnectivityInspector = new ConnectivityInspector<Station,DefaultEdge>(aConstraintGraph);
		
			for(Set<Station> aConnectedComponent : aConnectivityInspector.connectedSets())
			{
				//Early optimization - we wouldn't need to add groups of size 1.
//				if(aConnectedComponent.size()>1)
//				{
//					aGroups.add(aConnectedComponent);	
//				}
				aGroups.add(aConnectedComponent);
			}
		} catch(Exception e){
			aGroups.add(aStations); //if there's an error, return a single component (should we instead pass the exception along?)
		}
		return aGroups;

	}
}

