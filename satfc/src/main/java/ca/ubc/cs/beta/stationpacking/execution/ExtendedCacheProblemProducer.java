package ca.ubc.cs.beta.stationpacking.execution;

import java.io.FileNotFoundException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Transaction;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.ICacher;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.IProblemSampler;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.IStationSampler;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.ProblemSamplerFactory;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.StationSamplerFactory;
import ca.ubc.cs.beta.stationpacking.execution.parameters.ExtendedCacheProblemProducerParameters;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.SATFCFacadeProblem;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import ca.ubc.cs.beta.stationpacking.utils.RedisUtils;

import com.beust.jcommander.ParameterException;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Created by emily404 on 5/28/15.
 */
public class ExtendedCacheProblemProducer {

    private static Logger log;

    /**
     * This method takes an existing SAT cache entry and generate a new problem by adding a new station
     * The problem identifier is pushed into jobQueue and the problem json is stored at problemHash
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {

        ExtendedCacheProblemProducerParameters parameters = new ExtendedCacheProblemProducerParameters();
        JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters);
        parameters.fLoggingOptions.initializeLogging();
        log = LoggerFactory.getLogger(ExtendedCacheProblemProducer.class);

        String domainCSV = parameters.fInterferencesFolder + "/Domain.csv";
        IStationManager stationManager = new DomainStationManager(domainCSV);
        Jedis jedis = parameters.fRedisParameters.getJedis();
        IStationSampler stationSampler = StationSamplerFactory.getStationSampler(parameters.fStationSampler);
        IProblemSampler problemSampler = ProblemSamplerFactory.getProblemSampler(parameters.fProblemSampler);

        cleanUpAllQueues(jedis, parameters.fRedisParameters.fRedisQueue);

        populatingKeyQueue(parameters, jedis);

        while(true){
            if(queueSizeLow(jedis, parameters.fRedisParameters.fRedisQueue, parameters.fQueueSizeThreshold)){
                log.info("Queue size low, sampling more problems");
                jedis.lpush(parameters.fRedisParameters.fRedisQueue, problemSampler.sample());
                if(queueSizeLow(jedis, parameters.fRedisParameters.fRedisQueue, parameters.fQueueSizeThreshold)) {
                    Thread.sleep(parameters.fSleepInterval);
                }
            } else {
                String key;
                while((key = jedis.lpop(parameters.fRedisParameters.fRedisQueue)) != null){

                    final ContainmentCacheSATEntry ccEntry = getContainmentCacheSATEntry(jedis, key);

                    //initial stations domains
                    Set<Integer> stationIDs = ccEntry.getElements().stream().map(station -> station.getID()).collect(Collectors.toSet());
                    Map<Integer, Set<Integer>> domains = lookUpDomains(parameters.fClearingTarget, stationManager, ccEntry.getElements());

                    //add a new station and its domain
                    Integer stationID = stationSampler.sample((BitSet)ccEntry.getBitSet().clone());
                    stationIDs.add(stationID);
                    domains.put(stationID, lookUpOneDomain(parameters.fClearingTarget, stationManager, new Station(stationID)));
                    Set<Integer> channelsToPackOn = domains.values().stream().reduce(new HashSet<>(), Sets::union);

                    //build problem instance
                    String instanceName = getHashString(domains.toString()+ccEntry.toString());
                    SATFCFacadeProblem problem =
                            new SATFCFacadeProblem(stationIDs, channelsToPackOn, domains, ccEntry.getAssignment(), parameters.fInterferencesFolder, instanceName);

                    enqueueProblem(parameters.fRedisParameters.fRedisQueue, jedis, instanceName, problem);

                }
            }
        }
    }

    /**
     * convert entry string to containment cache entry object
     * @return a ContainmentCacheSATEntry with key
     */
    private static ContainmentCacheSATEntry getContainmentCacheSATEntry(Jedis jedis, String key) {
        String entry = jedis.get(key);
        ICacher.SATCacheEntry cacheEntry = JSONUtils.toObject(entry, ICacher.SATCacheEntry.class);
        return new ContainmentCacheSATEntry(cacheEntry.getAssignment(), key);
    }

    private static Map<Integer, Set<Integer>> lookUpDomains(int clearingTarget, IStationManager stationManager, Set<Station> stations) {
        Map<Integer, Set<Integer>> domains = new HashMap<>();
        StreamSupport.stream(stations.spliterator(), false)
                .forEach(station -> domains.put(station.getID(), lookUpOneDomain(clearingTarget, stationManager, station)));
        return domains;
    }

    private static Set<Integer> lookUpOneDomain(int clearingTarget, IStationManager stationManager, Station station){
        // TODO: filter out the special channel 37?
        return StreamSupport.stream(stationManager.getDomain(station).spliterator(), false)
                .filter(channel -> channel <= clearingTarget)
                .collect(Collectors.toSet());
    }

    /**
     * atomically load key into job queue and load problem json into hash
     */
    private static void enqueueProblem(String redisQueue, Jedis jedis, String instanceName, SATFCFacadeProblem problem) {
        String json = JSONUtils.toString(problem);
        Transaction multi = jedis.multi();
        multi.rpush(RedisUtils.makeKey(redisQueue, RedisUtils.JOB_QUEUE), instanceName);
        multi.hset(RedisUtils.makeKey(redisQueue, RedisUtils.JSON_HASH), instanceName, json);
        multi.exec();
    }

    /**
     * fresh start, also clear all queues if restarted upon crash
     */
    private static void cleanUpAllQueues(Jedis jedis, String fRedisQueue) {
        log.info("Cleaning up related queues");
        if(!fRedisQueue.isEmpty()){
            Set<String> queueKeys = jedis.keys("*" + fRedisQueue + "*");
            queueKeys.forEach(key -> jedis.del(key));
        } else {
            throw new ParameterException("Invalid redis queue argument detected.");
        }
        log.info("Finished cleaning up related queues");
    }

    private static void populatingKeyQueue(ExtendedCacheProblemProducerParameters parameters, Jedis jedis) {
        log.info("Populating keyQueue");
        String startAndEndCursor = "0";
        ScanParams params = new ScanParams();
        params.match("*:SAT*");
        ScanResult<String> scanResult = jedis.scan(startAndEndCursor, params);
        while(!scanResult.getStringCursor().equals(startAndEndCursor)){
            scanResult.getResult().forEach(key -> jedis.lpush(parameters.fRedisParameters.fRedisQueue, key));
            scanResult = jedis.scan(scanResult.getStringCursor(), params);
        }
        log.info("Finished populating keyQueue");
    }

    private static boolean queueSizeLow(Jedis jedis, String queue, int threshold){
        log.info("Checking queue size");
        return jedis.llen(queue) < threshold;
    }

    private static String getHashString(String aString) {
        HashFunction fHashFuction = Hashing.murmur3_32();
        final HashCode hash = fHashFuction.newHasher()
                .putString(aString, Charsets.UTF_8)
                .hash();
        return hash.toString();
    }
}
