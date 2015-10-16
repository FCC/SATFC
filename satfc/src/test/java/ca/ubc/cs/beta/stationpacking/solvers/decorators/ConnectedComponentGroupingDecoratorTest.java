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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Set;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ConnectedComponentGroupingDecoratorTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testSplitIntoComponents() {
        // should test that solve is called # components times on the decorator for a SAT problem
        final long seed = 0;
        final ISolver solver = mock(ISolver.class);
        final IComponentGrouper grouper = mock(IComponentGrouper.class);
        final IConstraintManager constraintManager = mock(IConstraintManager.class);
        final ITerminationCriterion terminationCriterion = mock(ITerminationCriterion.class);
        final ConnectedComponentGroupingDecorator connectedComponentGroupingDecorator = new ConnectedComponentGroupingDecorator(solver, grouper, constraintManager);

        final StationPackingInstance instance = new StationPackingInstance(Maps.newHashMap());
        final Set<Station> componentA = mock(Set.class);
        final Set<Station> componentB = mock(Set.class);
        final Set<Station> componentC = mock(Set.class);
        final Set<Set<Station>> components = Sets.newHashSet(componentA, componentB, componentC);

        when(grouper.group(instance, constraintManager)).thenReturn(components);
        when(solver.solve(any(StationPackingInstance.class), eq(terminationCriterion), eq(seed))).thenReturn(new SolverResult(SATResult.SAT, 0, new HashMap<>(), SolvedBy.UNKNOWN));
        connectedComponentGroupingDecorator.solve(instance, terminationCriterion, seed);

        verify(solver, times(components.size())).solve(any(StationPackingInstance.class), eq(terminationCriterion), eq(seed));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEarlyStopping() {
        // should test that if a problem has 3 UNSAT components, then the code examines only 1 component
        final long seed = 0;
        final ISolver solver = mock(ISolver.class);
        final IComponentGrouper grouper = mock(IComponentGrouper.class);
        final IConstraintManager constraintManager = mock(IConstraintManager.class);
        final ITerminationCriterion terminationCriterion = mock(ITerminationCriterion.class);
        final ConnectedComponentGroupingDecorator connectedComponentGroupingDecorator = new ConnectedComponentGroupingDecorator(solver, grouper, constraintManager);

        final StationPackingInstance instance = new StationPackingInstance(Maps.newHashMap());
        final Set<Station> componentA = mock(Set.class);
        final Set<Station> componentB = mock(Set.class);
        final Set<Station> componentC = mock(Set.class);
        final Set<Set<Station>> components = Sets.newHashSet(componentA, componentB, componentC);

        when(grouper.group(instance, constraintManager)).thenReturn(components);
        final SolverResult unsat = SolverResult.createNonSATResult(SATResult.UNSAT, 0, SolvedBy.UNKNOWN);
        when(solver.solve(any(StationPackingInstance.class), eq(terminationCriterion), eq(seed))).thenReturn(unsat);
        connectedComponentGroupingDecorator.solve(instance, terminationCriterion, seed);

        verify(solver, times(1)).solve(any(StationPackingInstance.class), eq(terminationCriterion), eq(seed));
    }


}
