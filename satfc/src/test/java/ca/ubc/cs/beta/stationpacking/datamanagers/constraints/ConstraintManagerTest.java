package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

public abstract class ConstraintManagerTest {

    final static protected String DOMAIN_PATH = Resources.getResource("data/testInterference/" + DataManager.DOMAIN_FILE).getPath();

    protected abstract IConstraintManager getConstraintManager() throws Exception;
    protected IStationManager getDomainManager() throws Exception {
        return new DomainStationManager(DOMAIN_PATH);
    }

    public static class ChannelSpecificConstraintManagerTest extends ConstraintManagerTest {

        @Override
        protected IConstraintManager getConstraintManager() throws Exception {
            final String interferencePath = Resources.getResource("data/testInterference").getPath();
            return new ChannelSpecificConstraintManager(getDomainManager(), interferencePath + File.separator + "channelspecific" + File.separator + DataManager.INTERFERENCES_FILE);
        }

    }

    public static class UnabridgedConstraintManagerTest extends ConstraintManagerTest {

        @Override
        protected IConstraintManager getConstraintManager() throws Exception {
            final String interferencePath = Resources.getResource("data/testInterference").getPath();
            return new UnabridgedFormatConstraintManager(getDomainManager(), interferencePath + File.separator + "unabridged" + File.separator + DataManager.INTERFERENCES_FILE);
        }

        @Test
        public void testUnabridgedFormat() throws Exception {
            final String interferencePath = Resources.getResource("data/testInterference/unabridged").getPath() + "/Interference_Paired_ADJm1_ADJm2.csv";
            final IStationManager dm = new DomainStationManager(DOMAIN_PATH);
            final IConstraintManager constraintManager = new UnabridgedFormatConstraintManager(dm, interferencePath);
            // Test ADJ-1 and ADJ-2 constraints
            assertEquals(Sets.newHashSet(s(dm, 9)), constraintManager.getADJplusOneInterferingStations(s(dm, 10), 9));
            assertEquals(Sets.newHashSet(s(dm, 10)), constraintManager.getCOInterferingStations(s(dm, 9), 9));
            assertEquals(Sets.newHashSet(s(dm, 10)), constraintManager.getCOInterferingStations(s(dm, 9), 10));
            // Test ADJ-2 constraints
            assertEquals(Sets.newHashSet(s(dm, 9)), constraintManager.getADJplusTwoInterferingStations(s(dm, 10), 98));
            assertEquals(Sets.newHashSet(s(dm, 10)), constraintManager.getCOInterferingStations(s(dm, 9), 98));
            assertEquals(Sets.newHashSet(s(dm, 10)), constraintManager.getCOInterferingStations(s(dm, 9), 99));
            assertEquals(Sets.newHashSet(s(dm, 10)), constraintManager.getCOInterferingStations(s(dm, 9), 100));
            assertEquals(Sets.newHashSet(s(dm, 9)), constraintManager.getADJplusOneInterferingStations(s(dm, 10), 99));
            assertEquals(Sets.newHashSet(s(dm, 9)), constraintManager.getADJplusOneInterferingStations(s(dm, 10), 98));
        }


    }

    @Test
    public void testConstraints() throws Exception {
        IConstraintManager cm = getConstraintManager();
        IStationManager dm = getDomainManager();

        // simple CO
        assertEquals(Sets.newHashSet(s(dm, 2), s(dm, 3)), cm.getCOInterferingStations(s(dm, 1), 1));

        // adj+1 and implied CO
        assertEquals(Sets.newHashSet(s(dm, 2)), cm.getCOInterferingStations(s(dm, 1), 7));
        assertEquals(Sets.newHashSet(s(dm, 2)), cm.getCOInterferingStations(s(dm, 1), 8));
        assertEquals(Sets.newHashSet(s(dm, 2)), cm.getADJplusOneInterferingStations(s(dm, 1), 7));

        // adj+2 and implied adj+2 and implied CO
        assertEquals(Sets.newHashSet(s(dm, 6)), cm.getCOInterferingStations(s(dm, 5), 99));
        assertEquals(Sets.newHashSet(s(dm, 6)), cm.getCOInterferingStations(s(dm, 5), 100));
        assertEquals(Sets.newHashSet(s(dm, 6)), cm.getCOInterferingStations(s(dm, 5), 101));
        assertEquals(Sets.newHashSet(s(dm, 6)), cm.getADJplusOneInterferingStations(s(dm, 5), 99));
        assertEquals(Sets.newHashSet(s(dm, 6)), cm.getADJplusOneInterferingStations(s(dm, 5), 100));
        assertEquals(Sets.newHashSet(s(dm, 6)), cm.getADJplusTwoInterferingStations(s(dm, 5), 99));

        // test some negative examples
        assertEquals(new HashSet<Station>(), cm.getCOInterferingStations(s(dm, 2), 1));
        assertEquals(new HashSet<Station>(), cm.getCOInterferingStations(s(dm, 2), 777));
        assertEquals(new HashSet<Station>(), cm.getADJplusTwoInterferingStations(s(dm, 5), 100));
    }


    @Test
    public void testIsSatisfyingAssignment() throws Exception {
        IConstraintManager cm = getConstraintManager();
        IStationManager dm = getDomainManager();
        final ImmutableMap<Integer, Set<Station>> badAssignmentViolatesCo = ImmutableMap.<Integer, Set<Station>>builder()
                .put(1, Sets.newHashSet(s(dm, 1), s(dm, 2))).build();
        assertFalse(cm.isSatisfyingAssignment(badAssignmentViolatesCo));
        final ImmutableMap<Integer, Set<Station>> goodAssignment = ImmutableMap.<Integer, Set<Station>>builder()
                .put(1, Sets.newHashSet(s(dm, 1)))
                .put(2, Sets.newHashSet(s(dm, 2)))
                .build();
        assertTrue(cm.isSatisfyingAssignment(goodAssignment));
    }

    @Test
    public void testGetRelevantConstraints() throws Exception {
        IConstraintManager cm = getConstraintManager();
        IStationManager dm = getDomainManager();
        final ImmutableMap<Station, Set<Integer>> oneAndTwoAdj = ImmutableMap.<Station, Set<Integer>>builder()
                .put(s(dm, 1), Sets.newHashSet(7, 8))
                .put(s(dm, 2), Sets.newHashSet(7, 8))
                .build();
        final ArrayList<Constraint> constraints = Lists.newArrayList(cm.getAllRelevantConstraints(oneAndTwoAdj));
        assertEquals(3, constraints.size());
        assertTrue(constraints.contains(new Constraint(s(dm, 1), s(dm, 2), 7, 7)));
        assertTrue(constraints.contains(new Constraint(s(dm, 1), s(dm, 2), 8, 8)));
        assertTrue(constraints.contains(new Constraint(s(dm, 1), s(dm, 2), 7, 8)));

        final ImmutableMap<Station, Set<Integer>> oneAndTwoNoIssue = ImmutableMap.<Station, Set<Integer>>builder()
                .put(s(dm, 1), Sets.newHashSet(7, 8))
                .put(s(dm, 2), Sets.newHashSet(1))
                .build();
        final ArrayList<Constraint> constraints2 = Lists.newArrayList(cm.getAllRelevantConstraints(oneAndTwoNoIssue));
        assertTrue(constraints2.isEmpty());

        final ImmutableMap<Station, Set<Integer>> fiveSixAdjTwo = ImmutableMap.<Station, Set<Integer>>builder()
                .put(s(dm, 5), Sets.newHashSet(99))
                .put(s(dm, 6), Sets.newHashSet(101))
                .build();
        final ArrayList<Constraint> constraints3 = Lists.newArrayList(cm.getAllRelevantConstraints(fiveSixAdjTwo));
        assertEquals(1, constraints3.size());
        assertTrue(constraints3.contains(new Constraint(s(dm, 5), s(dm, 6), 99, 101)));

    }

    protected static Station s(IStationManager stationManager, int id) {
        return stationManager.getStationfromID(id);
    }

}