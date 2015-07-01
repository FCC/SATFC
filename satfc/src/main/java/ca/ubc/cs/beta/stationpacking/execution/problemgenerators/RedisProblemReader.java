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
package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import ca.ubc.cs.beta.stationpacking.execution.AProblemReader;
import ca.ubc.cs.beta.stationpacking.execution.Converter;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;
import ca.ubc.cs.beta.stationpacking.utils.RedisUtils;

import com.google.common.collect.Sets;

/**
* Created by newmanne on 12/05/15.
* Reads in problems from a redis queue, where each entry in the queue is a (full path) to an srpk file
*/
@Slf4j
public class RedisProblemReader extends AProblemReader {

    private final Jedis jedis;
    private final String queueName;
    private final String interferencesFolder;
    private String activeProblemFullPath;

    public RedisProblemReader(Jedis jedis, String queueName, String interferencesFolder) {
        this.interferencesFolder = interferencesFolder;
        this.jedis = jedis;
        if (!jedis.exists(RedisUtils.makeKey(queueName))) {
            throw new IllegalArgumentException("Queue " + queueName + " does not exist");
        }
        this.queueName = queueName;
        log.info("Reading instances from queue {}", RedisUtils.makeKey(queueName));
    }

    @Override
    public SATFCFacadeProblem getNextProblem() {
        Converter.StationPackingProblemSpecs stationPackingProblemSpecs;
        String fullPathToInstanceFile;
        String instanceFileName;
        while (true) {
            fullPathToInstanceFile = jedis.rpoplpush(RedisUtils.makeKey(queueName), RedisUtils.makeKey(queueName, RedisUtils.PROCESSING_QUEUE));
            if (fullPathToInstanceFile == null) { // all problems exhausted
                return null;
            }
            instanceFileName = new File(fullPathToInstanceFile).getName();
            try {
                stationPackingProblemSpecs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(fullPathToInstanceFile);
                break;
            } catch (IOException e) {
                log.warn("Error parsing file " + fullPathToInstanceFile + ", skipping it", e);
            }
        }
        final long remainingJobs = jedis.llen(RedisUtils.makeKey(queueName));
        log.info("There are {} problems remaining in the queue", remainingJobs);
        activeProblemFullPath = fullPathToInstanceFile;
        final Set<Integer> stations = stationPackingProblemSpecs.getDomains().keySet();
        return new SATFCFacadeProblem(
                stations,
                stationPackingProblemSpecs.getDomains().values().stream().reduce(new HashSet<>(), Sets::union),
                stationPackingProblemSpecs.getDomains(),
                stationPackingProblemSpecs.getPreviousAssignment(),
                interferencesFolder + File.separator + stationPackingProblemSpecs.getDataFoldername(),
                instanceFileName
        );
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