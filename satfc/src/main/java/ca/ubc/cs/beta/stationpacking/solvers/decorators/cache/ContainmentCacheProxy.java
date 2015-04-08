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
package ca.ubc.cs.beta.stationpacking.solvers.decorators.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.ICacher.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.utils.CacheUtils;

/**
 * Created by newmanne on 01/03/15.
 */
@RequiredArgsConstructor
@Slf4j
public class ContainmentCacheProxy {

    private final RestTemplate restTemplate = CacheUtils.getRestTemplate();
    private final String baseServerURL;
    private final CacheCoordinate coordinate;

    /**
     * Object used to represent a cache lookup request
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContainmentCacheRequest {
        private StationPackingInstance instance;
        private CacheCoordinate coordinate;
    }

    public ContainmentCacheSATResult proveSATBySuperset(StationPackingInstance instance) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseServerURL + "/v1/cache/query/SAT");
        final ContainmentCacheRequest request = new ContainmentCacheRequest(instance, coordinate);
        final String uriString = builder.build().toUriString();
        log.debug("Making a SAT request to the cache server for instance " + instance.getName() + " " + uriString);
        return restTemplate.postForObject(uriString, request, ContainmentCacheSATResult.class);
    }

    public ContainmentCacheUNSATResult proveUNSATBySubset(StationPackingInstance instance) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseServerURL + "/v1/cache/query/UNSAT");
        final ContainmentCacheRequest request = new ContainmentCacheRequest(instance, coordinate);
        final String uriString = builder.build().toUriString();
        log.debug("Making an UNSAT request to the cache server for instance " + instance.getName() + " " + uriString);
        return restTemplate.postForObject(uriString, request, ContainmentCacheUNSATResult.class);
    }

}
