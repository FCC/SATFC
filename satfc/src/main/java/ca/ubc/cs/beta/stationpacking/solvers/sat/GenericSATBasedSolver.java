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
package ca.ubc.cs.beta.stationpacking.solvers.sat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;
import lombok.extern.slf4j.Slf4j;

/**
 * SAT based ISolver that uses a SAT solver to solve station packing problems.
 */
@Slf4j
public class GenericSATBasedSolver implements ISolver {

    private final ISATEncoder fSATEncoder;
    private final ISATSolver fSATSolver;

    protected GenericSATBasedSolver(ISATSolver aSATSolver, ISATEncoder aSATEncoder) {
        fSATEncoder = aSATEncoder;
        fSATSolver = aSATSolver;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {

        Watch watch = Watch.constructAutoStartWatch();

        log.debug("Solving instance of {}...", aInstance.getInfo());

        log.debug("Encoding subproblem in CNF...");
        SATEncoder.CNFEncodedProblem aEncoding = fSATEncoder.encodeWithAssignment(aInstance);
        CNF aCNF = aEncoding.getCnf();
        ISATDecoder aDecoder = aEncoding.getDecoder();
        log.debug("CNF has {} clauses.", aCNF.size());
        if (aTerminationCriterion.hasToStop()) {
            log.debug("All time spent.");
            return SolverResult.createTimeoutResult(watch.getElapsedTime());
        }
        else
        {
            log.debug("Solving the subproblem CNF with " + aTerminationCriterion.getRemainingTime() + " s remaining.");
            SATSolverResult satSolverResult = fSATSolver.solve(aCNF, aEncoding.getInitialAssignment(), aTerminationCriterion, aSeed);
            // Even if the SAT solver was interrupted, this would be due to the fact that SATFC timed out. So to avoid confusion in output results, we make this change
            if (satSolverResult.getResult().equals(SATResult.INTERRUPTED)) {
                satSolverResult = new SATSolverResult(SATResult.TIMEOUT, satSolverResult.getRuntime(), satSolverResult.getAssignment(), satSolverResult.getSolvedBy(), satSolverResult.getNickname());
            }
            log.debug("Parsing result.");
            Map<Integer, Set<Station>> aStationAssignment = new HashMap<Integer, Set<Station>>();
            final Set<Station> assignedStations = new HashSet<>();
            if (satSolverResult.getResult().equals(SATResult.SAT)) {
                HashMap<Long, Boolean> aLitteralChecker = new HashMap<Long, Boolean>();
                for (Literal aLiteral : satSolverResult.getAssignment()) {
                    boolean aSign = aLiteral.getSign();
                    long aVariable = aLiteral.getVariable();
    
                    //Do some quick verifications of the assignment.
                    if (aLitteralChecker.containsKey(aVariable)) {
                        log.warn("A variable was present twice in a SAT assignment.");
                        if (!aLitteralChecker.get(aVariable).equals(aSign)) {
                            throw new IllegalStateException("SAT assignment from TAE wrapper assigns a variable to true AND false.");
                        }
                    } else {
                        aLitteralChecker.put(aVariable, aSign);
                    }
    
                    //If the litteral is positive, then we keep it as it is an assigned station to a channel.
                    if (aSign) {
                        Pair<Station, Integer> aStationChannelPair = aDecoder.decode(aVariable);
                        Station aStation = aStationChannelPair.getKey();
                        if (assignedStations.contains(aStation)) {
                            continue;
                        }
                        Integer aChannel = aStationChannelPair.getValue();
    
                        if (!aInstance.getStations().contains(aStation) || !aInstance.getDomains().get(aStation).contains(aChannel)) {
                            throw new IllegalStateException("A decoded station and channel from a component SAT assignment is not in that component's problem instance. (" + aStation + ", channel:" + aChannel + ")");
                        }
    
                        if (!aStationAssignment.containsKey(aChannel)) {
                            aStationAssignment.put(aChannel, new HashSet<Station>());
                        }

                        aStationAssignment.get(aChannel).add(aStation);
                        assignedStations.add(aStation);
                    }
                }
            }
    
            log.debug("...done.");
            log.debug("Cleaning up...");
    
            final SolverResult solverResult = new SolverResult(satSolverResult.getResult(), watch.getElapsedTime(), aStationAssignment, satSolverResult.getSolvedBy(), satSolverResult.getNickname());

            log.debug("Result:");
            log.debug(solverResult.toParsableString());

            return solverResult;
        }
    }
    
    @Override
    public void interrupt() {
    	fSATSolver.interrupt();
    }

    @Override
    public void notifyShutdown() {
        fSATSolver.notifyShutdown();
    }

}
