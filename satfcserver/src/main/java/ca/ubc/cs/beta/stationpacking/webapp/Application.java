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

import java.io.File;
import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import redis.clients.jedis.JedisShardInfo;
import ca.ubc.cs.beta.stationpacking.cache.CacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.ICacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.ISatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.SatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

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
    @Value("${stations.file:}")
    String stationsFile;

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        final MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        final ObjectMapper mapper = JSONUtils.getMapper();
        mappingJacksonHttpMessageConverter.setObjectMapper(mapper);
        return mappingJacksonHttpMessageConverter;
    }

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
        log.info("Using the following redis information: host " + redisURL + ", port: " + redisPort);
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
        return new CacheLocator(cacher(), satisfiabilityCacheFactory());
    }

    @Bean
    ISatisfiabilityCacheFactory satisfiabilityCacheFactory() {
        final List<String> stationIds;
        // By default, just load the universe of stations from our internal file; if the user specified somewhere else, load from there
        try {
            if (stationsFile.isEmpty()) {
                log.info("Reading station universe file from internal resources");
                stationIds = Resources.readLines(Resources.getResource("universe.txt"), Charsets.UTF_8);
            } else {
                log.info("Reading station universe file from " + stationsFile);
                stationIds = Files.readLines(new File(stationsFile), Charsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load stations file " + stationsFile + " that lists what stations are in the universe");
        }
        return new SatisfiabilityCacheFactory(stationIds);
    }

}
