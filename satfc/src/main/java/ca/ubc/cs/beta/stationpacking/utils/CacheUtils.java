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
package ca.ubc.cs.beta.stationpacking.utils;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by newmanne on 19/03/15.
 */
public class CacheUtils {

    public static BitSet toBitSet(Map<Integer, Set<Station>> answer, Map<Station, Integer> permutation) {
        final BitSet bitSet = new BitSet();
        answer.values().stream().forEach(stations -> stations.forEach(station -> bitSet.set(permutation.get(station))));
        return bitSet;
    }
    
    public static CloseableHttpAsyncClient createHttpClient() {
        final CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
        client.start();
        return client;
    }

    public static ParsedKey parseKey(String key) {
        final List<String> strings = Splitter.on(":").splitToList(key);
        Preconditions.checkState(strings.size() == 5, "Key %s not of expected cache key format SATFC:SAT:*:*:* or SATFC:UNSAT:*:*:*", key);
        return new ParsedKey(Long.parseLong(strings.get(4)), SATResult.valueOf(strings.get(1)), strings.get(2), strings.get(3));
    }


    @Data
    @AllArgsConstructor
    public static class ParsedKey {

        private final long num;
        private final SATResult result;
        private final String domainHash;
        private final String interferenceHash;
    }

}