package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ConstraintKey;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.TestConstraint;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.TestConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.VoidSolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.infinite.NeverEndingTerminationCriterion;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by newmanne on 2016-04-13.
 */
public class PreviousAssignmentContainsAnswerDecoratorTest {

    final Station s1 = new Station(1);
    final Station s2 = new Station(2);

    @Test
    public void testPreviousAssignmentSolves() throws Exception {
        final IConstraintManager constraintManager = new TestConstraintManager(new ArrayList<>());
        final PreviousAssignmentContainsAnswerDecorator decorator = new PreviousAssignmentContainsAnswerDecorator(new VoidSolver(), constraintManager);
        final Map<Station, Set<Integer>> domains = new HashMap<>();
        domains.put(s1, Sets.newHashSet(1));
        final Map<Station, Integer> previousAssignment = ImmutableMap.of(s1, 1);
        final StationPackingInstance instance = new StationPackingInstance(domains, previousAssignment);
        final SolverResult solve = decorator.solve(instance, new NeverEndingTerminationCriterion(), 0);
        Assert.assertEquals(SATResult.SAT, solve.getResult());
    }

    @Test
    public void testPreviousAssignmentDoesNotSolveInvalid() throws Exception{
        final IConstraintManager constraintManager = new TestConstraintManager(
                Lists.newArrayList(
                        new TestConstraint(ConstraintKey.CO, 1, s1, Sets.newHashSet(s2))
                )
        );
        final PreviousAssignmentContainsAnswerDecorator decorator = new PreviousAssignmentContainsAnswerDecorator(new VoidSolver(), constraintManager);
        final Map<Station, Set<Integer>> domains = new HashMap<>();
        domains.put(s1, Sets.newHashSet(1));
        domains.put(s2, Sets.newHashSet(1, 2));
        Map<Station, Integer> previousAssignment = ImmutableMap.of(s1, 1, s2, 1);
        StationPackingInstance instance = new StationPackingInstance(domains, previousAssignment);
        SolverResult solve = decorator.solve(instance, new NeverEndingTerminationCriterion(), 0);
        Assert.assertEquals(SATResult.TIMEOUT, solve.getResult());

        previousAssignment = ImmutableMap.of(s1, 1, s2, 2);
        instance = new StationPackingInstance(domains, previousAssignment);
        solve = decorator.solve(instance, new NeverEndingTerminationCriterion(), 0);
        Assert.assertEquals(SATResult.SAT, solve.getResult());

        // Test invalid domain doesn't count
        domains.get(s2).remove(2);
        instance = new StationPackingInstance(domains, previousAssignment);
        solve = decorator.solve(instance, new NeverEndingTerminationCriterion(), 0);
        Assert.assertEquals(SATResult.TIMEOUT, solve.getResult());
    }

}