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
public class ContainmentCacheSATEntryTest {

    // stations and channels
    private Station s1 = new Station(1);
    private Station s2 = new Station(2);
    private Integer c1 = 1;
    private Integer c2 = 2;
    private Integer c3 = 3;
    private ImmutableBiMap<Station, Integer> permutation = ImmutableBiMap.of(s1, 1, s2, 2);

    /**
     * an entry with same key is not considered the superset
     */
    @Test
    public void isSupersetSameKeyTest(){
        Map<Integer, Set<Station>> asgmnt = new HashMap<>();
        ContainmentCacheSATEntry entry = new ContainmentCacheSATEntry(asgmnt, permutation);

        Assert.assertFalse(entry.hasMoreSolvingPower(entry));
    }

    /**
     * Good case when an entry is truly superset of than another entry
     */
    @Test
    public void isSupersetTest(){
        Map<Integer, Set<Station>> asgmnt1 = new HashMap<>();
        asgmnt1.put(c1, new HashSet<>(Arrays.asList(s1)));
        ContainmentCacheSATEntry subsetEntry = new ContainmentCacheSATEntry(asgmnt1, permutation);

        Map<Integer, Set<Station>> asgmnt2 = new HashMap<>();
        asgmnt2.put(c1, new HashSet<>(Arrays.asList(s1)));
        asgmnt2.put(c2, new HashSet<>(Arrays.asList(s2)));
        ContainmentCacheSATEntry supersetEntry = new ContainmentCacheSATEntry(asgmnt2, permutation);

        Assert.assertTrue(supersetEntry.hasMoreSolvingPower(subsetEntry));
    }

    /**
     * When the assignments are identical, as long as the key differs, they are superset of each other
     */
    @Test
    public void isSupersetSameAssignmentTest(){
        Map<Integer, Set<Station>> asgmnt1 = new HashMap<>();
        asgmnt1.put(c1, new HashSet<>(Arrays.asList(s1)));
        ContainmentCacheSATEntry e1 = new ContainmentCacheSATEntry(asgmnt1, permutation);

        Map<Integer, Set<Station>> asgmnt2 = new HashMap<>();
        asgmnt2.put(c1, new HashSet<>(Arrays.asList(s1)));
        ContainmentCacheSATEntry e2 = new ContainmentCacheSATEntry(asgmnt2, permutation);

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
        ContainmentCacheSATEntry e1 = new ContainmentCacheSATEntry(asgmnt1, permutation);

        Map<Integer, Set<Station>> asgmnt2 = new HashMap<>();
        asgmnt2.put(c1, new HashSet<>(Arrays.asList(s2)));
        ContainmentCacheSATEntry e2 = new ContainmentCacheSATEntry(asgmnt2, permutation);

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
        ContainmentCacheSATEntry e1 = new ContainmentCacheSATEntry(asgmnt1, permutation);

        Map<Integer, Set<Station>> asgmnt2 = new HashMap<>();
        asgmnt2.put(c1, new HashSet<>(Arrays.asList(s1)));
        asgmnt2.put(c3, new HashSet<>(Arrays.asList(s2)));
        ContainmentCacheSATEntry e2 = new ContainmentCacheSATEntry(asgmnt2, permutation);

        Assert.assertFalse(e1.hasMoreSolvingPower(e2));
        Assert.assertFalse(e2.hasMoreSolvingPower(e1));
    }

}
