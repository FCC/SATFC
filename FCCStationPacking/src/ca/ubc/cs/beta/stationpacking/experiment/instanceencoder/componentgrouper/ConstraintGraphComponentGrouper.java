package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.componentgrouper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;


public class ConstraintGraphComponentGrouper implements IComponentGrouper{

	private ArrayList<Set<Station>> fComponents;
	
	/**
	 * Construct a constraint graph component grouper that creates groups on construction based on full constraint graph connected components.
	 * @param aStations - the set of stations/vertices of the constraint graph.
	 * @param aConstraints - the set of pairwise constraints/edges of the constraint graph (constraints <i>must</i> be on stations from <b>aStations</b>).
	 * @throws Exception
	 */
	public ConstraintGraphComponentGrouper(Set<Station> aStations, Set<Constraint> aConstraints) throws Exception
	{
		SimpleGraph<Station,DefaultEdge> aConstraintGraph = new SimpleGraph<Station,DefaultEdge>(DefaultEdge.class);

		for(Station aStation : aStations)
		{
			aConstraintGraph.addVertex(aStation);
		}
		
		for(Constraint aConstraint : aConstraints)
		{
			Station aStation1 = aConstraint.getProtectedPair().getStation();
			Station aStation2 = aConstraint.getInterferingPair().getStation();
			
			if(!aConstraintGraph.containsVertex(aStation1))
			{
				throw new Exception("Station in constraints does not appear in given set of stations.");
			}
			if(!aConstraintGraph.containsVertex(aStation2))
			{
				throw new Exception("Station in constraints does not appear in given set of stations.");
			}
			
			aConstraintGraph.addEdge(aStation1, aStation2);
		}
		
		ConnectivityInspector<Station, DefaultEdge> aConnectivityInspector = new ConnectivityInspector<Station,DefaultEdge>(aConstraintGraph);
		
		fComponents = new ArrayList<Set<Station>>();
		for(Set<Station> aComponent : aConnectivityInspector.connectedSets())
		{
			
			if(aComponent.size()>0)
			{
				fComponents.add(aComponent);
			}
		}

	}
	
	@Override
	public HashSet<Set<Station>> group(Set<Station> aStations) {
		
		ArrayList<HashSet<Station>> aGroups = new ArrayList<HashSet<Station>>(fComponents.size());
		for(int i=0;i<fComponents.size();i++)
		{
			aGroups.add(i,new HashSet<Station>());
		}
		
		for(Station aStation : aStations)
		{
			for(int i=0;i<fComponents.size();i++)
			{
				if(fComponents.get(i).contains(aStation)){
					aGroups.get(i).add(aStation);
					break;
				}
			}
		}
		HashSet<Set<Station>> aStationSet = new HashSet<Set<Station>>();
		Iterator<HashSet<Station>> aGroupsIterator = aGroups.iterator();
		while(aGroupsIterator.hasNext()){
			if((aStations = aGroupsIterator.next()).isEmpty()){
				aGroupsIterator.remove();
			} else {
				aStationSet.add(aStations);
			}
		}
		return aStationSet;
		
	}
	
	

}
