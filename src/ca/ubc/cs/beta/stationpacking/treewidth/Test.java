package ca.ubc.cs.beta.stationpacking.treewidth;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nl.uu.cs.treewidth.algorithm.GreedyFillIn;
import nl.uu.cs.treewidth.algorithm.MaximumMinimumDegreePlusLeastC;
import nl.uu.cs.treewidth.input.DgfReader;
import nl.uu.cs.treewidth.input.GraphInput;
import nl.uu.cs.treewidth.input.GraphInput.InputData;
import nl.uu.cs.treewidth.input.InputException;
import nl.uu.cs.treewidth.ngraph.NGraph;

import org.apache.commons.io.FileUtils;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.Subgraph;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		loadGraph();
	}
	
	private static void getTreeWidth()
	{
		String graphFile = "ConstraintGraph.dgf";
		
		System.out.println("Inputting graph from "+graphFile+" ...");
		GraphInput input = new DgfReader(graphFile);
		NGraph<InputData> g;
		try {
			g = input.get();
		} catch( InputException e ) {
			throw new IllegalArgumentException("Could not input graph.");
		}
		
		System.out.println("Calculating tree width lower bound ...");
		
		MaximumMinimumDegreePlusLeastC<InputData> lbAlgo = new MaximumMinimumDegreePlusLeastC<InputData>();
		lbAlgo.setInput( g );
		lbAlgo.run();
		int lowerbound = lbAlgo.getLowerBound();
		
		System.out.println("Tree width lowerbound = "+lowerbound);
		
		System.out.println("Calculating tree width upper bound ...");

		GreedyFillIn<InputData> ubAlgo = new GreedyFillIn<InputData>();
		ubAlgo.setInput( g );
		ubAlgo.run();
		int upperbound = ubAlgo.getUpperBound();
		
		System.out.println("Tree width upperbound = "+upperbound);
		
	}
	
	
	private static void loadGraph() throws IOException
	{
		System.out.println("Loading in data...");
		
		//String stationConfig = "/ubc/cs/home/a/afrechet/arrow-space/experiments/fcc-station-packing/webinar/Constraints02052014";
		String stationConfig = "/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingDeliverable/021814SC3M";
		
		String stationDomainsFile = stationConfig+File.separator+"domains.csv";
		IStationManager stationsManager = new DomainStationManager(stationDomainsFile);
		
		String constraintsFile = stationConfig+File.separator+"interferences.csv";;
		IConstraintManager constraintManager = new ChannelSpecificConstraintManager(stationsManager, constraintsFile);
		
		Set<Station> stations = stationsManager.getStations();
		Set<Integer> channels = new HashSet<Integer>();
		channels.addAll(StationPackingUtils.HVHF_CHANNELS);
		channels.addAll(StationPackingUtils.LVHF_CHANNELS);
		channels.addAll(StationPackingUtils.UHF_CHANNELS);
		
		System.out.println("Creating instance...");
		
		StationPackingInstance instance = StationPackingInstance.constructUniformDomainInstance(stations, channels, new HashMap<Station,Integer>());
		
		System.out.println("Getting constraint graph...");
		
		SimpleGraph<Station,DefaultEdge> constraintGraph = ConstraintGrouper.getConstraintGraph(instance, constraintManager);
		
		ConnectivityInspector<Station, DefaultEdge> connectivityInspector = new ConnectivityInspector<Station,DefaultEdge>(constraintGraph);
		int c =0;
		
		List<Set<Station>> components = new ArrayList<Set<Station>>(connectivityInspector.connectedSets());
		Collections.sort(components, new Comparator<Set<Station>>(){
			@Override
			public int compare(Set<Station> o1, Set<Station> o2) {
				return Integer.compare(o2.size(), o1.size());
			}});
		
		for(Set<Station> connectedComponent : components)
		{
			//Calculating properties of this subgraph.
			final Subgraph<Station,DefaultEdge,SimpleGraph<Station,DefaultEdge>> cG = new Subgraph<Station,DefaultEdge,SimpleGraph<Station,DefaultEdge>>(constraintGraph,connectedComponent);
			
			System.out.println("Analyzing properties of component of size "+cG.vertexSet().size());
			
			Collection<String> properties = new LinkedList<String>();
			
			properties.add("Component Graph "+c);
			properties.add("Num Vertex,"+cG.vertexSet().size());
			properties.add("Num Edges,"+cG.edgeSet().size());
			
			List<Station> vertices = new ArrayList<Station>(cG.vertexSet());
			Collections.sort(vertices, new Comparator<Station>(){

				@Override
				public int compare(Station o1, Station o2) {
					return Integer.compare(cG.edgesOf(o2).size(), cG.edgesOf(o1).size());
				}});
			
			properties.add("station-vertex,degree");
			
			for(Station v : vertices)
			{
				properties.add(v+","+cG.edgesOf(v).size());
			}
			
			FileUtils.writeLines(new File("ConstraintGraph_Component"+c+"_info.csv"), properties);
			
			
//			StringBuilder graphString = new StringBuilder();
//			System.out.println("Converting largest connected component of constraint graph to dgf...");
//			
//			Map<Station,Integer> stationIDs = new HashMap<Station,Integer>();
//			int i = 1;
//			for(Station s : connectedComponent)
//			{
//				stationIDs.put(s, i++);
//			}
//			
//			
//			for(Station s : connectedComponent)
//			{
//				for(Station t : connectedComponent)
//				{
//					if(constraintGraph.containsEdge(s, t))
//					{
//						graphString.append("e "+stationIDs.get(s)+" "+stationIDs.get(t)+"\n");
//					}
//				}
//			}
//			
//			graphString.insert(0, "p edge "+Integer.valueOf(constraintGraph.vertexSet().size())+" "+Integer.valueOf(constraintGraph.edgeSet().size())+"\n");
//			graphString.insert(0, "c Constraint file "+constraintsFile+".\n");
//			graphString.insert(0, "c Stations file "+stationDomainsFile+".\n");
//			graphString.insert(0, "c Station Packing Problem.\n");
//			
//			String cgFile = "ConstraintGraph.dgf";
//			System.out.println("Saving constraint graph to file ");
//			FileUtils.write(new File(cgFile), graphString.toString());
			
			c++;
		}
		
		
		
		
		
		
		
		
		
		
		
	}

}
