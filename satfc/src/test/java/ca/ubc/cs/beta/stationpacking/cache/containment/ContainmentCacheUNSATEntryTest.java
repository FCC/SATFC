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
package ca.ubc.cs.beta.stationpacking.cache.containment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableBiMap;

import ca.ubc.cs.beta.stationpacking.base.Station;

/**
 * Created by emily404 on 5/12/15.
 */
public class ContainmentCacheUNSATEntryTest {

	final ImmutableBiMap<Station, Integer> permutation = ImmutableBiMap.of(new Station(1), 1, new Station(2), 2);
	
    /**
     * the same entry is not considered less restrictive
     */
    @Test
    public void isLessRestrictiveSameEntriesTest(){
        Map<Station, Set<Integer>> domains = new HashMap<>();
        ContainmentCacheUNSATEntry entry = new ContainmentCacheUNSATEntry(domains, permutation);

        Assert.assertFalse(entry.isLessRestrictive(entry));
    }

    /**
     * Good case when an entry is truly less restrictive than another entry
     */
    @Test
    public void lessStationsMoreChannelsTest(){
        Map<Station, Set<Integer>> lesResDomains = new HashMap<>();
        lesResDomains.put(new Station(1), new HashSet<>(Arrays.asList(19,20)));
        ContainmentCacheUNSATEntry lessResEntry = new ContainmentCacheUNSATEntry(lesResDomains, permutation);

        Map<Station, Set<Integer>> moreResDomains = new HashMap<>();
        moreResDomains.put(new Station(1), new HashSet<>(Arrays.asList(19)));
        moreResDomains.put(new Station(2), new HashSet<>(Arrays.asList(19)));
        ContainmentCacheUNSATEntry moreResEntry = new ContainmentCacheUNSATEntry(moreResDomains, permutation);

        Assert.assertTrue(lessResEntry.isLessRestrictive(moreResEntry));
    }

    /**
     * more stations to pack means it is more restrictive
     */
    @Test
    public void moreStationTest(){
        Map<Station, Set<Integer>> moreStationDomains = new HashMap<>();
        moreStationDomains.put(new Station(1), new HashSet<>(Arrays.asList(19,20)));
        moreStationDomains.put(new Station(2), new HashSet<>(Arrays.asList(19, 20)));
        ContainmentCacheUNSATEntry moreStationEntry = new ContainmentCacheUNSATEntry(moreStationDomains, permutation);

        Map<Station, Set<Integer>> lessStationDomains = new HashMap<>();
        lessStationDomains.put(new Station(1), new HashSet<>(Arrays.asList(19, 20)));
        ContainmentCacheUNSATEntry lessStationEntry = new ContainmentCacheUNSATEntry(lessStationDomains, permutation);

        Assert.assertFalse(moreStationEntry.isLessRestrictive(lessStationEntry));
    }

    /**
     * same stations to pack but at least one station has less candidate channels still indicates more restriction
     */
    @Test
    public void sameStationLessChannelTest(){
        Map<Station, Set<Integer>> lessChannelDomains = new HashMap<>();
        lessChannelDomains.put(new Station(1), new HashSet<>(Arrays.asList(19)));
        ContainmentCacheUNSATEntry moreChannelEntry = new ContainmentCacheUNSATEntry(lessChannelDomains, permutation);

        Map<Station, Set<Integer>> moreChannelDomains = new HashMap<>();
        moreChannelDomains.put(new Station(1), new HashSet<>(Arrays.asList(19, 20)));
        ContainmentCacheUNSATEntry lessChannelEntry = new ContainmentCacheUNSATEntry(moreChannelDomains, permutation);

        Assert.assertFalse(moreChannelEntry.isLessRestrictive(lessChannelEntry));
    }

    /**
     * one station contains more candidate channel while the other contains less candidate channels,
     * this does not indicate less restrictive
     */
    @Test
    public void overlapDomainTest(){
        Map<Station, Set<Integer>> firstDomains = new HashMap<>();
        firstDomains.put(new Station(1), new HashSet<>(Arrays.asList(19,20)));
        firstDomains.put(new Station(2), new HashSet<>(Arrays.asList(21)));
        ContainmentCacheUNSATEntry firstEntry = new ContainmentCacheUNSATEntry(firstDomains, permutation);

        Map<Station, Set<Integer>> secondDomains = new HashMap<>();
        secondDomains.put(new Station(1), new HashSet<>(Arrays.asList(19)));
        secondDomains.put(new Station(2), new HashSet<>(Arrays.asList(21, 22)));
        ContainmentCacheUNSATEntry secondEntry = new ContainmentCacheUNSATEntry(secondDomains, permutation);

        Assert.assertFalse(firstEntry.isLessRestrictive(secondEntry));
    }
}
