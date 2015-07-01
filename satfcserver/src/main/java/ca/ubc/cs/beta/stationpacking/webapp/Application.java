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

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import redis.clients.jedis.JedisShardInfo;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.stationpacking.cache.CacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.ICacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.ISatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.SatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import ca.ubc.cs.beta.stationpacking.webapp.parameters.SATFCServerParameters;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

/**
 * Created by newmanne on 23/03/15.
 */
@Slf4j
@SpringBootApplication
public class Application {

    private final static SATFCServerParameters parameters = new SATFCServerParameters();

    public static void main(String[] args) {
        // Even though spring has its own parameter parsing, JComamnder gives tidier error messages
        JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters);
        parameters.validate();
        log.info("Using the following command line parameters " + System.lineSeparator() + parameters.toString());
        SpringApplication.run(Application.class, args);
    }

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        final MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        final ObjectMapper mapper = JSONUtils.getMapper();
        mappingJacksonHttpMessageConverter.setObjectMapper(mapper);
        return mappingJacksonHttpMessageConverter;
    }

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
        final SATFCServerParameters satfcServerParameters = satfcServerParameters();
        final int timeout = (int) TimeUnit.SECONDS.toMillis(60);
        return new JedisConnectionFactory(new JedisShardInfo(satfcServerParameters.getRedisURL(), satfcServerParameters.getRedisPort(), timeout));
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
        return new CacheLocator(satisfiabilityCacheFactory());
    }

    @Bean
    ISatisfiabilityCacheFactory satisfiabilityCacheFactory() {
        return new SatisfiabilityCacheFactory();
    }

    @Bean
    DataManager dataManager() {
        return new DataManager();
    }

    @Bean SATFCServerParameters satfcServerParameters() {
        return parameters;
    }

}
