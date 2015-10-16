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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher.ContainmentCacheInitData;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;

import containmentcache.util.PermutationUtils;

/**
 * Created by newmanne on 25/03/15.
 */
@Slf4j
@ThreadSafe
public class CacheLocator implements ICacheLocator, ApplicationListener<ContextRefreshedEvent> {

    private final Map<CacheCoordinate, ISatisfiabilityCache> caches;
    private final ISatisfiabilityCacheFactory cacheFactory;

    public CacheLocator(ISatisfiabilityCacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
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

        final Map<CacheCoordinate, ManagerBundle> coordinateToBundle = new HashMap<>();
        final Map<CacheCoordinate, ImmutableBiMap<Station, Integer>> coordinateToPermutation = new HashMap<>();

        // Set up the data manager
        final String constraintFolder = context.getEnvironment().getRequiredProperty("constraint.folder");
        log.info("Looking in " + constraintFolder + " for station configuration folders");
        final File[] stationConfigurationFolders = new File(constraintFolder).listFiles(File::isDirectory);
        log.info("Found " + stationConfigurationFolders.length + " station configuration folders");
        Arrays.stream(stationConfigurationFolders).forEach(folder -> {
            try {
                final String path = folder.getAbsolutePath();
                log.info("Adding data for station configuration folder " + path);
                dataManager.addData(folder.getAbsolutePath());
                // add cache coordinate to map
                final ManagerBundle bundle = dataManager.getData(folder.getAbsolutePath());
                log.info("Folder " + folder.getAbsolutePath() + " corresponds to coordinate " + bundle.getCacheCoordinate());
                coordinateToBundle.put(bundle.getCacheCoordinate(), bundle);
                
                final ImmutableBiMap<Station, Integer> permutation = PermutationUtils.makePermutation(bundle.getStationManager().getStations());
                coordinateToPermutation.put(bundle.getCacheCoordinate(), permutation);

            } catch (FileNotFoundException e) {
                throw new IllegalStateException(folder.getAbsolutePath() + " is not a valid station configuration folder (missing Domain or Interference files?)", e);
            }
        });

        log.info("Beginning to init caches");
        final ContainmentCacheInitData containmentCacheInitData = cacher.getContainmentCacheInitData(coordinateToPermutation);
        coordinateToBundle.keySet().forEach(cacheCoordinate -> {
            final ISatisfiabilityCache cache = cacheFactory.create(coordinateToPermutation.get(cacheCoordinate));
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
