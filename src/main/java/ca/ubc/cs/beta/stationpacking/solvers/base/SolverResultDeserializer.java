package ca.ubc.cs.beta.stationpacking.solvers.base;

import ca.ubc.cs.beta.stationpacking.base.Station;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.Data;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors.toImmutableMap;

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