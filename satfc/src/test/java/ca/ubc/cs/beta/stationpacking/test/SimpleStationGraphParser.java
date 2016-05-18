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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;

/**
 * <p>
 * Reads a text file that represents a simple graph, where each line in the file represents an edge
 *  between two nodes, and each node is designated by an integer. Nodes are separated by a space.
 *  Nodes are interpreted to represent stations; each numerical node is used to construct a Station,
 *  where Stations are defined completely by a single integer. Lines starting with '//' are interpreted
 *  as comments. Blank lines are also ignored.
 * </p>
 *
 * <p>
 *     Note that in certain situations, one station needs to be designated as a starting point in a search.
 *     For such cases, node number 0 is reserved to represent this node.
 * </p>
 *
 * <p> Example contents of a simple graph file:
 *  <pre>
 *  // The first line of this graph file is commented out.
 *  1 0
 *  3
 *
 *  1 2
 * </pre></p>
 *
 * <p>
 *     This example graph file represents a graph consisting of stations 0, 1, 2, and 3, with edges between
 *     stations 1 and 0, and between stations 1 and 2. Station 3 is not connected to any other station.
 * </p>
 *
 * @author pcernek
 */
public class SimpleStationGraphParser implements IStationGraphFileParser {

    private final SimpleGraph<Station, DefaultEdge> stationGraph;

    /**
     * Returns a SimpleGraphBuilder that reads from a file that specifies a graph of the interference
     *  constraints between stations. The SimpleGraphBuilder tries to build a graph from the specified file
     *  immediately upon construction.
     *
     * @param graphFilePath - path to the graph file, relative to the project's resources directory.
     * @throws IOException - if an error occurs when trying to read from the graph file, either due to
     * a bad file path or due to a graph file whose contents are malformed.
     */
    public SimpleStationGraphParser(Path graphFilePath) throws IOException {
        this.stationGraph = parseGraphFile(graphFilePath);
    }

    /**
     * Parse the given graph file and produce a simple graph.
     * @param graphPath - the file containing a representation of a simple undirected graph.
     * @return - the simple graph.
     */
    private SimpleGraph<Station, DefaultEdge> parseGraphFile(Path graphPath) throws IOException {
        SimpleGraph<Station, DefaultEdge> stationGraph = new SimpleGraph<>(DefaultEdge.class);

        for (String line: Files.readAllLines(graphPath)) {

            // Filter out comment lines.
            if (line.startsWith("//") || line.isEmpty()) {
                continue;
            }

            // Make stations from current line.
            List<Station> currentStations = Stream.of(line.split(" "))  // stations on the same line are space-separated
                    .map(Integer::new)
                    .map(Station::new)
                    .collect(Collectors.toList());

            // Make sure line contains no more than two stations.
            if (currentStations.size() <= 2) {

                currentStations.forEach(stationGraph::addVertex);

                if (currentStations.size() == 2)
                    stationGraph.addEdge(currentStations.get(0), currentStations.get(1));
            }
            else {
                throw new IOException("Graph file is malformed, each line should contain no more than two integers");
            }

        }

        return stationGraph;
    }

    @Override
    public SimpleGraph<Station, DefaultEdge> getStationGraph() {
        return this.stationGraph;
    }
}
