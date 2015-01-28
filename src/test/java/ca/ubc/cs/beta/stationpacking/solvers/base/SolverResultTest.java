package ca.ubc.cs.beta.stationpacking.solvers.base;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class SolverResultTest {

    @Before
    public void setUp() {
        Set<Station> s = Sets.newHashSet();
        s.add(new Station(3));
        result = new SolverResult(SATResult.SAT, 37.4, ImmutableMap.of(3, s));
    }

    private SolverResult result;

    @Test
    public void testSerialization() throws Exception {
        Set<Station> s = Sets.newHashSet();
        s.add(new Station(3));
        assertEquals("{\"assignment\":{\"3\":[\"3\"]},\"runtime\":37.4,\"result\":\"SAT\"}", JSONUtils.toString(result));
    }

    @Test
    public void testSerializationDeserializationAreInverses() throws Exception {
        assertEquals(result, JSONUtils.toObject(JSONUtils.toString(result), SolverResult.class));
    }
}