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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.underconstrained.IUnderconstrainedStationFinder;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * Solver decorator that removes underconstrained stations from the instance, solve the sub-instance and then add back
 * the underconstrained stations using simply looping over all the station's domain channels.
 * <p/>
 * A station is underconstrained if no matter what channels the other stations are assigned to, the station will always have a feasible
 * channel to be packed on.
 *
 * @author afrechet
 */
@Slf4j
public class UnderconstrainedStationRemoverSolverDecorator extends ASolverDecorator {

    private final IUnderconstrainedStationFinder underconstrainedStationFinder;
    private final IConstraintManager constraintManager;
    private final boolean recurse;

    public UnderconstrainedStationRemoverSolverDecorator(ISolver aSolver, IConstraintManager constraintManager, IUnderconstrainedStationFinder underconstrainedStationFinder, boolean recurse) {
        super(aSolver);
        this.underconstrainedStationFinder = underconstrainedStationFinder;
        this.constraintManager = constraintManager;
        this.recurse = recurse;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        return solve(aInstance, aTerminationCriterion, aSeed, aInstance.getStations());
    }

    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed, Set<Station> stationsToCheck) {
        Watch watch = Watch.constructAutoStartWatch();
        final Map<Station, Set<Integer>> domains = aInstance.getDomains();
        if (aTerminationCriterion.hasToStop()) {
            log.debug("All time spent.");
            return SolverResult.createTimeoutResult(watch.getElapsedTime());
        }
        final Set<Station> underconstrainedStations = underconstrainedStationFinder.getUnderconstrainedStations(domains, aTerminationCriterion, stationsToCheck);
        SATFCMetrics.postEvent(new SATFCMetrics.UnderconstrainedStationsRemovedEvent(aInstance.getName(), underconstrainedStations));
        if (aTerminationCriterion.hasToStop()) {
            log.debug("All time spent.");
            return SolverResult.createTimeoutResult(watch.getElapsedTime());
        }

        log.debug("Removing {} underconstrained stations...", underconstrainedStations.size());

        //Remove the nodes from the instance.
        final Map<Station, Set<Integer>> alteredDomains = new HashMap<>(domains);
        alteredDomains.keySet().removeAll(underconstrainedStations);

        final SolverResult subResult;
        final double preTime;
        if (!alteredDomains.isEmpty()) {
            StationPackingInstance alteredInstance = new StationPackingInstance(alteredDomains, aInstance.getPreviousAssignment(), aInstance.getMetadata());
            preTime = watch.getElapsedTime();
            log.trace("{} s spent on underconstrained pre-solving setup.", preTime);
            if (recurse && !underconstrainedStations.isEmpty()) {
                log.debug("Going one layer deeper with underconstrained station removal");
                // You only need to recheck a station that might be underconstrained because some of his neigbhours have disappeared
                final SimpleGraph<Station, DefaultEdge> constraintGraph = ConstraintGrouper.getConstraintGraph(domains, constraintManager);
                final NeighborIndex<Station, DefaultEdge> neighborIndex = new NeighborIndex<>(constraintGraph);
                final Set<Station> stationsToRecheck = underconstrainedStations.stream().map(neighborIndex::neighborsOf).flatMap(Collection::stream).filter(s -> !underconstrainedStations.contains(s)).collect(Collectors.toSet());
                subResult = solve(alteredInstance, aTerminationCriterion, aSeed, stationsToRecheck);
            } else { // we bottomed out
                //Solve the reduced instance.
                SATFCMetrics.postEvent(new SATFCMetrics.TimingEvent(aInstance.getName(), SATFCMetrics.TimingEvent.FIND_UNDERCONSTRAINED_STATIONS, watch.getElapsedTime()));
                log.debug("Solving the sub-instance...");
                subResult = fDecoratedSolver.solve(alteredInstance, aTerminationCriterion, aSeed);
            }
        } else {
            log.debug("All stations were underconstrained!");
            preTime = watch.getElapsedTime();
            log.trace("{} s spent on underconstrained pre-solving setup.", preTime);
            subResult = new SolverResult(SATResult.SAT, 0.0, new HashMap<>(), SolvedBy.UNDERCONSTRAINED);
        }

        if (subResult.getResult().equals(SATResult.SAT)) {
            log.debug("Sub-instance is packable, adding back the underconstrained stations...");
            //If satisfiable, find a channel for the under constrained nodes that were removed by brute force through their domain.
            final Map<Integer, Set<Station>> assignment = subResult.getAssignment();
            final Map<Integer, Set<Station>> alteredAssignment = new HashMap<Integer, Set<Station>>(assignment);

            for (Station station : underconstrainedStations) {

                boolean stationAdded = false;

                Set<Integer> domain = domains.get(station);
                //Try to add the underconstrained station at one of its channel domain.
                log.trace("Trying to add back underconstrained station {} on its domain {} ...", station, domain);

                for (Integer channel : domain) {
                    log.trace("Checking domain channel {} ...", channel);
                    if (!alteredAssignment.containsKey(channel)) {
                        alteredAssignment.put(channel, new HashSet<Station>());
                    }

                    alteredAssignment.get(channel).add(station);
                    final boolean addedSAT = constraintManager.isSatisfyingAssignment(alteredAssignment);

                    if (addedSAT) {
                        log.trace("Added on channel {}.", channel);
                        stationAdded = true;
                        break;
                    } else {
                        alteredAssignment.get(channel).remove(station);
                        if (alteredAssignment.get(channel).isEmpty()) {
                            alteredAssignment.remove(channel);
                        }
                    }
                }
                if (!stationAdded) {
                    throw new IllegalStateException("Could not add unconstrained station " + station + " on any of its domain channels.");
                }
            }
            return new SolverResult(SATResult.SAT, watch.getElapsedTime(), alteredAssignment, subResult.getSolvedBy());
        } else {
            log.debug("Sub-instance was not satisfiable, no need to consider adding back underconstrained stations.");
            //Not satisfiable, so re-adding the underconstrained nodes cannot change anything.
            return SolverResult.relabelTime(subResult, watch.getElapsedTime());
        }
    }

}
