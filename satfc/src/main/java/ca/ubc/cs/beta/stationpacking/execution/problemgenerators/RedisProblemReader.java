/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import ca.ubc.cs.beta.stationpacking.execution.AProblemReader;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.problemparsers.IProblemParser;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import ca.ubc.cs.beta.stationpacking.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
* Created by newmanne on 12/05/15.
* Reads in problems from a redis queue, where each entry in the queue is a (full path) to an srpk file
*/
@Slf4j
public class RedisProblemReader extends AProblemReader {

    private final Jedis jedis;
    private final String queueName;
    private final IProblemParser nameToProblem;
    private final boolean testForCachedSolution;
    private String activeProblemFullPath;

    public RedisProblemReader(Jedis jedis, String queueName, IProblemParser nameToProblem, boolean testForCachedSolution) {
        this.jedis = jedis;
        this.queueName = queueName;
        this.nameToProblem = nameToProblem;
        this.testForCachedSolution = testForCachedSolution;
        log.info("Reading instances from queue {}", RedisUtils.makeKey(queueName));
    }

    @Override
    public SATFCFacadeProblem getNextProblem() {
        SATFCFacadeProblem problem = null;
        String fullPathToInstanceFile;
        String instanceFileName;
        while (true) {
            fullPathToInstanceFile = jedis.rpoplpush(RedisUtils.makeKey(queueName), RedisUtils.makeKey(queueName, RedisUtils.PROCESSING_QUEUE));
            if (fullPathToInstanceFile == null) { // all problems exhausted
                return null;
            }
            instanceFileName = FilenameUtils.getBaseName(fullPathToInstanceFile);
            try {
                problem = nameToProblem.problemFromName(fullPathToInstanceFile);
                break;
            } catch (IOException e) {
                log.warn("Error parsing file " + fullPathToInstanceFile + ", skipping it", e);
            }
        }

        if (problem != null) {
            // Do a dumb check to see if we have a solution cached in redis
            if (this.testForCachedSolution) {
                String answer = jedis.get(instanceFileName);
                if (answer != null) {
                    // TODO: storing JSON blows up memory for no reason, use a more condensed format...
                    final TypeReference<Map<String, Integer>> typeRef = new TypeReference<Map<String, Integer>>() {};
                    try {
                        final Map<String, Integer> ans = JSONUtils.getMapper().readValue(answer, typeRef);
                        final Map<Integer, Integer> solution = ans.entrySet().stream().collect(Collectors.toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue));
                        problem.setPreviousAssignment(solution);
                    } catch (IOException e) {
                        throw new RuntimeException("Couldn't parse solution!!!", e);
                    }
                }
            }
        }

        final long remainingJobs = jedis.llen(RedisUtils.makeKey(queueName));
        log.info("There are {} problems remaining in the queue", remainingJobs);
        activeProblemFullPath = fullPathToInstanceFile;
        return problem;
    }

    @Override
    public void onPostProblem(SATFCFacadeProblem problem, SATFCResult result) {
        super.onPostProblem(problem, result);
        // update redis queue - if the job timed out, move it to the timeout channel. Either way, delete it from the processing queue
        if (!result.getResult().isConclusive()) {
            log.info("Adding problem " + problem.getInstanceName() + " to the timeout queue");
            jedis.rpush(RedisUtils.makeKey(queueName, RedisUtils.TIMEOUTS_QUEUE), activeProblemFullPath);
        }
        final long numDeleted = jedis.lrem(RedisUtils.makeKey(queueName, RedisUtils.PROCESSING_QUEUE), 1, activeProblemFullPath);
        if (numDeleted != 1) {
            log.error("Couldn't delete problem " + activeProblemFullPath + " from the processing queue!");
        }
    }

}