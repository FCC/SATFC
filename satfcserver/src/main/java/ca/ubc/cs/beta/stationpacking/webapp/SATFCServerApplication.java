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

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.containment.transformer.ICacheEntryTransformer;
import ca.ubc.cs.beta.stationpacking.cache.containment.transformer.UHFRestrictionTransformer;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.ReflectionUtils;

import com.beust.jcommander.Parameter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.stationpacking.cache.ICacheEntryFilter;
import ca.ubc.cs.beta.stationpacking.cache.ICacheLocator;
import ca.ubc.cs.beta.stationpacking.cache.ISatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.cache.NewInfoEntryFilter;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.SatisfiabilityCacheFactory;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import ca.ubc.cs.beta.stationpacking.webapp.filters.GzipRequestFilter;
import ca.ubc.cs.beta.stationpacking.webapp.parameters.SATFCServerParameters;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.JedisShardInfo;

/**
 * Created by newmanne on 23/03/15.
 */
@Slf4j
@EnableScheduling
@SpringBootApplication
public class SATFCServerApplication {

    private final static SATFCServerParameters parameters = new SATFCServerParameters();

    public static void main(String[] args) {
        // Jcommander will throw an exception if it sees parameters it does not know about, but these are possibly spring boot commands
        final List<String> jcommanderArgs = new ArrayList<>();
        final List<String> jcommanderFields = Lists.newArrayList("--help");
        ReflectionUtils.doWithFields(SATFCServerParameters.class, field -> {
            final Parameter annotation = field.getAnnotation(Parameter.class);
            if (annotation != null) {
                Collections.addAll(jcommanderFields, annotation.names());
            }
        });
        for (String arg : args) {
            for (String jcommanderField : jcommanderFields) {
                final String[] split = arg.split("=");
                if (split.length > 0 && split[0].equals(jcommanderField)) {
                    jcommanderArgs.add(arg);
                    break;
                }
            }
        }
        // Even though spring has its own parameter parsing, JComamnder gives tidier error messages
        JCommanderHelper.parseCheckingForHelpAndVersion(jcommanderArgs.toArray(new String[jcommanderArgs.size()]), parameters);
        parameters.validate();
        log.info("Using the following command line parameters " + System.lineSeparator() + parameters.toString());
        SpringApplication.run(SATFCServerApplication.class, args);
    }

    @Autowired
    MetricRegistry registry;

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        final MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        final ObjectMapper mapper = JSONUtils.getMapper();
        mappingJacksonHttpMessageConverter.setObjectMapper(mapper);
        return mappingJacksonHttpMessageConverter;
    }

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
        return new JedisConnectionFactory(getShardInfo());
    }

    private JedisShardInfo getShardInfo() {
        final SATFCServerParameters satfcServerParameters = satfcServerParameters();
        final int timeout = (int) TimeUnit.SECONDS.toMillis(60);
        return new JedisShardInfo(satfcServerParameters.getRedisURL(), satfcServerParameters.getRedisPort(), timeout);
    }

    @Bean
    BinaryJedis binaryJedis() {
        return new BinaryJedis(getShardInfo());
    }

    @Bean
    RedisCacher cacher() {
        return new RedisCacher(dataManager(), redisTemplate(), binaryJedis());
    }

    @Bean
    StringRedisTemplate redisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory());
    }

    @Bean
    ICacheLocator containmentCacheLocator() {
        return new CacheLocator(satisfiabilityCacheFactory(), parameters);
    }

    @Bean
    ISatisfiabilityCacheFactory satisfiabilityCacheFactory() {
        final SATFCServerParameters satfcServerParameters = satfcServerParameters();
        return new SatisfiabilityCacheFactory(satfcServerParameters.getNumPermutations(), satfcServerParameters.getSeed());
    }

    @Bean
    DataManager dataManager() {
        return new DataManager();
    }

    @Bean
    SATFCServerParameters satfcServerParameters() {
        return parameters;
    }

    @Bean
    public ServletRegistrationBean servletRegistrationBean() {
        return new ServletRegistrationBean(new MetricsServlet(registry), "/metrics/extra/*");
    }

    @Bean
    public Filter gzipFilter() {
        // Apply a filter to decompress incoming compressed requests
        return new GzipRequestFilter();
    }

    @Bean
    public ICacheEntryFilter cacheScreener() {
        final ICacheEntryFilter screener;
        final SATFCServerParameters parameters = satfcServerParameters();
        switch (parameters.getCacheScreenerChoice()) {
            case NEW_INFO:
                screener = new NewInfoEntryFilter(containmentCacheLocator());
                break;
            case ADD_EVERYTHING:
                screener = (coordinate, instance, result) -> true;
                break;
            case ADD_NOTHING:
                screener = (coordinate, instance, result) -> false;
                break;
            default:
                throw new IllegalStateException("Unrecognized value for " + parameters.getCacheScreenerChoice());
        }
        return screener;
    }

    @Bean
    public ICacheEntryTransformer cacheEntryTransformer() {
        final SATFCServerParameters parameters = satfcServerParameters();
        return parameters.isCacheUHFOnly() ? new UHFRestrictionTransformer() : x -> x;
    }

}
