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

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.ICacher;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy.ContainmentCacheRequest;
import ca.ubc.cs.beta.stationpacking.utils.CacheUtils;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by newmanne on 22/03/15.
 */
@Slf4j
public class CacherProxy implements ICacher {

    private final RestTemplate restTemplate = CacheUtils.getRestTemplate();
    private final String baseServerURL;
    private final CacheCoordinate coordinate;

    public CacherProxy(String serverURL, CacheCoordinate cacheCoordinate) {
        this.baseServerURL = serverURL;
        this.coordinate = cacheCoordinate;
    }

    @Override
    public void cacheResult(CacheCoordinate cacheCoordinate, StationPackingInstance instance, SolverResult result) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseServerURL + "/v1/cache");
        final ContainmentCacheCacheRequest request = new ContainmentCacheCacheRequest(instance, coordinate, result);
        restTemplate.postForLocation(builder.build().toUriString(), request);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContainmentCacheCacheRequest {
        private StationPackingInstance instance;
        private CacheCoordinate coordinate;
        private SolverResult result;

    }


}
