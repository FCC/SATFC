package ca.ubc.cs.beta.stationpacking.cache.containment;

import ca.ubc.cs.beta.stationpacking.base.Station;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Created by emily404 on 5/12/15.
 */
public class ContainmentCacheUNSATEntryTest {

    /**
     * the same entry is not considered less restrictive
     */
    @Test
    public void isLessRestrictiveSameEntriesTest(){
        Map<Station, Set<Integer>> domains = new HashMap<>();
        String key = "key";
        ContainmentCacheUNSATEntry entry = new ContainmentCacheUNSATEntry(domains, key);

        Assert.assertFalse(entry.isLessRestrictive(entry));
    }

    /**
     * Good case when an entry is truly less restrictive than another entry
     */
    @Test
    public void lessStationsMoreChannelsTest(){
        Map<Station, Set<Integer>> lesResDomains = new HashMap<>();
        lesResDomains.put(new Station(1), new HashSet<>(Arrays.asList(5,6)));
        String lessResKey = "lessResKey";
        ContainmentCacheUNSATEntry lessResEntry = new ContainmentCacheUNSATEntry(lesResDomains, lessResKey);

        Map<Station, Set<Integer>> moreResDomains = new HashMap<>();
        moreResDomains.put(new Station(1), new HashSet<>(Arrays.asList(5)));
        moreResDomains.put(new Station(2), new HashSet<>(Arrays.asList(5)));
        String moreResKey = "moreResKey";
        ContainmentCacheUNSATEntry moreResEntry = new ContainmentCacheUNSATEntry(moreResDomains, moreResKey);

        Assert.assertTrue(lessResEntry.isLessRestrictive(moreResEntry));
    }

    /**
     * more stations to pack means it is more restrictive
     */
    @Test
    public void moreStationTest(){
        Map<Station, Set<Integer>> moreStationDomains = new HashMap<>();
        moreStationDomains.put(new Station(1), new HashSet<>(Arrays.asList(5, 6)));
        moreStationDomains.put(new Station(2), new HashSet<>(Arrays.asList(5, 6)));
        String lessResKey = "lessStationsKey";
        ContainmentCacheUNSATEntry moreStationEntry = new ContainmentCacheUNSATEntry(moreStationDomains, lessResKey);

        Map<Station, Set<Integer>> lessStationDomains = new HashMap<>();
        lessStationDomains.put(new Station(1), new HashSet<>(Arrays.asList(5, 6)));
        String moreResKey = "moreStationsKey";
        ContainmentCacheUNSATEntry lessStationEntry = new ContainmentCacheUNSATEntry(lessStationDomains, moreResKey);

        Assert.assertFalse(moreStationEntry.isLessRestrictive(lessStationEntry));
    }

    /**
     * same stations to pack but at least one station has less candidate channels still indicates more restriction
     */
    @Test
    public void sameStationLessChannelTest(){
        Map<Station, Set<Integer>> lessChannelDomains = new HashMap<>();
        lessChannelDomains.put(new Station(1), new HashSet<>(Arrays.asList(5)));
        String lessChannelKey = "lessChannelKey";
        ContainmentCacheUNSATEntry moreChannelEntry = new ContainmentCacheUNSATEntry(lessChannelDomains, lessChannelKey);

        Map<Station, Set<Integer>> moreChannelDomains = new HashMap<>();
        moreChannelDomains.put(new Station(1), new HashSet<>(Arrays.asList(5, 6)));
        String moreChannelKey = "moreChannelKey";
        ContainmentCacheUNSATEntry lessChannelEntry = new ContainmentCacheUNSATEntry(moreChannelDomains, moreChannelKey);

        Assert.assertFalse(moreChannelEntry.isLessRestrictive(lessChannelEntry));
    }

    /**
     * empty channels indicates more restriction
     */
    @Test
    public void emptyChannelTest(){
        Map<Station, Set<Integer>> emptyChannelDomains = new HashMap<>();
        emptyChannelDomains.put(new Station(1), new HashSet<>(Arrays.asList()));
        String emptyDomainsKey = "emptyDomainsKey";
        ContainmentCacheUNSATEntry emptyDomainsEntry = new ContainmentCacheUNSATEntry(emptyChannelDomains, emptyDomainsKey);

        Map<Station, Set<Integer>> nonEmptyDomains = new HashMap<>();
        nonEmptyDomains.put(new Station(1), new HashSet<>(Arrays.asList(5, 6)));
        String nonEmptyDomainsKey = "nonEmptyDomainsKey";
        ContainmentCacheUNSATEntry nonEmptyDomainsEntry = new ContainmentCacheUNSATEntry(nonEmptyDomains, nonEmptyDomainsKey);

        Assert.assertFalse(emptyDomainsEntry.isLessRestrictive(nonEmptyDomainsEntry));
    }

    /**
     * one station contains more candidate channel while the other contains less candidate channels,
     * this does not indicate less restrictive
     */
    @Test
    public void Test(){
        Map<Station, Set<Integer>> firstDomains = new HashMap<>();
        firstDomains.put(new Station(1), new HashSet<>(Arrays.asList(5,6)));
        firstDomains.put(new Station(2), new HashSet<>(Arrays.asList(7)));
        String key1 = "key1";
        ContainmentCacheUNSATEntry firstEntry = new ContainmentCacheUNSATEntry(firstDomains, key1);

        Map<Station, Set<Integer>> secondDomains = new HashMap<>();
        secondDomains.put(new Station(1), new HashSet<>(Arrays.asList(5)));
        secondDomains.put(new Station(2), new HashSet<>(Arrays.asList(7, 8)));
        String key2 = "key2";
        ContainmentCacheUNSATEntry secondEntry = new ContainmentCacheUNSATEntry(secondDomains, key2);

        Assert.assertFalse(firstEntry.isLessRestrictive(secondEntry));
    }
}
