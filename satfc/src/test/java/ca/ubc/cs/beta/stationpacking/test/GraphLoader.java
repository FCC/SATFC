/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.google.common.io.Resources;

import ca.ubc.cs.beta.stationpacking.base.Station;

/**
 * A class that lazily loads graphs to make them accessible to any other class.
 * @author pcernek
 *
 */
public class GraphLoader {
    
	private SimpleGraph <Station,DefaultEdge> noNeighbors;
    private SimpleGraph <Station,DefaultEdge> bigConnectedGraph;
    private SimpleGraph <Station,DefaultEdge> clique;
    private SimpleGraph <Station,DefaultEdge> hubAndSpoke;
    private SimpleGraph <Station,DefaultEdge> longChainOfNeighbors;
    private SimpleGraph <Station,DefaultEdge> bipartiteGraph;
    private SimpleGraph <Station,DefaultEdge> disconnectedComponents;
    
    private SimpleGraph<Station, DefaultEdge> emptyGraph = new SimpleGraph<>(DefaultEdge.class);
    
    public SimpleGraph<Station, DefaultEdge> getNoNeighbors() throws IOException, URISyntaxException {
    	return loadSimpleGraph(noNeighbors, "graphs/noNeighbors.txt");
    }

    public SimpleGraph<Station, DefaultEdge> getBigConnectedGraph() throws IOException, URISyntaxException {
    	return loadSimpleGraph(bigConnectedGraph, "graphs/bigConnectedGraph.txt");
    }
    
    public SimpleGraph<Station, DefaultEdge> getClique() throws IOException, URISyntaxException {
    	return loadSimpleGraph(clique, "graphs/clique.txt");
    }
    
    public SimpleGraph<Station, DefaultEdge> getHubAndSpoke() throws IOException, URISyntaxException {
    	return loadSimpleGraph(hubAndSpoke, "graphs/hubAndSpoke.txt");
    }
    
    public SimpleGraph<Station, DefaultEdge> getLongChainOfNeighbors() throws IOException, URISyntaxException {
    	return loadSimpleGraph(longChainOfNeighbors, "graphs/longChainOfNeighbors.txt");
    }
    
    public SimpleGraph<Station, DefaultEdge> getBipartiteGraph() throws IOException, URISyntaxException {
    	return loadSimpleGraph(bipartiteGraph, "graphs/bipartiteGraph.txt");
    }

    public SimpleGraph<Station, DefaultEdge> getDisconnectedComponents() throws IOException, URISyntaxException {
    	return loadSimpleGraph(disconnectedComponents, "graphs/disconnectedComponents.txt");
    }
    
    public SimpleGraph<Station, DefaultEdge> getEmptyGraph() {
    	return emptyGraph;
    }    
    
    private static SimpleGraph<Station, DefaultEdge> loadSimpleGraph(SimpleGraph<Station, DefaultEdge> graph, String relativePath)
    		throws IOException, URISyntaxException 
    {
    	if(graph == null)
    		graph = new SimpleStationGraphParser(GraphLoader.resourceLocationToPath(relativePath)).getStationGraph();
    	return graph;
    }
    
    /**
     * Loads all the graphs accessible to this GraphLoader. Convenient for ensuring all overhead
     *  incurred by file I/O happens at one time.
     * 
     * If any graph is ever added to this class, it would be preferable to add a call to a getter for it to this method.
     * @throws IOException
     * @throws URISyntaxException
     */
	public void loadAllGraphs() throws IOException, URISyntaxException
	{
	    getNoNeighbors();
		getBigConnectedGraph();
	    getClique();
	    getHubAndSpoke();
	    getLongChainOfNeighbors();
	    getBipartiteGraph();
	    getDisconnectedComponents();
	}

	/**
	 * Convert a string, specifying a path to a resource file, into a java Path object, suited to file I/O.
	 *  The path is assumed to be relative to the "resources" folder.
	 *  highest directory
	 * @param resourceLocationString - the relative path to a resource contained in a resource folder.
	 * @return - a Path object corresponding to the location of that resource file.
	 * @throws URISyntaxException - if the string passed as an argument cannot be parsed as a valid path.
	 */
	private static Path resourceLocationToPath(String resourceLocationString) throws URISyntaxException {
	    return Paths.get(Resources.getResource(resourceLocationString).toURI());
	}
}