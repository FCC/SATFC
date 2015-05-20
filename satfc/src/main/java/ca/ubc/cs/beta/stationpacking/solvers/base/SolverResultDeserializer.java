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
package ca.ubc.cs.beta.stationpacking.solvers.base;

import static ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors.toImmutableMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import ca.ubc.cs.beta.stationpacking.base.Station;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Created by newmanne on 01/12/14.
 */
public class SolverResultDeserializer extends JsonDeserializer<SolverResult> {

    @Override
    public SolverResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        final SolverResultJson solverResultJson = jsonParser.readValueAs(SolverResultJson.class);
        // transform back into stations
        Map<Integer,Set<Station>> assignment = solverResultJson.getAssignment().entrySet()
                .stream()
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(Station::new).collect(Collectors.toSet())
                ));
        return new SolverResult(solverResultJson.getResult(), solverResultJson.getRuntime(), assignment);
    }

    @Data
    public static class SolverResultJson {
        private Map<Integer, List<Integer>> assignment;
        private double runtime;
        private SATResult result;
    }

}