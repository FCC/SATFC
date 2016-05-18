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
package ca.ubc.cs.beta.stationpacking.cache;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.utils.CacheUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        final CacheUtils.ParsedKey parsedKey = CacheUtils.parseKey(key);
        return new CacheCoordinate(parsedKey.getDomainHash(), parsedKey.getInterferenceHash());
    }

    // create a redis key from a coordinate, a result, and an instance
    public String toKey(SATResult result, long num) {
        Preconditions.checkArgument(result.equals(SATResult.SAT) || result.equals(SATResult.UNSAT));
        return Joiner.on(":").join(ImmutableList.of("SATFC", result, domainHash, interferenceHash, num));
    }

}
