package ca.ubc.cs.beta.stationpacking.execution.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

/**
 * Created by newmanne on 12/05/15.
 */
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