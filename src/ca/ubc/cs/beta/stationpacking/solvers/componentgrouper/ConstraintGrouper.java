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
		
		SimpleGraph<Station,DefaultEdge> aConstraintGraph = getConstraintGraph(aInstance, aConstraintManager);
		
		HashSet<Set<Station>> aGroups = new HashSet<Set<Station>>();
		
		ConnectivityInspector<Station, DefaultEdge> aConnectivityInspector = new ConnectivityInspector<Station,DefaultEdge>(aConstraintGraph);
	
		for(Set<Station> aConnectedComponent : aConnectivityInspector.connectedSets())
		{
			aGroups.add(aConnectedComponent);
		}
		
		return aGroups;
	}
	
	/**
	 * @param aInstance - the instances that form the constraint graph's vertex set.
	 * @param aConstraintManager - the constraint manager to use to form edges of the constraint graph.
	 * @return the constraint graph.
	 */
	public static SimpleGraph<Station,DefaultEdge> getConstraintGraph(StationPackingInstance aInstance, IConstraintManager aConstraintManager)
	{
		Set<Station> aStations = aInstance.getStations();
		Set<Integer> aInstanceDomain = aInstance.getChannels();
		SimpleGraph<Station,DefaultEdge> aConstraintGraph = new SimpleGraph<Station,DefaultEdge>(DefaultEdge.class);
		for(Station aStation : aStations){
			aConstraintGraph.addVertex(aStation);
		}
		
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
		
		return aConstraintGraph;
	}
}

