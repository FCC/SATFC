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
package ca.ubc.cs.beta.stationpacking.webapp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.google.common.collect.ImmutableSet;

import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.ICacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.ISatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher.ContainmentCacheInitData;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.webapp.parameters.SATFCServerParameters;
import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;

/**
 * Created by newmanne on 25/03/15.
 */
@Slf4j
@ThreadSafe
public class CacheLocator implements ICacheLocator, ApplicationListener<ContextRefreshedEvent> {

    private final Map<CacheCoordinate, ISatisfiabilityCache> caches;
    private final ISatisfiabilityCacheFactory cacheFactory;
    private final SATFCServerParameters parameters;

    public CacheLocator(ISatisfiabilityCacheFactory cacheFactory, SATFCServerParameters parameters) {
        this.cacheFactory = cacheFactory;
        this.parameters = parameters;
        caches = new HashMap<>();
    }

    @Override
    public ISatisfiabilityCache locate(CacheCoordinate coordinate) {
        ISatisfiabilityCache cache = caches.get(coordinate);
        if (cache == null) {
            throw new IllegalStateException("No cache was made for coordinate " + coordinate + ". Was the corresponding station configuration folder present at server start up?");
        }
        return cache;
    }

    // We want this to happen after the context has been brought up (so the error messages aren't horrific)
    // Uses the context to pull out beans / command line arguments
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        final ApplicationContext context = event.getApplicationContext();
        final RedisCacher cacher = context.getBean(RedisCacher.class);
        final DataManager dataManager = context.getBean(DataManager.class);

        // Set up the data manager
        final String constraintFolder = parameters.getConstraintFolder();
        dataManager.loadMultipleConstraintSets(constraintFolder);

        log.info("Beginning to init caches");
        final ContainmentCacheInitData containmentCacheInitData = cacher.getContainmentCacheInitData(parameters.getCacheSizeLimit(), parameters.isSkipSAT(), parameters.isSkipUNSAT(), parameters.isValidateSAT());
        dataManager.getCoordinateToBundle().keySet().forEach(cacheCoordinate -> {
            final ISatisfiabilityCache cache = cacheFactory.create(dataManager.getData(cacheCoordinate).getPermutation());
            log.info("Cache created for coordinate " + cacheCoordinate);
            caches.put(cacheCoordinate, cache);
            if (containmentCacheInitData.getCaches().contains(cacheCoordinate)) {
                cache.addAllSAT(containmentCacheInitData.getSATResults().get(cacheCoordinate));
                cache.addAllUNSAT(containmentCacheInitData.getUNSATResults().get(cacheCoordinate));
            }
        });
    }

	@Override
	public Set<CacheCoordinate> getCoordinates() {
		return ImmutableSet.copyOf(caches.keySet());
	}
	
}
