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
package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.commons.io.FileUtils;

import redis.clients.jedis.Jedis;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.RedisUtils;

import com.google.common.base.Joiner;

/**
 * Solver decorator that saves CNFs on solve query.
 *
 * @author afrechet
 */
public class CNFSaverSolverDecorator extends ASolverDecorator {

    private final IConstraintManager fConstraintManager;
    private final ICNFSaver fCNFSaver;
    private boolean preSAT;

    public CNFSaverSolverDecorator(@NonNull ISolver aSolver,
                                   @NonNull IConstraintManager aConstraintManager,
                                   @NonNull ICNFSaver aCNFSaver,
                                   boolean preSAT) {
        super(aSolver);
        this.fCNFSaver = aCNFSaver;
        this.preSAT = preSAT;
        fConstraintManager = aConstraintManager;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        //Encode instance.
        final SATCompressor aSATEncoder = new SATCompressor(fConstraintManager);
        final SATEncoder.CNFEncodedProblem aEncoding = aSATEncoder.encodeWithAssignment(aInstance);
        final CNF CNF = aEncoding.getCnf();

        //Create comments
        final String[] comments = new String[]{
                "FCC Feasibility Checking Instance",
                "Instance Info: " + aInstance.getInfo(),
                "Original instance: " + aInstance.getName()
        };

        final String cnfFileContentString = CNF.toDIMACS(comments);

        //Save instance to redis
        final String CNFName = aInstance.getHashString();
        fCNFSaver.saveCNF(aInstance.getName(), CNFName, cnfFileContentString);

        if (!preSAT) {
            // Create assignment
            final String assignmentString = Joiner.on(System.lineSeparator()).join(aEncoding.getInitialAssignment().entrySet().stream()
                    .map(entry -> entry.getKey() + " " + (entry.getValue() ? 1 : 0)).collect(Collectors.toList()));
            fCNFSaver.saveAssignment(CNFName, assignmentString);
        }

        return fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
    }

    /**
     * Abstraction around ssaving CNF files
     */
    public interface ICNFSaver {

        /**
         *
         * @param instanceName Name of the instance
         * @param CNFName Name of the CNF
         * @param CNFContents CNF contents as a string
         */
        void saveCNF(String instanceName, String CNFName, String CNFContents);

        /**
         *
         * @param CNFName Name of the CNF
         * @param assignmentContents Assignment contents as string
         */
        void saveAssignment(String CNFName, String assignmentContents);

    }

    @RequiredArgsConstructor
    public static class RedisCNFSaver implements ICNFSaver {
    	
    	@NonNull
    	private final Jedis jedis;
    	@NonNull
    	private final String queueName;

        @Override
        public void saveCNF(String instanceName, String CNFName, String CNFContents) {
            final String contentKey = RedisUtils.makeKey(queueName, RedisUtils.CNF_QUEUE, CNFName);
            final String indexKey = RedisUtils.makeKey(queueName, RedisUtils.CNF_INDEX_QUEUE, CNFName);
            jedis.set(contentKey, CNFContents);
            jedis.rpush(indexKey, instanceName);
        }

        @Override
        public void saveAssignment(String CNFName, String assignmentContents) {
            final String assignmentKey = RedisUtils.makeKey(queueName, RedisUtils.CNF_ASSIGNMENT_QUEUE, CNFName);
            jedis.set(assignmentKey, assignmentContents);
        }

    }

    public static class FileCNFSaver implements ICNFSaver {

        private final String fCNFDirectory;

        public FileCNFSaver(@NonNull String aCNFDirectory) {
            File cnfdir = new File(aCNFDirectory);
            if (!cnfdir.exists()) {
                throw new IllegalArgumentException("CNF directory " + aCNFDirectory + " does not exist.");
            }
            if (!cnfdir.isDirectory()) {
                throw new IllegalArgumentException("CNF directory " + aCNFDirectory + " is not a directory.");
            }
            fCNFDirectory = aCNFDirectory;
        }


        @Override
        public void saveCNF(String instanceName, String CNFName, String CNFContents) {
            try {
                FileUtils.writeStringToFile(new File(fCNFDirectory+File.separator+CNFName + ".cnf"), CNFContents);
            } catch (IOException e) {
                throw new IllegalStateException("Could not write CNF to file", e);
            }
        }

        @Override
        public void saveAssignment(String CNFName, String assignmentContents) {
            try {
                FileUtils.writeStringToFile(new File(fCNFDirectory+File.separator+CNFName + "_assignment.txt"), assignmentContents);
            } catch (IOException e) {
                throw new IllegalStateException("Could not write CNF to file", e);
            }
        }

    }

}
