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
package ca.ubc.cs.beta.stationpacking.execution.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

/**
 * Created by newmanne on 12/05/15.
 */
@UsageTextField(title="Redis Parameters",description="Parameters describing how to take jobs from redis")
public class RedisParameters extends AbstractOptions {

    @Parameter(names = "-REDIS-QUEUE", description = "The queue to take redis jobs from")
    public String fRedisQueue;
    @Parameter(names = "-REDIS-PORT", description = "Redis port (for problem queue)")
    public Integer fRedisPort;
    @Parameter(names = "-REDIS-HOST", description = "Redis host (for problem queue)")
    public String fRedisHost;

    private static Jedis jedis;

    synchronized public Jedis getJedis() {
        Logger log = LoggerFactory.getLogger(RedisParameters.class);
        if (jedis == null) {
            log.info("Making a redis connection to {}:{}", fRedisHost, fRedisPort);
            jedis = new Jedis(fRedisHost, fRedisPort);
        }
        return jedis;
    }

    public boolean areValid() {
        return fRedisQueue != null && fRedisPort != null && fRedisHost != null;
    }
}