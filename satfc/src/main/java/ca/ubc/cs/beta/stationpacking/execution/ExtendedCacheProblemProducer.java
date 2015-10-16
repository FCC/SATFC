package ca.ubc.cs.beta.stationpacking.execution;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.ICacher.SATCacheEntry;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.IProblemSampler;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.IStationSampler;
import ca.ubc.cs.beta.stationpacking.execution.parameters.extendedcache.ExtendedCacheProblemProducerParameters;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.SATFCFacadeProblem;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
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
     *
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        ExtendedCacheProblemProducerParameters parameters = new ExtendedCacheProblemProducerParameters();
        JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters);
        parameters.fLoggingOptions.initializeLogging();
        log = LoggerFactory.getLogger(ExtendedCacheProblemProducer.class);

        Jedis jedis = parameters.fRedisParameters.getJedis();
        IStationSampler stationSampler = parameters.fStationSamplerParameters.getStationSampler();
        IProblemSampler problemSampler = parameters.fProblemSamplerParameters.getProblemSampler(parameters.fRedisParameters);

        if(parameters.fCleanQueue) cleanUpAllQueues(jedis, parameters.fRedisParameters.fRedisQueue);

        String jobQueue = RedisUtils.makeKey(parameters.fRedisParameters.fRedisQueue, RedisUtils.JOB_QUEUE);
        while(true){

            if(queueSizeLow(jedis, jobQueue, parameters.fQueueSizeThreshold)){

                log.info("Job queue size below threshold, sampling more problems to put into key queue...");
                problemSampler.sample(parameters.fQueueSizeThreshold).forEach(key -> jedis.lpush(parameters.fRedisParameters.fRedisQueue, key));
                extendProblem(parameters, jedis, stationSampler);

            } else {
                log.info("Job queue size above threshold, producer thread sleeping...");
                Thread.sleep(parameters.fSleepInterval);
            }
        }
    }

    private static void extendProblem(ExtendedCacheProblemProducerParameters parameters, Jedis jedis, IStationSampler stationSampler) throws FileNotFoundException {
        log.info("Extending...");

        String domainCSV = parameters.fInterferencesFolder + DataManager.DOMAIN_FILE;
        IStationManager stationManager = new DomainStationManager(domainCSV);

        String key;
        while((key = jedis.lpop(parameters.fRedisParameters.fRedisQueue)) != null){

            final SATCacheEntry satEntry = getSATCacheEntry(jedis, key);

            //initial stations domains
            Set<Station> stations = satEntry.getStations();
            Set<Integer> stationIDs = stations.stream().map(station -> station.getID()).collect(Collectors.toSet());
            Map<Integer, Set<Integer>> domains = lookUpDomains(parameters.fClearingTarget, stationManager, stations);
            int expectedDomainSize = domains.size() + 1;

            //add a new station and its domain
            Integer stationID = stationSampler.sample(stationIDs);
            stationIDs.add(stationID);
            domains.put(stationID, lookUpOneDomain(parameters.fClearingTarget, stationManager, new Station(stationID)));
            Assert.isTrue(expectedDomainSize == domains.size());
            Set<Integer> channelsToPackOn = domains.values().stream().reduce(new HashSet<>(), Sets::union);

            //build problem instance
            Map<Integer, Integer> previousAssignment = cleanUpPreviousAssignment(parameters.fClearingTarget, satEntry.getStationToChannel(), domains);

            String instanceName = getHashString(domains.toString() + satEntry.toString());
            //only attach the unique interference folder identifier
            //so that solver can run at other clusters without running into interference folder discrepancy problem
            String[] splits = parameters.fInterferencesFolder.split("/");
            String intereferenceFolder = splits[splits.length-1];

            SATFCFacadeProblem problem =
                    new SATFCFacadeProblem(stationIDs, channelsToPackOn, domains, previousAssignment, intereferenceFolder, instanceName);

            enqueueProblem(parameters.fRedisParameters.fRedisQueue, jedis, instanceName, problem);

        }
    }

    private static SATCacheEntry getSATCacheEntry(Jedis jedis, String key) {
        String entry = jedis.get(key);
        return JSONUtils.toObject(entry, SATCacheEntry.class);
    }

    private static Map<Integer, Set<Integer>> lookUpDomains(int clearingTarget, IStationManager stationManager, Set<Station> stations) {
        Map<Integer, Set<Integer>> domains = new HashMap<>();
        StreamSupport.stream(stations.spliterator(), false)
                .forEach(station -> domains.put(station.getID(), lookUpOneDomain(clearingTarget, stationManager, station)));
        return domains;
    }

    private static Set<Integer> lookUpOneDomain(int clearingTarget, IStationManager stationManager, Station station){
        return StreamSupport.stream(stationManager.getDomain(station).spliterator(), false)
                .filter(channel -> channel <= clearingTarget)
                .collect(Collectors.toSet());
    }

    /**
     * If previous assigned channel is greater than clearing target, remove from the assignment as it is an invalid heuristic
     * If previous assigned channel is not in station domain, remove from the assignment
     * @param clearingTarget highest channel a station can be packed on
     * @param assignment map of station to its assigned channel
     * @param domains map of station to its domain channels
     * @return cleaned up map of assignment
     */
    private static Map<Integer, Integer> cleanUpPreviousAssignment(int clearingTarget, Map<Integer, Integer> assignment, Map<Integer, Set<Integer>> domains) {
        Iterator<Map.Entry<Integer, Integer>> iterator = assignment.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<Integer, Integer> next = iterator.next();
            int station = next.getKey();
            int assignedChannel = next.getValue();
            if (assignedChannel > clearingTarget || !domains.get(station).contains(assignedChannel)) {
                iterator.remove();
            }
        }
        return assignment;
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
        if (!fRedisQueue.isEmpty()) {
            Set<String> queueKeys = jedis.keys("*" + fRedisQueue + "*");
            queueKeys.forEach(key -> jedis.del(key));
        } else {
            throw new ParameterException("Invalid redis queue argument detected.");
        }
        log.info("Finished cleaning up related queues");
    }

    private static boolean queueSizeLow(Jedis jedis, String queue, int threshold){
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
