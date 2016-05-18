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
package ca.ubc.cs.beta.stationpacking.consistency;

import static ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ConstraintKey.ADJp1;
import static ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ConstraintKey.CO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.TestConstraint;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.TestConstraintManager;

public class AC3EnforcerTest {

    final Station s1 = new Station(1);
    final Station s2 = new Station(2);
    final Station s3 = new Station(3);

    @Test
    public void testAC3() throws Exception {
        final StationPackingInstance instance = new StationPackingInstance(
                ImmutableMap.of(
                        s1, ImmutableSet.of(1),
                        s2, ImmutableSet.of(1, 2, 3),
                        s3, ImmutableSet.of(1, 2, 3)
                )
        );
        List<TestConstraint> constraints = new ArrayList<>();
        constraints.add(new TestConstraint(CO, 1, s1, ImmutableSet.of(s2, s3))); // CO,1,1,s1,s2,s3
        constraints.add(new TestConstraint(ADJp1, 1, s1, ImmutableSet.of(s2, s3))); // ADJ+1,1,2,s1,s2,s3
        IConstraintManager constraintManager = new TestConstraintManager(constraints);
        final AC3Enforcer ac3 = new AC3Enforcer(constraintManager);
        final AC3Output ac3Output = ac3.AC3(instance);
        assertFalse(ac3Output.isNoSolution());
        assertEquals(4, ac3Output.getNumReducedChannels());
        final Map<Station, Set<Integer>> reducedDomains = ac3Output.getReducedDomains();
        assertEquals(ImmutableSet.of(3), reducedDomains.get(s2));
        assertEquals(ImmutableSet.of(3), reducedDomains.get(s3));
    }

    @Test
    public void testNoSolution() throws Exception {
        final StationPackingInstance instance = new StationPackingInstance(
                ImmutableMap.of(
                        s1, ImmutableSet.of(1),
                        s2, ImmutableSet.of(1, 2)
                )
        );
        List<TestConstraint> constraints = new ArrayList<>();
        constraints.add(new TestConstraint(CO, 1, s1, ImmutableSet.of(s2)));
        constraints.add(new TestConstraint(ADJp1, 1, s1, ImmutableSet.of(s2)));
        IConstraintManager constraintManager = new TestConstraintManager(constraints);
        final AC3Enforcer ac3 = new AC3Enforcer(constraintManager);
        final AC3Output ac3Output = ac3.AC3(instance);
        assertTrue(ac3Output.isNoSolution());
    }

}