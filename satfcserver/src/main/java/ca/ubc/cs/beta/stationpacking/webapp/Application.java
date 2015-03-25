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
package ca.ubc.cs.beta.stationpacking.webapp;

import ca.ubc.cs.beta.stationpacking.cache.ContainmentCache;
import ca.ubc.cs.beta.stationpacking.cache.ICacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.ICacher;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher.ContainmentCacheInitData;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.RedisHealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import redis.clients.jedis.JedisShardInfo;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by newmanne on 23/03/15.
 */
@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // These will be set by command line properties e.g. --redis.port=8080
    @Value("${redis.host:localhost}")
    String redisURL;
    @Value("${redis.port:6379}")
    int redisPort;

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        final MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        final ObjectMapper mapper = JSONUtils.getMapper();
        mappingJacksonHttpMessageConverter.setObjectMapper(mapper);
        return mappingJacksonHttpMessageConverter;
    }

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
        return new JedisConnectionFactory(new JedisShardInfo(redisURL, redisPort));
    }

    @Bean
    RedisCacher cacher() {
        return new RedisCacher(redisTemplate());
    }

    @Bean
    StringRedisTemplate redisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory());
    }

    @Bean
    ICacheLocator containmentCache() {
        final ContainmentCacheInitData subsetCacheData = cacher().getContainmentCacheInitData();
        final ConcurrentMap<ICacher.CacheCoordinate, ContainmentCache> caches = new ConcurrentHashMap<>();
        subsetCacheData.getCaches().forEach(cacheCoordinate -> {
            caches.put(cacheCoordinate, new ContainmentCache(subsetCacheData.getSATResults().get(cacheCoordinate), subsetCacheData.getUNSATResults().get(cacheCoordinate)));
        });
        return coordinate -> Optional.ofNullable(caches.get(coordinate));
    }


}
