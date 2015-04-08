/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.cache;

import static ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors.toImmutableList;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.utils.CacheUtils;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

/**
 * Created by newmanne on 1/24/15.
 */
@Slf4j
public class ContainmentCache {

    final ImmutableList<ImmutableList<ContainmentCacheUNSATEntry>> UNSATCache;
    final ImmutableList<ImmutableList<ContainmentCacheSATEntry>> SATCache;

    final ImmutableList<PermutableBitSetComparator> comparators;

    // A list of permutations of the numbers {1... N_STATIONS}
    final int[][] permutations;

    public ContainmentCache(List<ContainmentCacheSATEntry> SATData, List<ContainmentCacheUNSATEntry> UNSATData) {
        permutations = initPermutation();

        comparators = Arrays.stream(permutations)
                .map(PermutableBitSetComparator::new)
                .collect(toImmutableList());

        // Sort and store the data according to each comparator
        final ImmutableList.Builder<ImmutableList<ContainmentCacheSATEntry>> SATCacheBuilder = ImmutableList.builder();
        final ImmutableList.Builder<ImmutableList<ContainmentCacheUNSATEntry>> UNSATCacheBuilder = ImmutableList.builder();
        for (int i = 0; i < permutations.length; i++) {
            final PermutableBitSetComparator permutableBitSetComparator = comparators.get(i);
            final Comparator<ContainmentCacheSATEntry> SATComparator = (o1, o2) -> permutableBitSetComparator.compare(o1.getBitSet(), o2.getBitSet());
            final Comparator<ContainmentCacheUNSATEntry> UNSATComparator = (o1, o2) -> permutableBitSetComparator.compare(o1.getBitSet(), o2.getBitSet());
            SATData.sort(SATComparator);
            UNSATData.sort(UNSATComparator);
            SATCacheBuilder.add(ImmutableList.copyOf(SATData));
            UNSATCacheBuilder.add(ImmutableList.copyOf(UNSATData));
        }
        SATCache = SATCacheBuilder.build();
        UNSATCache = UNSATCacheBuilder.build();
    }

    // Read the permutations in from disk
    private int[][] initPermutation() {
        final int[][] permutationsTemp;
        try {
            final List<String> lines = Resources.readLines(Resources.getResource("precache_permutations.txt"), Charsets.UTF_8);
            final int numPermutations = lines.size();
            log.debug("Read " + numPermutations + " permutations");
            permutationsTemp = new int[numPermutations][StationPackingUtils.N_STATIONS];
            for (int i = 0; i < lines.size(); i++) {
                final List<String> numbers = Splitter.on(',').trimResults().splitToList(lines.get(i));
                Preconditions.checkState(numbers.size() == StationPackingUtils.N_STATIONS, "Each permutation must have length equal to N_STATIONS");
                for (int j = 0; j < numbers.size(); j++) {
                    permutationsTemp[i][j] = Integer.valueOf(numbers.get(j));
                }
            }
            return permutationsTemp;
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read from permutations file!", e);
        }
    }

    /**
     * Return a collection of entries that are potential supersets of the query
     * In order for an entry be a superset of a the query, a necessary condition is that is larger (in unsigned integer representation) than the query
     * We find the set of all entries larger than the query according to every permutation, and return the smallest such set so that we have the fewest entries to search further
     * This initial filtering can be performed efficiently using binary search
     * If we find an exact match, we return that much only
     */
    private Collection<ContainmentCacheSATEntry> smallSetLargerThanOrEqualTo(BitSet aBitSet) {
        final ContainmentCacheSATEntry fakeEntry = new ContainmentCacheSATEntry(aBitSet); // need to wrap this for comparator
        final List<Integer> binarySearchReturn = IntStream
                .range(0, permutations.length)
                .mapToObj(permutationIndex -> Collections.binarySearch(SATCache.get(permutationIndex), fakeEntry, (o1, o2) -> comparators.get(permutationIndex).compare(o1.getBitSet(), o2.getBitSet())))
                .collect(toImmutableList());
        // binary search return value is positive if the item is found in the list (the index). If it's in one list, it will be in all the lists, so might as well just work with the first
        final Integer exactMatchIndexInFirstPermutation = binarySearchReturn.get(0);
        if (exactMatchIndexInFirstPermutation >= 0) {
            log.debug("Found an exact match in the SAT cache!");
            return SATCache.get(0).subList(exactMatchIndexInFirstPermutation, exactMatchIndexInFirstPermutation + 1);
        } else {
            final List<Integer> insertionIndices = binarySearchReturn.stream().map(i -> -(i + 1)).collect(toImmutableList());
            log.debug("No exact match found. Insertion indices for the various permutations are {}", insertionIndices);
            final int bestInsertionPoint = Collections.max(insertionIndices);
            final int bestPermutation = insertionIndices.indexOf(bestInsertionPoint);
            final List<ContainmentCacheSATEntry> potentialSupersets = SATCache.get(bestPermutation).subList(bestInsertionPoint, SATCache.get(bestPermutation).size());
            log.debug("Returning a list of " + potentialSupersets.size() + " entries to search through");
            // reverse the list so the bigger things are seen first as they are most likely to be supersets
            return Lists.reverse(potentialSupersets);
        }
    }

    /**
     * Return a collection of entries that are potential subsets of the query
     * In order for an entry be a subset of a the query, a necessary condition is that is smaller (in unsigned integer representation) than the query
     * We find the set of all entries smaller than the query according to every permutation, and return the smallest such set so that we have the fewest entries to search further
     * This initial filtering can be performed efficiently using binary search
     * If we find an exact match, we return that much only
     */
    private Collection<ContainmentCacheUNSATEntry> smallSetSmallerThanOrEqualTo(BitSet aBitSet) {
        final ContainmentCacheUNSATEntry fakeEntry = new ContainmentCacheUNSATEntry(aBitSet); // need to wrap this for comparator
        final List<Integer> binarySearchReturn = IntStream
                .range(0, permutations.length)
                .mapToObj(permutationIndex -> Collections.binarySearch(UNSATCache.get(permutationIndex), fakeEntry, (o1, o2) -> comparators.get(permutationIndex).compare(o1.getBitSet(), o2.getBitSet())))
                .collect(toImmutableList());
        // binary search return value is positive if the item is found in the list (the index). If it's in one list, it will be in all the lists, so might as well just work with the first
        final Integer exactMatchIndexInFirstPermutation = binarySearchReturn.get(0);
        if (exactMatchIndexInFirstPermutation >= 0) {
            log.debug("Found an exact match in the UNSAT cache!");
            return UNSATCache.get(0).subList(exactMatchIndexInFirstPermutation, exactMatchIndexInFirstPermutation + 1);
        } else {
            final List<Integer> insertionIndices = binarySearchReturn.stream().map(i -> -(i + 1)).collect(toImmutableList());
            log.debug("No exact match found. Insertion indices for the various permutations are {}", insertionIndices);
            final int bestInsertionPoint = Collections.min(insertionIndices);
            final int bestPermutation = insertionIndices.indexOf(bestInsertionPoint);
            final ImmutableList<ContainmentCacheUNSATEntry> potentialSubsets = UNSATCache.get(bestPermutation).subList(0, bestInsertionPoint);
            log.debug("Returning a list of " + potentialSubsets.size() + " entries to search through");
            return potentialSubsets;
        }
    }

    // true if a is a superset of b
    private boolean isSupersetOrEqualToByStations(final BitSet a, final BitSet b) {
        return b.stream().allMatch(a::get);
    }

    // true if a is a subset of b
    private boolean isSubsetOrEqualToByStations(final BitSet a, final BitSet b) {
        return a.stream().allMatch(b::get);
    }

    public ContainmentCacheUNSATResult proveUNSATBySubset(final StationPackingInstance aInstance) {
        // convert instance to bit set representation
        final BitSet bitSet = CacheUtils.toBitSet(aInstance);
        // try to narrow down the entries we have to search by only looking at subsets
        final Collection<ContainmentCacheUNSATEntry> containmentCacheUNSATEntries = smallSetSmallerThanOrEqualTo(bitSet);
        return containmentCacheUNSATEntries.stream()
                /*
                 * The entry's stations should be a subset of the query's stations (so as to be less constrained)
                 * and each station in the entry must have larger than or equal to the corresponding station domain in the target (so as to be less constrained)
                 */
                .filter(entry -> isSubsetOrEqualToByStations(entry.getBitSet(), bitSet) && isSupersetOrEqualToByDomains(entry.getDomains(), aInstance.getDomains()))
                .map(entry -> new ContainmentCacheUNSATResult(entry.getKey()))
                .findAny()
                .orElse(ContainmentCacheUNSATResult.failure());
    }

    // true if a's domain is a superset of b's domain
    private boolean isSupersetOrEqualToByDomains(Map<Station, Set<Integer>> a, Map<Station, Set<Integer>> b) {
        return b.entrySet().stream().allMatch(entry -> {
            final Set<Integer> integers = a.get(entry.getKey());
            return integers != null && integers.containsAll(entry.getValue());
        });
    }

    public ContainmentCacheSATResult proveSATBySuperset(final StationPackingInstance aInstance) {
        // convert instance to bit set representation
        final BitSet bitSet = CacheUtils.toBitSet(aInstance);
        // try to narrow down the entries we have to search by only looking at supersets
        final Collection<ContainmentCacheSATEntry> containmentCacheSATEntries = smallSetLargerThanOrEqualTo(bitSet);
        return containmentCacheSATEntries.stream()
                /**
                 * The entry must contain at least every station in the query in order to provide a solution (hence superset)
                 * The entry should also be a solution to the problem, which it will be as long as the solution can project onto the query's domains since they come from the set of interference constraints
                 */
                .filter(entry -> isSupersetOrEqualToByStations(entry.getBitSet(), bitSet) && entry.isSolutionTo(aInstance))
                .map(entry -> new ContainmentCacheSATResult(entry.getAssignmentChannelToStation(), entry.getKey()))
                .findAny()
                .orElse(ContainmentCacheSATResult.failure());
    }

    /**
     * A comparator that compares bitsets according to an ordering specified by permutation such that the least
     * significant digit is the first number in the permutation)
     */
    private static class PermutableBitSetComparator implements Comparator<BitSet> {

        final int[] permutation;

        public PermutableBitSetComparator(int[] permutation) {
            this.permutation = permutation;
        }

        @Override
        public int compare(BitSet bs1, BitSet bs2) {
            for (int i = permutation.length - 1; i >= 0; i--) {
                int index = permutation[i];
                boolean b1 = bs1.get(index);
                boolean b2 = bs2.get(index);
                if (b1 && !b2) {
                    return 1;
                } else if (!b1 && b2) {
                    return -1;
                }
            }
            return 0;
        }
    }

}