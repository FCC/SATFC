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

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

/**
 * Created by newmanne on 1/25/15.
 */
public interface ICacher {

    void cacheResult(CacheCoordinate cacheCoordinate, StationPackingInstance instance, SolverResult result);

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
