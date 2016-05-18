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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import containmentcache.ILockableContainmentCache;
import containmentcache.SimpleCacheSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 19/04/15.
 */
@Slf4j
public class SatisfiabilityCache implements ISatisfiabilityCache {

    final ILockableContainmentCache<Station, ContainmentCacheSATEntry> SATCache;
    final ILockableContainmentCache<Station, ContainmentCacheUNSATEntry> UNSATCache;
    @Getter
    final ImmutableBiMap<Station, Integer> permutation;

    public SatisfiabilityCache(
            BiMap<Station, Integer> permutation,
            ILockableContainmentCache<Station, ContainmentCacheSATEntry> SATCache,
            ILockableContainmentCache<Station, ContainmentCacheUNSATEntry> UNSATCache) {
        this.permutation = ImmutableBiMap.copyOf(permutation);
        this.SATCache = SATCache;
        this.UNSATCache = UNSATCache;
    }

    @Override
    public ContainmentCacheSATResult proveSATBySuperset(final StationPackingInstance aInstance, final Predicate<ContainmentCacheSATEntry> ignorePredicate) {
        // try to narrow down the entries we have to search by only looking at supersets
        try {
            SATCache.getReadLock().lock();
            final Iterable<ContainmentCacheSATEntry> iterable = SATCache.getSupersets(new SimpleCacheSet<Station>(aInstance.getStations(), permutation));
            return StreamSupport.stream(iterable.spliterator(), false)
                    /**
                     * The entry must contain at least every station in the query in order to provide a solution (hence superset)
                     * The entry should also be a solution to the problem, which it will be as long as the solution can project onto the query's domains since they come from the set of interference constraints
                     */
                    .filter(entry -> entry.isSolutionTo(aInstance))
                    .filter(ignorePredicate)
                    .map(entry -> new ContainmentCacheSATResult(entry.getAssignmentChannelToStation(), entry.getKey()))
                    .findAny()
                    .orElse(ContainmentCacheSATResult.failure());
        } finally {
            SATCache.getReadLock().unlock();
        }
    }

    @Override
    public ContainmentCacheUNSATResult proveUNSATBySubset(final StationPackingInstance aInstance) {
        // try to narrow down the entries we have to search by only looking at subsets
        try {
            UNSATCache.getReadLock().lock();
            final Iterable<ContainmentCacheUNSATEntry> iterable = UNSATCache.getSubsets(new SimpleCacheSet<Station>(aInstance.getStations(), permutation));
            return StreamSupport.stream(iterable.spliterator(), false)
                /*
                 * The entry's stations should be a subset of the query's stations (so as to be less constrained)
                 * and each station in the entry must have larger than or equal to the corresponding station domain in the target (so as to be less constrained)
                 */
                    .filter(entry -> isSupersetOrEqualToByDomains(entry.getDomains(), aInstance.getDomains()))
                    .map(entry -> new ContainmentCacheUNSATResult(entry.getKey()))
                    .findAny()
                    .orElse(ContainmentCacheUNSATResult.failure());
        } finally {
            UNSATCache.getReadLock().unlock();
        }
    }

    @Override
    public void add(ContainmentCacheSATEntry SATEntry) {
        SATCache.add(SATEntry);
    }

    @Override
    public void add(ContainmentCacheUNSATEntry UNSATEntry) {
        UNSATCache.add(UNSATEntry);
    }

    /**
     * Domain a has less stations than domain b because of previous method call getSubsets();
     * If each station domain in domain a has same or more channels than the matching station in domain b,
     * then a is superset of b
     *
     * @param a superset domain
     * @param b subset domain
     * @return true if a's domain is a superset of b's domain
     */
    private boolean isSupersetOrEqualToByDomains(Map<Station, Set<Integer>> a, Map<Station, Set<Integer>> b) {
        return a.entrySet().stream().allMatch(entry -> {
            final Set<Integer> integers = b.get(entry.getKey());
            return integers != null && entry.getValue().containsAll(integers);
        });
    }

    /**
     * removes redundant SAT entries from this SATCache
     *
     * @return list of cache entries to be removed
     */
    @Override
    public List<ContainmentCacheSATEntry> filterSAT(IStationManager stationManager, boolean strong) {
        List<ContainmentCacheSATEntry> prunableEntries = Collections.synchronizedList(new ArrayList<>());
        Iterable<ContainmentCacheSATEntry> satEntries = SATCache.getSets();

        final AtomicLong counter = new AtomicLong();
        SATCache.getReadLock().lock();
        try {
            StreamSupport.stream(satEntries.spliterator(), true)
                    .forEach(cacheEntry -> {
                        if (counter.getAndIncrement() % 1000 == 0) {
                            log.info("Scanned {} / {} entries; Found {} prunables", counter.get(), SATCache.size(), prunableEntries.size());
                        }
                        if ((strong && shouldFilterStrong(cacheEntry, stationManager)) || (!strong && shouldFilterWeak(cacheEntry))) {
                            prunableEntries.add(cacheEntry);
                        }
                    });
        } finally {
            SATCache.getReadLock().unlock();
        }

        prunableEntries.forEach(SATCache::remove);
        return prunableEntries;
    }

    private boolean shouldFilterWeak(ContainmentCacheSATEntry cacheEntry) {
        Iterable<ContainmentCacheSATEntry> supersets = SATCache.getSupersets(cacheEntry);
        return StreamSupport.stream(supersets.spliterator(), false)
                        .filter(entry -> entry.hasMoreSolvingPower(cacheEntry))
                        .findAny().isPresent();
    }

    private boolean shouldFilterStrong(ContainmentCacheSATEntry cacheEntry, IStationManager stationManager) {
        final Map<Station, Set<Integer>> domains = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : cacheEntry.getAssignmentStationToChannel().entrySet()) {
            final Station station = stationManager.getStationfromID(e.getKey());
            domains.put(station, stationManager.getRestrictedDomain(station, e.getValue(), false));
        }
        final StationPackingInstance i = new StationPackingInstance(domains);
        return proveSATBySuperset(i, entry -> entry != cacheEntry).isValid();
    }

    /**
     * removes redundant UNSAT entries from this UNSATCache
     *
     * @return list of cache entries to be removed
     */
    @Override
    public List<ContainmentCacheUNSATEntry> filterUNSAT() {
        List<ContainmentCacheUNSATEntry> prunableEntries = new ArrayList<>();
        Iterable<ContainmentCacheUNSATEntry> unsatEntries = UNSATCache.getSets();


        final AtomicLong counter = new AtomicLong();
        UNSATCache.getReadLock().lock();
        try {
            unsatEntries.forEach(cacheEntry -> {
                if (counter.getAndIncrement() % 1000 == 0) {
                    log.info("Scanned {} / {} entries; Found {} prunables", counter.get(), UNSATCache.size(), prunableEntries.size());
                }
                Iterable<ContainmentCacheUNSATEntry> subsets = UNSATCache.getSubsets(cacheEntry);
                // For two UNSAT problems P and Q, if Q has less stations to pack,
                // and each station has more candidate channels, then Q is less restrictive than P
                Optional<ContainmentCacheUNSATEntry> lessRestrictiveUNSAT =
                        StreamSupport.stream(subsets.spliterator(), true)
                                .filter(entry -> entry.isLessRestrictive(cacheEntry))
                                .findAny();
                if (lessRestrictiveUNSAT.isPresent()) {
                    prunableEntries.add(cacheEntry);
                }
            });
        } finally {
            UNSATCache.getReadLock().unlock();
        }

        prunableEntries.forEach(UNSATCache::remove);
        return prunableEntries;
    }

    @Override
    public List<ContainmentCacheSATEntry> findMaxIntersections(StationPackingInstance instance, int k) {
        BitSet bitSet = new SimpleCacheSet<>(instance.getStations(), permutation).getBitSet();
        ImmutableMap<Station, Set<Integer>> domains = instance.getDomains();
        SATCache.getReadLock().lock();
        try {
            return StreamSupport.stream(SATCache.getSets().spliterator(), false)
                    .sorted((a, b) -> {
                        final BitSet aCopy = (BitSet) a.getBitSet().clone();
                        final BitSet bCopy = (BitSet) b.getBitSet().clone();
                        aCopy.and(bitSet);
                        bCopy.and(bitSet);
                        aCopy.stream().forEach(i -> {
                            Station station = permutation.inverse().get(i);
                            if (!domains.get(station).contains(a.getAssignmentStationToChannel().get(station.getID()))) {
                                aCopy.clear(i);
                            }
                        });
                        bCopy.stream().forEach(i -> {
                            Station station = permutation.inverse().get(i);
                            if (!domains.get(station).contains(b.getAssignmentStationToChannel().get(station.getID()))) {
                                bCopy.clear(i);
                            }
                        });
                        return Integer.compare(aCopy.cardinality(), bCopy.cardinality());
                    })
                    .limit(k)
                    .collect(Collectors.toList());
        } finally {
            SATCache.getReadLock().unlock();
        }
    }

}
