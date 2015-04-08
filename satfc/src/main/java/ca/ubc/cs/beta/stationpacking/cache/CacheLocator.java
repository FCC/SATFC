package ca.ubc.cs.beta.stationpacking.cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import ca.ubc.cs.beta.stationpacking.cache.ICacher.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher.ContainmentCacheInitData;

/**
 * Created by newmanne on 25/03/15.
 */
@Slf4j
public class CacheLocator implements ICacheLocator, ApplicationListener<ContextRefreshedEvent> {

    private final RedisCacher cacher;
    private final ConcurrentMap<CacheCoordinate, ContainmentCache> caches;

    public CacheLocator(RedisCacher cacher) {
        this.cacher = cacher;
        caches = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<ContainmentCache> locate(CacheCoordinate coordinate) {
        return Optional.ofNullable(caches.get(coordinate));
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Beginning to init caches");
        final ContainmentCacheInitData containmentCacheInitData = cacher.getContainmentCacheInitData();
        containmentCacheInitData.getCaches().forEach(cacheCoordinate -> {
            caches.put(cacheCoordinate, new ContainmentCache(containmentCacheInitData.getSATResults().get(cacheCoordinate), containmentCacheInitData.getUNSATResults().get(cacheCoordinate)));
        });

    }
}
