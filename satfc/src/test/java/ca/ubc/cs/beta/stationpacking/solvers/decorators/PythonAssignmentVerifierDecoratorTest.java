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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import ca.ubc.cs.beta.stationpacking.StationPackingTestUtils;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories.PythonInterpreterContainer;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by emily404 on 9/3/15.
 */
@Slf4j
public class PythonAssignmentVerifierDecoratorTest {

    final static ISolver solver = mock(ISolver.class);
    static PythonAssignmentVerifierDecorator pythonAssignmentVerifierDecorator;
    static PythonAssignmentVerifierDecorator nonCompactPythonAssignmentVerifierDecorator;
    final StationPackingInstance instance = new StationPackingInstance(Maps.newHashMap());
    final ITerminationCriterion terminationCriterion = mock(ITerminationCriterion.class);
    final long seed = 0;

    @BeforeClass
    public static void setUp() {

        final String interferenceFolder = Resources.getResource("data/021814SC3M").getPath();
        final boolean compact = true;
        pythonAssignmentVerifierDecorator = new PythonAssignmentVerifierDecorator(solver, new PythonInterpreterContainer(interferenceFolder, compact));
        nonCompactPythonAssignmentVerifierDecorator = new PythonAssignmentVerifierDecorator(solver, new PythonInterpreterContainer(interferenceFolder, !compact));

    }

    @Test(expected=IllegalStateException.class)
    public void testDomainViolationCompactIntereference() {

        Map<Integer,Set<Station>> domainViolationAnswer = ImmutableMap.of(-1, ImmutableSet.of(new Station(1)));
        when(solver.solve(any(StationPackingInstance.class), eq(terminationCriterion), eq(seed)))
                .thenReturn(new SolverResult(SATResult.SAT, 0, domainViolationAnswer, SolverResult.SolvedBy.UNKNOWN));

        pythonAssignmentVerifierDecorator.solve(instance, terminationCriterion, seed);

    }

    @Test
    public void testNoViolationCompactInterference() {

        when(solver.solve(any(StationPackingInstance.class), eq(terminationCriterion), eq(seed)))
                .thenReturn(new SolverResult(SATResult.SAT, 0, StationPackingTestUtils.getSimpleInstanceAnswer(), SolverResult.SolvedBy.UNKNOWN));

        SolverResult result = pythonAssignmentVerifierDecorator.solve(instance, terminationCriterion, seed);
        assertEquals(result.getAssignment(), StationPackingTestUtils.getSimpleInstanceAnswer());

    }

    @Test
    public void testNoViolationNonCompactInterference() {

        when(solver.solve(any(StationPackingInstance.class), eq(terminationCriterion), eq(seed)))
                .thenReturn(new SolverResult(SATResult.SAT, 0, StationPackingTestUtils.getSimpleInstanceAnswer(), SolverResult.SolvedBy.UNKNOWN));

        SolverResult result = nonCompactPythonAssignmentVerifierDecorator.solve(instance, terminationCriterion, seed);
        assertEquals(result.getAssignment(), StationPackingTestUtils.getSimpleInstanceAnswer());

    }

}
