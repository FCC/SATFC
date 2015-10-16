package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import ca.ubc.cs.beta.stationpacking.execution.AProblemReader;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import ca.ubc.cs.beta.stationpacking.utils.RedisUtils;

/**
 * Created by emily404 on 5/20/15.
 */

@Slf4j
public class ExtendedCacheProblemReader extends AProblemReader {

    private final Jedis jedis;
    private final String keyQueue;
    private final String interferenceFolder;

    public ExtendedCacheProblemReader(Jedis jedis, String keyQueue, String interferenceFolder) {
        this.jedis = jedis;
        this.keyQueue = keyQueue;
        this.interferenceFolder = interferenceFolder;
        log.info("Reading instances from queue {}", RedisUtils.makeKey(keyQueue, RedisUtils.JOB_QUEUE));
    }

    @Override
    public SATFCFacadeProblem getNextProblem() {
        log.info("Get next problem from queue {}", RedisUtils.makeKey(keyQueue, RedisUtils.JOB_QUEUE));
        // atomically pop a problem entry and push into processingQueue
        String instanceName = jedis.rpoplpush(RedisUtils.makeKey(keyQueue, RedisUtils.JOB_QUEUE), RedisUtils.makeKey(keyQueue, RedisUtils.PROCESSING_QUEUE));
        if(instanceName != null){
            String jsonProblem = jedis.hget(RedisUtils.makeKey(keyQueue, RedisUtils.JSON_HASH), instanceName);
            final SATFCFacadeProblem problem = JSONUtils.toObject(jsonProblem, SATFCFacadeProblem.class);
            //reconstruct interference folder with local path
            problem.setStationConfigFolder(interferenceFolder+problem.getStationConfigFolder());
            return problem;
        }
        return null;
    }

    @Override
    public void onPostProblem(SATFCFacadeProblem problem, SATFCResult result) {
        super.onPostProblem(problem, result);

        // update redis queue - if the job timed out, move it to the timeout channel. Either way, delete it from the processing queue
        if (!result.getResult().isConclusive()) {
            log.info("problem " + problem.getInstanceName() + "timed out");
            jedis.rpush(RedisUtils.makeKey(keyQueue, RedisUtils.TIMEOUTS_QUEUE), problem.getInstanceName());
        }

        Transaction multi = jedis.multi();
        multi.lrem(RedisUtils.makeKey(keyQueue, RedisUtils.PROCESSING_QUEUE), 1, problem.getInstanceName());
        multi.hdel(RedisUtils.makeKey(keyQueue, RedisUtils.JSON_HASH), problem.getInstanceName());
        multi.exec();

        while (jedis.llen(RedisUtils.makeKey(keyQueue, RedisUtils.JOB_QUEUE)) == 0) {
            try{
                log.info("Job queue empty, solver thread sleeping...");
                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

}
