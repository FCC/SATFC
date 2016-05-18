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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.PostConstruct;

import ca.ubc.cs.beta.stationpacking.cache.containment.transformer.ICacheEntryTransformer;
import ca.ubc.cs.beta.stationpacking.cache.containment.transformer.InstanceAndResult;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import com.google.common.collect.Maps;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.Timer;
import com.google.common.collect.Queues;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.ICacheEntryFilter;
import ca.ubc.cs.beta.stationpacking.cache.ICacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATResult;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy.ContainmentCacheRequest;
import ca.ubc.cs.beta.stationpacking.webapp.parameters.SATFCServerParameters;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/v1/cache")
public class ContainmentCacheController {

    private final String JSON_CONTENT = "application/json";

    @Autowired
    ICacheLocator containmentCacheLocator;

    @Autowired
    RedisCacher cacher;

    @Autowired
    SATFCServerParameters parameters;

    @Autowired
    ICacheEntryFilter cacheEntryFilter;

    @Autowired
    ICacheEntryTransformer cacheEntryTransformer;

    @Autowired
    DataManager dataManager;

    // Metrics
    @Autowired
    MetricRegistry registry;
    private Meter cacheAdditions;
    private Meter satCacheHits;
    private Timer satCacheTimer;
    private Meter unsatCacheHits;
    private Timer unsatCacheTimer;

    private volatile Map<Integer, Set<Station>> lastCachedAssignment = new HashMap<>();

    private final Queue<ContainmentCacheRequest> pendingCacheAdditions = Queues.newArrayDeque();

    @PostConstruct
    void init() {
        cacheAdditions = registry.meter("cache.sat.additions");
        satCacheHits = registry.meter("cache.sat.hits");
        satCacheTimer = registry.timer("cache.sat.timer");
        unsatCacheHits = registry.meter("cache.unsat.hits");
        unsatCacheTimer = registry.timer("cache.unsat.timer");
        registry.register("cache.sat.hitrate.fifteenminute", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(satCacheHits.getFifteenMinuteRate(), satCacheTimer.getFifteenMinuteRate());
            }
        });
        registry.register("cache.unsat.hitrate.fifteenminute", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(unsatCacheHits.getFifteenMinuteRate(), unsatCacheTimer.getFifteenMinuteRate());
            }
        });
        registry.register("cache.sat.hitrate.overall", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(satCacheHits.getCount(), satCacheTimer.getCount());
            }
        });
        registry.register("cache.unsat.hitrate.overall", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(unsatCacheHits.getCount(), unsatCacheTimer.getCount());
            }
        });
    }

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
        final Timer.Context context = satCacheTimer.time();
        try {
            final StationPackingInstance instance = request.getInstance();
            final String description = instance.hasName() ? instance.getName() : instance.getInfo();
            log.info("Querying the SAT cache with coordinate {} for entry {}", request.getCoordinate(), description);
            final ISatisfiabilityCache cache = containmentCacheLocator.locate(request.getCoordinate());
            final ContainmentCacheSATResult containmentCacheSATResult;
            if (parameters.getBadsets() != null) {
                final Map<String, Set<String>> badsets = parameters.getBadsets();
                final String auction = instance.getAuction();
                containmentCacheSATResult = cache.proveSATBySuperset(instance, c -> {
                    if (c.getAuction() != null && auction != null) {
                        return !badsets.get(auction).contains(c.getAuction());
                    }
                    return true;
                });
            } else if (parameters.isExcludeSameAuction()) {
                final String auction = instance.getAuction();
                containmentCacheSATResult = cache.proveSATBySuperset(instance, c -> {
                    if (c.getAuction() != null && auction != null) {
                        return !c.getAuction().equals(auction);
                    }
                    return true;
                });
            } else {
                containmentCacheSATResult = cache.proveSATBySuperset(instance);
            }
            if (containmentCacheSATResult.isValid()) {
                log.info("Query for SAT cache with coordinate {} for entry {} is a hit", request.getCoordinate(), description);
                satCacheHits.mark();
            } else {
                log.info("Query for SAT cache with coordinate {} for entry {} is a miss", request.getCoordinate(), description);
            }
            return containmentCacheSATResult;
        } finally {
            context.stop();
        }
    }


    // note that while this is conceptually a GET request, the fact that we need to send json means that its simpler to achieve as a POST
    @RequestMapping(value = "/query/UNSAT", method = RequestMethod.POST, produces = JSON_CONTENT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ContainmentCacheUNSATResult lookupUNSAT(
            @RequestBody final ContainmentCacheRequest request
    ) {
        final Timer.Context context = unsatCacheTimer.time();
        try {
            final StationPackingInstance instance = request.getInstance();
            final String description = instance.hasName() ? instance.getName() : instance.getInfo();
            log.info("Querying the UNSAT cache with coordinate {} for entry {}", request.getCoordinate(), description);
            final ISatisfiabilityCache cache = containmentCacheLocator.locate(request.getCoordinate());
            final ContainmentCacheUNSATResult result = cache.proveUNSATBySubset(instance);
            if (result.isValid()) {
                log.info("Query for UNSAT cache with coordinate {} for entry {} is a hit", request.getCoordinate(), description);
                unsatCacheHits.mark();
            } else {
                log.info("Query for UNSAT cache with coordinate {} for entry {} is a miss", request.getCoordinate(), description);
            }
            return result;
        } finally {
            context.stop();
        }
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void cache(
            @RequestBody final ContainmentCacheRequest request
    ) {
        // Just dump the entry and return - we don't want to delay the SATFC thread
        pendingCacheAdditions.add(request);
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void addCacheEntries() {
        log.debug("Waking up to check list of potential cache additions");
        while (!pendingCacheAdditions.isEmpty()) {
            final ContainmentCacheRequest request = pendingCacheAdditions.poll();
            final SolverResult result = request.getResult();
            if ((result.getResult().equals(SATResult.UNSAT) && parameters.isSkipUNSAT()) || result.getResult().equals(SATResult.SAT) && parameters.isSkipSAT()) {
                continue;
            }
            final StationPackingInstance instance = request.getInstance();
            final String description = instance.hasName() ? instance.getName() : instance.getInfo();

            final ISatisfiabilityCache cache = containmentCacheLocator.locate(request.getCoordinate());

            final InstanceAndResult transformedInstanceAndResult = cacheEntryTransformer.transform(instance, result);
            if (transformedInstanceAndResult != null) {
                final StationPackingInstance transformedInstance = transformedInstanceAndResult.getInstance();
                final SolverResult transformedResult = transformedInstanceAndResult.getResult();

                if (cacheEntryFilter.shouldCache(request.getCoordinate(), transformedInstance, transformedResult)) {
                    final String key;
                    if (result.getResult().equals(SATResult.SAT)) {
                        final ContainmentCacheSATEntry entry = new ContainmentCacheSATEntry(transformedResult.getAssignment(), cache.getPermutation());
                        key = cacher.cacheResult(request.getCoordinate(), entry, transformedInstance.hasName() ? transformedInstance.getName() : null);
                        entry.setKey(key);
                        cache.add(entry);
                        lastCachedAssignment = transformedResult.getAssignment();
                    } else if (result.getResult().equals(SATResult.UNSAT)) {
                        final ContainmentCacheUNSATEntry entry = new ContainmentCacheUNSATEntry(transformedInstance.getDomains(), cache.getPermutation());
                        cache.add(entry);
                        key = cacher.cacheResult(request.getCoordinate(), entry, transformedInstance.hasName() ? transformedInstance.getName() : null);
                        entry.setKey(key);
                    } else {
                        throw new IllegalStateException("Tried adding a result that was neither SAT or UNSAT");
                    }
                    log.info("Adding entry to the cache with coordinate {} with key {}. Entry {}", request.getCoordinate(), key, description);
                    // add to permanent storage
                    cacheAdditions.mark();
                } else {
                    log.info("Not adding entry {} to cache {}. No new info", request.getCoordinate(), description);
                }
            }
        }
        log.debug("Done checking potential cache additions");
    }

    @RequestMapping(value = "/filterSAT", method = RequestMethod.POST)
    @ResponseBody
    public void filterSATCache(@RequestParam(value = "strong", required = false, defaultValue = "true") boolean strong) {
    	containmentCacheLocator.getCoordinates().forEach(cacheCoordinate -> {
            log.info("Finding SAT entries to be filtered at cacheCoordinate {} ({})", cacheCoordinate, strong);
            final ISatisfiabilityCache cache = containmentCacheLocator.locate(cacheCoordinate);
            List<ContainmentCacheSATEntry> SATPrunables = cache.filterSAT(dataManager.getData(cacheCoordinate).getStationManager(), strong);
            log.info("Pruning {} SAT entries from Redis", SATPrunables.size());
            cacher.deleteSATCollection(SATPrunables);
        });
        log.info("Filter completed");
    }

    @RequestMapping(value = "/filterUNSAT", method = RequestMethod.POST)
    @ResponseBody
    public void filterUNSATCache() {
        containmentCacheLocator.getCoordinates().forEach(cacheCoordinate -> {
            log.info("Finding UNSAT entries to be filtered at cacheCoordinate {}", cacheCoordinate);
            final ISatisfiabilityCache cache = containmentCacheLocator.locate(cacheCoordinate);
            final List<ContainmentCacheUNSATEntry> UNSATPrunables = cache.filterUNSAT();
            log.info("Pruning {} UNSAT entries from Redis", UNSATPrunables.size());
            cacher.deleteUNSATCollection(UNSATPrunables);
        });
        log.info("Filter completed");
    }


    /**
     * Return the last solved SAT problem you know about
     */
    @RequestMapping(value = "/previousAssignment", method = RequestMethod.GET)
    @ResponseBody
    public Map<Integer, Set<Station>> getPreviousAssignment() {
        log.info("Returning the last cached SAT assignment");
        return lastCachedAssignment;
    }

    /**
     * Return the number of entries waiting to be filtered into the cache
     */
    @RequestMapping(value = "/n_pending_additions", method = RequestMethod.GET)
    @ResponseBody
    public int getNumFiltering() {
        return pendingCacheAdditions.size();
    }

}