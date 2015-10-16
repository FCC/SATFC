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
package ca.ubc.cs.beta.stationpacking.webapp.rest;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.CacherProxy.ContainmentCacheCacheRequest;
import ca.ubc.cs.beta.stationpacking.cache.ICacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy.ContainmentCacheRequest;

@Controller
@Slf4j
@RequestMapping("/v1/cache")
public class ContainmentCacheController {

    private final String JSON_CONTENT = "application/json";

    @Autowired
    ICacheLocator containmentCacheLocator;

    @Autowired
    RedisCacher cacher;

    @ExceptionHandler(ClientAbortException.class)
    void clientAbortException() {
        // Nothing to do
        log.trace("ClientAbortException ignored because it is expected behavior");
    }

    // note that while this is conceptually a GET request, the fact that we need to send json means that its simpler to achieve as a POST
    @RequestMapping(value = "/query/SAT", method = RequestMethod.POST, produces = JSON_CONTENT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ContainmentCacheSATResult lookupSAT(
            @RequestBody final ContainmentCacheRequest request
    ) {
        final StationPackingInstance instance = request.getInstance();
        final String description = instance.getMetadata().containsKey(StationPackingInstance.NAME_KEY) ? instance.getName() : instance.getInfo();
        log.info("Querying the SAT cache with coordinate " + request.getCoordinate() + " for entry " + description);
        final ISatisfiabilityCache cache = containmentCacheLocator.locate(request.getCoordinate());
        return cache.proveSATBySuperset(instance);
    }

    // note that while this is conceptually a GET request, the fact that we need to send json means that its simpler to achieve as a POST
    @RequestMapping(value = "/query/UNSAT", method = RequestMethod.POST, produces = JSON_CONTENT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ContainmentCacheUNSATResult lookupUNSAT(
            @RequestBody final ContainmentCacheRequest request
    ) {
        final StationPackingInstance instance = request.getInstance();
        final String description = instance.getMetadata().containsKey(StationPackingInstance.NAME_KEY) ? instance.getName() : instance.getInfo();
        log.info("Querying the UNSAT cache with coordinate " + request.getCoordinate() + " for entry " + description);
        final ISatisfiabilityCache cache = containmentCacheLocator.locate(request.getCoordinate());
        return cache.proveUNSATBySubset(instance);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void cache(
            @RequestBody final ContainmentCacheCacheRequest request
    ) {
        final StationPackingInstance instance = request.getInstance();
        final String description = instance.getMetadata().containsKey(StationPackingInstance.NAME_KEY) ? instance.getName() : instance.getInfo();
        log.info("Adding entry to the cache with coordinate " + request.getCoordinate() + ". Entry: " + description);

        // add to redis
        final String key = cacher.cacheResult(request.getCoordinate(), instance, request.getResult());
        final ISatisfiabilityCache cache = containmentCacheLocator.locate(request.getCoordinate());
        cache.add(instance, request.getResult(), key);
    }

    @RequestMapping(value = "/filter", method = RequestMethod.POST)
    @ResponseBody
    public void filterCache() {
    	containmentCacheLocator.getCoordinates().forEach(cacheCoordinate -> {
            log.info("Finding SAT entries to be filted at cacheCoordinate " + cacheCoordinate);
            final ISatisfiabilityCache cache = containmentCacheLocator.locate(cacheCoordinate);
            List<ContainmentCacheSATEntry> SATPrunables = cache.filterSAT();
            log.info("Pruning " + SATPrunables.size() + " SAT entries from Redis");
            cacher.deleteSATCollection(SATPrunables);

            log.info("Finding UNSAT entries to be filted at cacheCoordinate " + cacheCoordinate);
            List<ContainmentCacheUNSATEntry> UNSATPrunables = cache.filterUNSAT();
            log.info("Pruning " + UNSATPrunables.size() + " UNSAT entries from Redis");
            cacher.deleteUNSATCollection(UNSATPrunables);

            log.info("Filter completed");
        });
    }

}