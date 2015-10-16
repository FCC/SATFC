/**
 * Copyright 2015, Auctionomics, Alexandre FrÃ©chette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.solvers.underconstrained;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.output.NullOutputStream;
import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.Constraint;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Created by newmanne on 1/8/15.
 * Beta idea about using linear programs to find underconstrained stations
 */
@Slf4j
public class MIPUnderconstrainedStationFinder implements IUnderconstrainedStationFinder {

    private final IConstraintManager constraintManager;
    private boolean exact;

    public MIPUnderconstrainedStationFinder(IConstraintManager constraintManager, boolean exact) {
        this.constraintManager = constraintManager;
        this.exact = exact;
    }
    
    public MIPUnderconstrainedStationFinder(IConstraintManager constraintManager) {
    	this(constraintManager, false);
    }

    @Override
    public Set<Station> getUnderconstrainedStations(Map<Station, Set<Integer>> domains, ITerminationCriterion terminationCriterion, Set<Station> stationsToCheck) {
        final Set<Station> underconstrainedStations = new HashSet<>();
        log.debug("Finding underconstrained stations in the instance...");
        final NeighborIndex<Station, DefaultEdge> neighborIndex = new NeighborIndex<>(ConstraintGrouper.getConstraintGraph(domains, constraintManager));
        for (final Station station : stationsToCheck) {
            final Set<Integer> domain = domains.get(station);
            final Set<Station> neighbours = neighborIndex.neighborsOf(station);
            log.debug("Station {} has {} neighbours, {}", station, neighbours.size(), neighbours);
            if (neighbours.isEmpty()) {
                log.debug("Station {} has no neighbours and is therefore trivially underconstrained", station);
                underconstrainedStations.add(station);
            } else {
                // Create a map of which channels on the station's domain each of its neighbours can block it given all of their choices
                final Map<Station, Map<Integer, Set<Integer>>> channelsThatANeighbourCanBlockOut = neighbours.stream() // Map<Neighbour, Map<NeigbhournCHannel, Set<MyBadChannel>>
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        neighbour -> domains.get(neighbour).stream()
                                                .collect(
                                                        Collectors.toMap(
                                                                Function.identity(),
                                                                neighbourChannel -> domain.stream()
                                                                        .filter(myChannel -> !constraintManager.isSatisfyingAssignment(station, myChannel, neighbour, (int) neighbourChannel))
                                                                        .collect(Collectors.toSet())
                                                        )
                                                )
                                )
                        ); 
                double maxSpread;
                try {
                    maxSpread = encodeAndSolveAsLinearProgram(domain, channelsThatANeighbourCanBlockOut, domains);
                } catch (IloException e) {
                    throw new RuntimeException(e);
                }
                log.trace("Max spread is upper bounded at {}, and the domain is of size {}", maxSpread, domain.size());
                if ((int) (maxSpread + 0.5) < domain.size()) {
                    log.info("Station {} is underconstrained as it has {} domain channels, but the {} neighbouring interfering stations can only spread to a max of {} of them", station, domain.size(), neighbours.size(), maxSpread);
                    underconstrainedStations.add(station);
                }
            }
        }
        log.debug("Found {} underconstrained stations", underconstrainedStations.size());
        return underconstrainedStations;
    }

    public double trueIP(
    		Station theStation,
    		Set<Integer> domain,
            Map<Station, Map<Integer, Set<Integer>>> channels,
            Map<Station, Set<Integer>> domains) throws IloException {

        IloCplex cplex = new IloCplex();
        cplex.setOut(new NullOutputStream());

        final List<Integer> domainAsList = new ArrayList<>(domain);
        final IloLinearIntExpr[] bucketTerms = new IloLinearIntExpr[domain.size()];
        for (int i = 0; i < bucketTerms.length; i++) {
        	bucketTerms[i] = cplex.linearIntExpr();
        }

        // Create all of the variables
        final Map<Station, Map<Integer, IloIntVar>> stationToChannelToVar = new HashMap<>();
        for (Entry<Station, Set<Integer>> entry : domains.entrySet()) {
            final Station station = entry.getKey();
            if (station.equals(theStation)) {
            	continue;
            }
        	stationToChannelToVar.put(station, new HashMap<>());
        	for (Integer c : entry.getValue()) {
                final IloIntVar iloIntVar = cplex.boolVar();
                stationToChannelToVar.get(station).put(c, iloIntVar); // sjk in 0, 1
                if (channels.containsKey(station)) { // neighbour
                    for (Entry<Integer, Set<Integer>> innerEntry : channels.get(station).entrySet()) {
                        for (int chan : innerEntry.getValue()) {
                            bucketTerms[domainAsList.indexOf(chan)].addTerm(1, iloIntVar);
                        }
                    }
                }
        	}
            final Collection<IloIntVar> stationVars = stationToChannelToVar.get(station).values();
            cplex.addEq(cplex.sum(stationVars.toArray(new IloNumVar[stationVars.size()])), 1); // sum sjk is le 1
        }
        
        for (Constraint constraint : constraintManager.getAllRelevantConstraints(domains)) { // Don't violate constraints
            Map<Integer, IloIntVar> channelToVarSource = stationToChannelToVar.get(constraint.getSource());
            Map<Integer, IloIntVar> channelToVarTarget = stationToChannelToVar.get(constraint.getTarget());
            if (channelToVarSource != null && channelToVarTarget != null) {
                IloIntVar sourceSjk = channelToVarSource.get(constraint.getSourceChannel());
                IloIntVar targetSjk = channelToVarTarget.get(constraint.getTargetChannel());
                if (sourceSjk != null && targetSjk != null) {
                     cplex.addLe(cplex.sum(new IloNumVar[] {sourceSjk, targetSjk}), 1);
                }
            }
        }

        final IloNumExpr[] mins = new IloNumVar[domain.size()];
        for (int i = 0; i < domain.size(); i++) {
            mins[i] = cplex.min(1, bucketTerms[i]);
        }
        cplex.addMaximize(cplex.sum(mins));

        // Solve the MIP.
        final boolean feasible = cplex.solve();
        // log.info("Solution status = " + cplex.getStatus());
        Double d = null;
        if (feasible) {
        	// TODO: NUMERICAL ACCURACY IS A REAL ISSUE
        	log.info("Solution is {}, and quality is {}", cplex.getObjValue(), cplex.getStatus());
            d = cplex.getObjValue();
            log.trace("Sum is {}", d);
        } else {
            log.info("MIP is infeasible");
        }
        cplex.end();
        return d != null ? d : Double.MAX_VALUE;
    }

    public double encodeAndSolveAsLinearProgram(Set<Integer> domain,
                                                Map<Station, Map<Integer, Set<Integer>>> channels,
                                                Map<Station, Set<Integer>> domains) throws IloException {
        IloCplex cplex = new IloCplex();
        cplex.setOut(new NullOutputStream());
        final List<Integer> domainAsList = new ArrayList<>(domain);
        IloLinearNumExpr[] constraints = new IloLinearNumExpr[domain.size()];
        for (int i = 0; i < domain.size(); i++) {
            constraints[i] = cplex.linearNumExpr();
        }
        final Map<Station, Map<Integer, IloNumVar>> stationToChannelToVar = new HashMap<>();
        for (Entry<Station, Map<Integer, Set<Integer>>> entry : channels.entrySet()) {
            final Station station = entry.getKey();
            stationToChannelToVar.put(station, new HashMap<>());
            Collection<Entry<Integer, Set<Integer>>> c = entry.getValue().entrySet();
            int size = c.size();
            IloNumVar[] sjk = exact ? cplex.boolVarArray(size) : cplex.numVarArray(size, 0, 1.0);
            final Iterator<Entry<Integer, Set<Integer>>> iterator = c.iterator();
            int m = 0;
            while (iterator.hasNext()) {
                Entry<Integer, Set<Integer>> c2 = iterator.next();
                stationToChannelToVar.get(station).put(c2.getKey(), sjk[m]);
                for (Integer i : c2.getValue()) {
                    int ind = domainAsList.indexOf(i);
                    constraints[ind].addTerm(1.0, sjk[m]);
                }
                m++;
            }
            cplex.addEq(cplex.sum(sjk), 1.0);
        }
        IloNumVar[] uArray = exact ? cplex.boolVarArray(domain.size()) : cplex.numVarArray(domain.size(), 0, 1);
        IloNumVar[] qArray = exact ? cplex.intVarArray(domain.size(), -Integer.MAX_VALUE, 0) : cplex.numVarArray(domain.size(), -Double.MAX_VALUE, 0);
        final double epsilon = 10e-3;
        final IloLinearNumExpr obj = cplex.linearNumExpr();
        final double[] uCoeffs = new double[uArray.length];
        Arrays.fill(uCoeffs, 1);
        obj.addTerms(uArray, uCoeffs);
        final double[] qCoeffs = new double[qArray.length];
        Arrays.fill(uCoeffs, epsilon);
        obj.addTerms(qArray, qCoeffs);
        cplex.addMaximize(obj);
        for (int i = 0; i < domainAsList.size(); i++) {
            IloNumVar u = uArray[i];
            IloNumVar q = qArray[i];
            IloLinearNumExpr justu = cplex.linearNumExpr();
            justu.addTerm(1.0, u);
            IloLinearNumExpr qSide = cplex.linearNumExpr();
            qSide.addTerm(1.0, q);
            qSide.add(constraints[i]);
            cplex.addEq(qSide, justu);
        }

        // TODO: these don't seem to be doing anything
        for (Constraint constraint : constraintManager.getAllRelevantConstraints(domains)) {
            Map<Integer, IloNumVar> channelToVarSource = stationToChannelToVar.get(constraint.getSource());
            Map<Integer, IloNumVar> channelToVarTarget = stationToChannelToVar.get(constraint.getTarget());
            if (channelToVarSource != null && channelToVarTarget != null) {
                IloNumVar sourceSjk = channelToVarSource.get(constraint.getSourceChannel());
                IloNumVar targetSjk = channelToVarTarget.get(constraint.getTargetChannel());
                if (sourceSjk != null && targetSjk != null) {
                    final IloLinearNumExpr iloLinearNumExpr = cplex.linearNumExpr();
                    iloLinearNumExpr.addTerm(1.0, sourceSjk);
                    iloLinearNumExpr.addTerm(1.0, targetSjk);
                    cplex.addLe(iloLinearNumExpr, 1.0);
                }
            }
        }

        // Solve the MIP.
        final boolean feasible = cplex.solve();
        // log.info("Solution status = " + cplex.getStatus());
        Double d = null;
        if (feasible) {
            // log.info("Solution value = " + cplex.getObjValue());
            double[] val = cplex.getValues(uArray);
            double sum = Arrays.stream(val).sum();
            d = sum;
            log.trace("Sum is {}", sum);
        } else {
            log.info("Could not solve MIP");
        }
        cplex.end();
        return d != null ? d : Double.MAX_VALUE;
    }

}