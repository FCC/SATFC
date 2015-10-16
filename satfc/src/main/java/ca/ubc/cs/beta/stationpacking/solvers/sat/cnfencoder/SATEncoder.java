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
package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import lombok.Data;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base.IBijection;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base.IdentityBijection;

/**
 * Encodes a problem instance as a propositional satisfiability problem.
 * A variable of the SAT encoding is a station channel pair, each constraint is trivially
 * encoded as a clause (this station cannot be on this channel when this other station is on this other channel is a two clause with the previous
 * SAT variables), and base clauses are added (each station much be on exactly one channel).
 *
 * @author afrechet
 */
public class SATEncoder implements ISATEncoder {

    private final IConstraintManager fConstraintManager;
    private final IBijection<Long, Long> fBijection;

    public SATEncoder(IConstraintManager aConstraintManager) {
        this(aConstraintManager, new IdentityBijection<Long>());
    }

    public SATEncoder(IConstraintManager aConstraintManager, IBijection<Long, Long> aBijection) {
        fConstraintManager = aConstraintManager;

        fBijection = aBijection;
    }

    public CNFEncodedProblem encodeWithAssignment(StationPackingInstance aInstance) {
        Pair<CNF, ISATDecoder> enconding = encode(aInstance);
        /**
         * Generate the starting values of the variables based on the prevoius assignment information: if a station was
         * assigned to a channel, then the corresponding variable is set to true. Otherwise, false. This might not result
         * in a file with a value for every variable. Presumably whoever uses this can do something sensible with the rest,
         * typically random assignment.
         */

        final Map<Long, Boolean> initialAssignment = new LinkedHashMap<>();
        aInstance.getDomains().entrySet().forEach(entry -> {
            final Station station = entry.getKey();
            if (aInstance.getPreviousAssignment().containsKey(station)) {
                final Set<Integer> domain = entry.getValue();
                domain.forEach(channel -> {
                    long varId = fBijection.map(SATEncoderUtils.SzudzikElegantPairing(station.getID(), channel));
                    boolean startingValue = aInstance.getPreviousAssignment().get(station).equals(channel);
                    initialAssignment.put(varId, startingValue);
                });
            }
        });
        return new CNFEncodedProblem(enconding.getFirst(), enconding.getSecond(), initialAssignment);
    }

    @Data
    public static class CNFEncodedProblem {
        private final CNF cnf;
        private final ISATDecoder decoder;
        private final Map<Long, Boolean> initialAssignment;
    }

    @Override
    public Pair<CNF, ISATDecoder> encode(StationPackingInstance aInstance) {

        CNF aCNF = new CNF();

        //Encode base clauses,
        aCNF.addAll(encodeBaseClauses(aInstance));

        //Encode co-channel and adj-channel constraints
        aCNF.addAll(encodeInterferenceConstraints(aInstance));

        //Save station map.
        final Map<Integer, Station> stationMap = new HashMap<>();
        for (Station station : aInstance.getStations()) {
            stationMap.put(station.getID(), station);
        }

        //Create the decoder
        ISATDecoder aDecoder = new ISATDecoder() {
            @Override
            public Pair<Station, Integer> decode(long aVariable) {

                //Decode the long variable to station channel pair.
                Pair<Integer, Integer> aStationChannelPair = SATEncoderUtils.SzudzikElegantInversePairing(fBijection.inversemap(aVariable));

                //Get station.
                Integer stationID = aStationChannelPair.getKey();
                Station aStation = stationMap.get(stationID);

                //Get channel
                Integer aChannel = aStationChannelPair.getValue();

                return new Pair<>(aStation, aChannel);
            }
        };

        return new Pair<CNF, ISATDecoder>(aCNF, aDecoder);
    }

    /**
     * Get the base SAT clauses of a station packing instances. The base clauses encode the following two constraints:
     * <ol>
     * <li> Every station must be on at least one channel in the intersection of its domain and the problem instance's channels. </li>
     * <li> Every station must be on at most one channel in the intersection of its domain and the problem instance's channels. </li>
     * <ol>
     *
     * @param aInstance - a station packing problem instance.
     * @return A CNF of base clauses.
     */
    public CNF encodeBaseClauses(StationPackingInstance aInstance) {
        CNF aCNF = new CNF();

        Set<Station> aInstanceStations = aInstance.getStations();
        Map<Station, Set<Integer>> aInstanceDomains = aInstance.getDomains();

        //Each station has its own base clauses.
        for (Station aStation : aInstanceStations) {
            ArrayList<Integer> aStationInstanceDomain = new ArrayList<Integer>(aInstanceDomains.get(aStation));

            //A station must be on at least one channel,
            Clause aStationValidAssignmentBaseClause = new Clause();
            for (Integer aChannel : aStationInstanceDomain) {

                aStationValidAssignmentBaseClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(), aChannel)), true));
            }
            aCNF.add(aStationValidAssignmentBaseClause);

            //A station can be on at most one channel,
            for (int i = 0; i < aStationInstanceDomain.size(); i++) {
                for (int j = i + 1; j < aStationInstanceDomain.size(); j++) {
                    Clause aStationSingleAssignmentBaseClause = new Clause();

                    Integer aDomainChannel1 = aStationInstanceDomain.get(i);
                    aStationSingleAssignmentBaseClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(), aDomainChannel1)), false));

                    Integer aDomainChannel2 = aStationInstanceDomain.get(j);
                    aStationSingleAssignmentBaseClause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(aStation.getID(), aDomainChannel2)), false));

                    aCNF.add(aStationSingleAssignmentBaseClause);
                }
            }
        }

        return aCNF;
    }

    private CNF encodeInterferenceConstraints(StationPackingInstance aInstance) {
        final CNF cnf = new CNF();

        fConstraintManager.getAllRelevantConstraints(aInstance.getDomains()).forEach(constraint -> {
            final Clause clause = new Clause();
            clause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(constraint.getSource().getID(), constraint.getSourceChannel())), false));
            clause.add(new Literal(fBijection.map(SATEncoderUtils.SzudzikElegantPairing(constraint.getTarget().getID(), constraint.getTargetChannel())), false));
            cnf.add(clause);
        });

        return cnf;
    }

}
