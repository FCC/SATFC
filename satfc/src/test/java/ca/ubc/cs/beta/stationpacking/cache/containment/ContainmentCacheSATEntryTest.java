package ca.ubc.cs.beta.stationpacking.cache.containment;

import ca.ubc.cs.beta.stationpacking.base.Station;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Created by emily404 on 5/12/15.
 */
public class ContainmentCacheSATEntryTest {

    // stations and channels
    private Station s1 = new Station(1);
    private Station s2 = new Station(2);
    private Integer c1 = 1;
    private Integer c2 = 2;
    private Integer c3 = 3;

    /**
     * an entry with same key is not considered the superset
     */
    @Test
    public void isSupersetSameKeyTest(){
        Map<Integer, Set<Station>> asgmnt = new HashMap<>();
        String key = "key";
        ContainmentCacheSATEntry entry = new ContainmentCacheSATEntry(asgmnt, key);

        Assert.assertFalse(entry.hasMoreSolvingPower(entry));
    }

    /**
     * Good case when an entry is truly superset of than another entry
     */
    @Test
    public void isSupersetTest(){
        Map<Integer, Set<Station>> asgmnt1 = new HashMap<>();
        asgmnt1.put(c1, new HashSet<>(Arrays.asList(s1)));
        String k1 = "k1";
        ContainmentCacheSATEntry subsetEntry = new ContainmentCacheSATEntry(asgmnt1, k1);

        Map<Integer, Set<Station>> asgmnt2 = new HashMap<>();
        asgmnt2.put(c1, new HashSet<>(Arrays.asList(s1)));
        asgmnt2.put(c2, new HashSet<>(Arrays.asList(s2)));
        String k2 = "k2";
        ContainmentCacheSATEntry supersetEntry = new ContainmentCacheSATEntry(asgmnt2, k2);

        Assert.assertTrue(supersetEntry.hasMoreSolvingPower(subsetEntry));
    }

    /**
     * When the assignments are identical, as long as the key differs, they are superset of each other
     */
    @Test
    public void isSupersetSameAssignmentTest(){
        Map<Integer, Set<Station>> asgmnt1 = new HashMap<>();
        asgmnt1.put(c1, new HashSet<>(Arrays.asList(s1)));
        String k1 = "k1";
        ContainmentCacheSATEntry e1 = new ContainmentCacheSATEntry(asgmnt1, k1);

        Map<Integer, Set<Station>> asgmnt2 = new HashMap<>();
        asgmnt2.put(c1, new HashSet<>(Arrays.asList(s1)));
        String k2 = "k2";
        ContainmentCacheSATEntry e2 = new ContainmentCacheSATEntry(asgmnt2, k2);

        Assert.assertTrue(e1.hasMoreSolvingPower(e2));
        Assert.assertTrue(e2.hasMoreSolvingPower(e1));
    }

    /**
     * The assignments don't align at all, they are not superset of each other
     */
    @Test
    public void isNotSupersetTest(){
        Map<Integer, Set<Station>> asgmnt1 = new HashMap<>();
        asgmnt1.put(c1, new HashSet<>(Arrays.asList(s1)));
        String k1 = "k1";
        ContainmentCacheSATEntry e1 = new ContainmentCacheSATEntry(asgmnt1, k1);

        Map<Integer, Set<Station>> asgmnt2 = new HashMap<>();
        asgmnt2.put(c1, new HashSet<>(Arrays.asList(s2)));
        String k2 = "k2";
        ContainmentCacheSATEntry e2 = new ContainmentCacheSATEntry(asgmnt2, k2);

        Assert.assertFalse(e1.hasMoreSolvingPower(e2));
        Assert.assertFalse(e2.hasMoreSolvingPower(e1));
    }

    /**
     * The channels in assignments do not align, they are not supersets of each other
     */
    @Test
    public void offChannelTest(){
        Map<Integer, Set<Station>> asgmnt1 = new HashMap<>();
        asgmnt1.put(c1, new HashSet<>(Arrays.asList(s1)));
        asgmnt1.put(c2, new HashSet<>(Arrays.asList(s2)));
        String k1 = "k1";
        ContainmentCacheSATEntry e1 = new ContainmentCacheSATEntry(asgmnt1, k1);

        Map<Integer, Set<Station>> asgmnt2 = new HashMap<>();
        asgmnt2.put(c1, new HashSet<>(Arrays.asList(s1)));
        asgmnt2.put(c3, new HashSet<>(Arrays.asList(s2)));
        String k2 = "k2";
        ContainmentCacheSATEntry e2 = new ContainmentCacheSATEntry(asgmnt2, k2);

        Assert.assertFalse(e1.hasMoreSolvingPower(e2));
        Assert.assertFalse(e2.hasMoreSolvingPower(e1));
    }

}
