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
    private boolean saveAssignment;

    public CNFSaverSolverDecorator(@NonNull ISolver aSolver,
                                   @NonNull IConstraintManager aConstraintManager,
                                   @NonNull ICNFSaver aCNFSaver,
                                   boolean saveAssignment) {
        super(aSolver);
        this.fCNFSaver = aCNFSaver;
        this.saveAssignment = saveAssignment;
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

        if (saveAssignment) {
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
         * @param instanceName Name of the instance
         * @param CNFName      Name of the CNF
         * @param CNFContents  CNF contents as a string
         */
        void saveCNF(String instanceName, String CNFName, String CNFContents);

        /**
         * @param CNFName            Name of the CNF
         * @param assignmentContents Assignment contents as string
         */
        void saveAssignment(String CNFName, String assignmentContents);

    }

    /**
     * This CNFSaver wraps anohter CNF saver and builds an index of saved CNFs in redis for easy parsing
     */
    @RequiredArgsConstructor
    public static class RedisIndexCNFSaver implements ICNFSaver {

        // This just builds an index in redis: use a different saver to save the actual files
        @NonNull
        private final ICNFSaver saver;

        @NonNull
        private final Jedis jedis;
        @NonNull
        private final String queueName;

        @Override
        public void saveCNF(String instanceName, String CNFName, String CNFContents) {
            final String indexKey = RedisUtils.makeKey(queueName, RedisUtils.CNF_INDEX_QUEUE);
            jedis.rpush(indexKey, Joiner.on(',').join(CNFName, instanceName));
            saver.saveCNF(instanceName, CNFName, CNFContents);
        }

        @Override
        public void saveAssignment(String CNFName, String assignmentContents) {
            saver.saveAssignment(CNFName, assignmentContents);
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
                FileUtils.writeStringToFile(new File(fCNFDirectory + File.separator + CNFName + ".cnf"), CNFContents);
            } catch (IOException e) {
                throw new IllegalStateException("Could not write CNF to file", e);
            }
        }

        @Override
        public void saveAssignment(String CNFName, String assignmentContents) {
            try {
                FileUtils.writeStringToFile(new File(fCNFDirectory + File.separator + CNFName + "_assignment.txt"), assignmentContents);
            } catch (IOException e) {
                throw new IllegalStateException("Could not write CNF to file", e);
            }
        }

    }

}
