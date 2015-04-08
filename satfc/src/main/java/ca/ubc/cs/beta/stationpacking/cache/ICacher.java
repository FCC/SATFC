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

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * Created by newmanne on 1/25/15.
 */
public interface ICacher {

    void cacheResult(CacheCoordinate cacheCoordinate, StationPackingInstance instance, SolverResult result);

    /**
     * This class determines which cache is accessed
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheCoordinate {

        private String domainHash;
        private String interferenceHash;

        // transform a redis key into a cache coordinate
        public static CacheCoordinate fromKey(String key) {
            final List<String> strings = Splitter.on(":").splitToList(key);
            return new CacheCoordinate(strings.get(2), strings.get(3));
        }

        // create a redis key from a coordinate, a result, and an instance
        public String toKey(SATResult result, StationPackingInstance instance) {
            Preconditions.checkArgument(result.equals(SATResult.SAT) || result.equals(SATResult.UNSAT));
            return Joiner.on(":").join(ImmutableList.of("SATFC", result, domainHash, interferenceHash, StationPackingInstanceHasher.hash(instance)));
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SATCacheEntry {
        private Map<String, Object> metadata;
        private Map<Integer, Set<Station>> assignment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UNSATCacheEntry {
        private Map<String, Object> metadata;
        Map<Station, Set<Integer>> domains;
    }

}
