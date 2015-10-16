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
package ca.ubc.cs.beta.stationpacking.cache;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.SatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import containmentcache.IContainmentCache;
import containmentcache.ILockableContainmentCache;
import containmentcache.bitset.opt.MultiPermutationBitSetCache;
import containmentcache.bitset.opt.sortedset.redblacktree.RedBlackTree;
import containmentcache.decorators.BufferedThreadSafeCacheDecorator;
import containmentcache.util.PermutationUtils;

/**
* Created by newmanne on 22/04/15.
*/
@Slf4j
public class SatisfiabilityCacheFactory implements ISatisfiabilityCacheFactory {

    private static final int SAT_BUFFER_SIZE = 100;
    private static final int UNSAT_BUFFER_SIZE = 3;
    private final int numPermutations;
    private final long seed;

    public SatisfiabilityCacheFactory(int numPermutations, long seed) {
        Preconditions.checkArgument(numPermutations > 0, "Need at least one permutation!");
        this.numPermutations = numPermutations;
        this.seed = seed;
    }

    @Override
    public ISatisfiabilityCache create(ImmutableBiMap<Station, Integer> permutation) {
        // 1) Create other permutations, if any
        final List<BiMap<Station, Integer>> permutations;
        if (numPermutations > 1) {
            permutations = PermutationUtils.makeNPermutations(permutation, seed, numPermutations - 1);
        } else {
            permutations = ImmutableList.of();
        }

        // 2) Create the actual caches
        final IContainmentCache<Station, ContainmentCacheSATEntry> undecoratedSATCache = new MultiPermutationBitSetCache<>(permutation, permutations, RedBlackTree::new);
        final ILockableContainmentCache<Station, ContainmentCacheSATEntry> SATCache = BufferedThreadSafeCacheDecorator.makeBufferedThreadSafe(undecoratedSATCache, SAT_BUFFER_SIZE);
        final IContainmentCache<Station, ContainmentCacheUNSATEntry> undecoratedUNSATCache = new MultiPermutationBitSetCache<>(permutation, permutations, RedBlackTree::new);
        final ILockableContainmentCache<Station, ContainmentCacheUNSATEntry> UNSATCache = BufferedThreadSafeCacheDecorator.makeBufferedThreadSafe(undecoratedUNSATCache, UNSAT_BUFFER_SIZE);
        return new SatisfiabilityCache(permutation, SATCache, UNSATCache);
    }
}