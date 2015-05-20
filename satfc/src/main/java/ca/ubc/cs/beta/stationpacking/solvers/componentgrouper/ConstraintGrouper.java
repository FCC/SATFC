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
package ca.ubc.cs.beta.stationpacking.solvers.componentgrouper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;

/**
 * Groups the stations in a station packing instance based on connected components in interference constraint graph.
 * @author afrechet
 */
public class ConstraintGrouper implements IComponentGrouper{
	
	//NA - just assume that at least two feasible channels are adjacent (so that ADJ constraints are relevant).
	@Override
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
		final Set<Station> aStations = aInstance.getStations();
		final Map<Station,Set<Integer>> aDomains = aInstance.getDomains();
		
		final SimpleGraph<Station,DefaultEdge> aConstraintGraph = new SimpleGraph<Station,DefaultEdge>(DefaultEdge.class);
		
		for(Station aStation : aStations){
			aConstraintGraph.addVertex(aStation);
		}
		
		for(Station aStation1 : aStations){
			for(Integer channel : aDomains.get(aStation1))
			{
				for(Station aStation2 : aConstraintManager.getCOInterferingStations(aStation1, channel)){
					if(aStations.contains(aStation2) && aDomains.get(aStation2).contains(channel))
					{
						aConstraintGraph.addEdge(aStation1, aStation2);
					}
				}
				
				int channelp1 = channel+1;
				for(Station aStation2 : aConstraintManager.getADJplusInterferingStations(aStation1,channel)){
					if(aStations.contains(aStation2) && aDomains.get(aStation2).contains(channelp1))
					{
						aConstraintGraph.addEdge(aStation1, aStation2);
					}
				}
			}
		}
		
		return aConstraintGraph;
	}

}

