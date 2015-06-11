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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * This class determines which cache is accessed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheCoordinate {

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
