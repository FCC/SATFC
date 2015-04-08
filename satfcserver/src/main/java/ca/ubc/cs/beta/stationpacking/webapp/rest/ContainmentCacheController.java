/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfcserver.
 *
 * satfcserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfcserver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfcserver.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.webapp.rest;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.CacherProxy.ContainmentCacheCacheRequest;
import ca.ubc.cs.beta.stationpacking.cache.ContainmentCache;
import ca.ubc.cs.beta.stationpacking.cache.ICacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy.ContainmentCacheRequest;

@Controller
@Slf4j
@RequestMapping("/v1/cache")
public class ContainmentCacheController extends AbstractController {

    @Autowired
    ICacheLocator containmentCache;

    @Autowired
    RedisCacher cacher;

    // note that while this is conceptually a GET request, the fact that we need to send json means that its simpler to achieve as a POST
    @RequestMapping(value = "/query/SAT", method = RequestMethod.POST, produces = JSON_CONTENT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ContainmentCacheSATResult lookupSAT(
            @RequestBody final ContainmentCacheRequest request
    ) {
        final StationPackingInstance instance = request.getInstance();
        final String description = instance.getMetadata().containsKey(StationPackingInstance.NAME_KEY) ? instance.getName() : instance.getInfo();
        log.info("Querying the SAT cache for entry " + description);
        final Optional<ContainmentCache> cache = containmentCache.locate(request.getCoordinate());
        if (cache.isPresent()) {
            return cache.get().proveSATBySuperset(instance);
        } else {
            return ContainmentCacheSATResult.failure();
        }
    }

    // note that while this is conceptually a GET request, the fact that we need to send json means that its simpler to achieve as a POST
    @RequestMapping(value = "/query/UNSAT", method = RequestMethod.POST, produces = JSON_CONTENT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ContainmentCacheUNSATResult lookupUNSAT(
            @RequestBody final ContainmentCacheRequest request
    ) {
        final StationPackingInstance instance = request.getInstance();
        final String description = instance.getMetadata().containsKey(StationPackingInstance.NAME_KEY) ? instance.getName() : instance.getInfo();
        log.info("Querying the UNSAT cache for entry " + description);
        final Optional<ContainmentCache> cache = containmentCache.locate(request.getCoordinate());
        if (cache.isPresent()) {
            return cache.get().proveUNSATBySubset(instance);
        } else {
            return ContainmentCacheUNSATResult.failure();
        }
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void cache(
            @RequestBody final ContainmentCacheCacheRequest request
    ) {
        cacher.cacheResult(request.getCoordinate(), request.getInstance(), request.getResult());
    }

}