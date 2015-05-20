package ca.ubc.cs.beta.stationpacking;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.ICacher;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by newmanne on 24/04/15.
 */
@Slf4j
public class ConvertCacheTest {

    @JsonDeserialize(using = CacheEntry.CacheEntryDeserializer.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Data
    public static  class CacheEntry {

        private final SolverResult solverResult;
        private final Map<Station, Set<Integer>> domains;
        private final Date cacheDate;
        private final String name;

        public static class CacheEntryDeserializer extends JsonDeserializer<CacheEntry> {

            @Override
            public CacheEntry deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                final CacheEntryJson cacheEntryJson = jp.readValueAs(CacheEntryJson.class);
                final Map<Station, Set<Integer>> collect = cacheEntryJson.getDomains().entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> new Station(e.getKey()), Map.Entry::getValue));
                return new CacheEntry(cacheEntryJson.getSolverResult(), collect, cacheEntryJson.getCacheDate(), cacheEntryJson.getName());
            }

        }

        @Data
        private static class CacheEntryJson {
            private SolverResult solverResult;
            private Map<Integer, Set<Integer>> domains;
            private Date cacheDate;
            private String name;
        }

    }

    @Ignore
    @Test
    public void drive() {
    	Jedis jedisOldCache = new Jedis("cersei");
        Jedis jedisNewCache = new Jedis("cersei", 7777);
        final String domainHash= "1fa85deb";
        final String interferenceHash = "0683bb7d";
        final Set<String> keys = jedisOldCache.keys("*");
        log.info("Found " + keys.size() + " keys");
        jedisOldCache.keys("*").forEach(key -> {
            final CacheEntry cacheEntry = JSONUtils.toObject(jedisOldCache.get(key), CacheEntry.class);
            final Map<String, Object> metadata = new HashMap<>();
            metadata.put(StationPackingInstance.NAME_KEY, cacheEntry.getName() != null ? cacheEntry.getName() : "UNTITLED");
            metadata.put(StationPackingInstance.CACHE_DATE_KEY, new Date());
            final String instanceHash = Iterables.getLast(Splitter.on(":").split(key));
            // convert
            final String json;
            if (cacheEntry.getSolverResult().getResult().equals(SATResult.SAT)) {
                json = JSONUtils.toString(new ICacher.SATCacheEntry(metadata, cacheEntry.getSolverResult().getAssignment()));
            } else {
                json = JSONUtils.toString(new ICacher.UNSATCacheEntry(metadata, cacheEntry.getDomains()));
            }
            // cache
            final String newKey = Joiner.on(":").join(ImmutableList.of("SATFC", cacheEntry.getSolverResult().getResult(), domainHash, interferenceHash, instanceHash));
            log.info("Key: " + key + " to key: " + newKey);
            jedisNewCache.set(newKey, json);
        });
    }

}