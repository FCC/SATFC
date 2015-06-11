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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import ca.ubc.cs.beta.stationpacking.cache.RedisCacher.ContainmentCacheInitData;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.containmentcache.ISatisfiabilityCache;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Created by newmanne on 25/03/15.
 */
@Slf4j
@ThreadSafe
public class CacheLocator implements ICacheLocator, ApplicationListener<ContextRefreshedEvent> {

    private final RedisCacher cacher;
    private final Map<CacheCoordinate, ISatisfiabilityCache> caches;
    private final ReadWriteLock readWriteLock;
    private final ISatisfiabilityCacheFactory cacheFactory;

    public CacheLocator(RedisCacher cacher, ISatisfiabilityCacheFactory cacheFactory) {
        this.cacher = cacher;
        this.cacheFactory = cacheFactory;
        caches = new HashMap<>();
        readWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public Optional<ISatisfiabilityCache> locate(CacheCoordinate coordinate) {
        readWriteLock.readLock().lock();
        try {
            return Optional.ofNullable(caches.get(coordinate));
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void addCache(CacheCoordinate coordinate) {
        readWriteLock.writeLock().lock();
        try {
            // Perform this check to make sure that no one added a cache already when you were waiting to acquire the lock
            if (caches.get(coordinate) == null) {
                log.info("Adding a new empty containment cache for coordinate: " + coordinate);
                caches.put(coordinate, cacheFactory.create(ImmutableList.of(), ImmutableList.of()));
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    // We want this to happen after the context has been brought up (so the error messages aren't horrific)
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Beginning to init caches");
        final ContainmentCacheInitData containmentCacheInitData = cacher.getContainmentCacheInitData();
        containmentCacheInitData.getCaches().forEach(cacheCoordinate -> {
            final List<ContainmentCacheSATEntry> SATEntries = containmentCacheInitData.getSATResults().get(cacheCoordinate);
            final List<ContainmentCacheUNSATEntry> UNSATEntries = containmentCacheInitData.getUNSATResults().get(cacheCoordinate);
            ISatisfiabilityCache cache = cacheFactory.create(SATEntries, UNSATEntries);
            caches.put(cacheCoordinate, cache);
        });
    }

	@Override
	public Set<CacheCoordinate> getCoordinates() {
		return ImmutableSet.copyOf(caches.keySet());
	}
	
}
