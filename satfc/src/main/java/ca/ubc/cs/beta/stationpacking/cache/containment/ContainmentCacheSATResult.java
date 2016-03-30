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

import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import lombok.Data;

/**
* Created by newmanne on 25/03/15.
*/
@Data
public class ContainmentCacheSATResult {

    public ContainmentCacheSATResult() {
        valid = false;
    }

    public ContainmentCacheSATResult(Map<Integer, Set<Station>> result, String key) {
        this.key = key;
        this.result = result;
        this.valid = true;
    }

    private Map<Integer, Set<Station>> result;
    // the redis key of the problem whose solution "solves" this problem
    private String key;
    private boolean valid;

    // return an empty or failed result, that represents an error or that the problem was not solvable via the cache
    public static ContainmentCacheSATResult failure() {
        return new ContainmentCacheSATResult();
    }
}
