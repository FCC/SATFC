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

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;

/**
* Created by newmanne on 25/03/15.
*/
public class StationPackingInstanceHasher {

    // hashing function
    private static final HashFunction fHashFuction = Hashing.murmur3_128();

    public static HashCode hash(StationPackingInstance aInstance) {
        final HashCode hash = fHashFuction.newHasher()
                .putString(aInstance.toString(), Charsets.UTF_8)
                .hash();
        return hash;
    }

}
