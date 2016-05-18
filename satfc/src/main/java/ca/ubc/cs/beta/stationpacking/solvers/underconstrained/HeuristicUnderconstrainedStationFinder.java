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
package ca.ubc.cs.beta.stationpacking.solvers.underconstrained;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 1/8/15.
 *
 * The goal is to find stations for which, no matter how their neighbours are arranged, there will always be a channel
 * to put them onto. Then, you can remove them from the problem, and simply add them back afterwards by iterating
 * through their domain until you find a satisfying assignment.
 *
 * One helpful framework for thinking about this problem is to think of the question as:
 * If all of my neighbours were placed adversarially to block out the maximum number of channels from my domain,
 * how many could they block out? If the answer is less than my domain's size, then I am underconstrained.
 *
 * For example,
 * Neighbour A can block out {1, 2} or {2, 3}
 * Neighbour B can block out {2} or {2, 3}
 * Then the worst case is when neighbour A blocks out {1,2} and neighbour B blocks out {2,3}
 *
 * Slightly more formally:
 * There are N groups of sets
 * You have to choose exactly one set from each group
 * Your goal is to maximize the size of the union of the groups that you choose
 * (Note that we don't need the actual values of the choices, just the size)
 *
 * This problem seems to be a variant of the Maximum Coverage Problem
 *
 * We do not solve the problem exactly, but rely instead take the minimum of two heuristics that are upper bounds to this question
 * 1) The size of the union of all the sets in every group
 * 2) The sum of the sizes of the largest set in every group
 */
@Slf4j
public class HeuristicUnderconstrainedStationFinder implements IUnderconstrainedStationFinder {

    private final IConstraintManager constraintManager;
    private final boolean performExpensiveAnalysis;

    public HeuristicUnderconstrainedStationFinder(IConstraintManager constraintManager, boolean performExpensiveAnalysis) {
        this.constraintManager = constraintManager;
        this.performExpensiveAnalysis = performExpensiveAnalysis;
    }

    @Override
    public Set<Station> getUnderconstrainedStations(Map<Station, Set<Integer>> domains, ITerminationCriterion criterion, Set<Station> stationsToCheck) {
        final Set<Station> underconstrainedStations = new HashSet<>();

        log.debug("Finding underconstrained stations in the instance...");

        /**
         * Heuristic #1 for underconstrained:
         * Take the union of all the channels that my neighbours can block and see if its smaller than my domain
         */
        final HashMultimap<Station, Integer> badChannels = HashMultimap.create();
        constraintManager.getAllRelevantConstraints(domains).forEach(constraint -> {
            badChannels.put(constraint.getSource(), constraint.getSourceChannel());
            badChannels.put(constraint.getTarget(), constraint.getTargetChannel());
        });

        final NeighborIndex<Station, DefaultEdge> neighborIndex = new NeighborIndex<>(ConstraintGrouper.getConstraintGraph(domains, constraintManager));

        for (final Station station : stationsToCheck) {
            if (criterion.hasToStop()) {
                log.debug("Underconstrained stations timed out. Returned set will be only a partial set");
                break;
            }
            final Set<Integer> domain = domains.get(station);

            final Set<Integer> stationBadChannels = badChannels.get(station);
            final Set<Integer> stationGoodChannels = Sets.difference(domain, stationBadChannels);

            log.trace("Station {} domain channels: {}.", station, domain);
            log.trace("Station {} bad channels: {}.", station, stationBadChannels);

            if (!stationGoodChannels.isEmpty()) {
                log.trace("Station {} is underconstrained as it has {} domain channels ({}) on which it interferes with no one.", station, stationGoodChannels.size(), stationGoodChannels);
                underconstrainedStations.add(station);
                continue;
            }

            if (performExpensiveAnalysis) {
                /*
                 * Heuristic #2 for underconstrained:
                 * For each of my neighbours, count the maximum number of channels in my domain that each neighbour can potentially "block" out. Then assume each neighbour does block out this maximal number of channels. Would I still have a channel left over?
                 */
                final Set<Station> neighbours = neighborIndex.neighborsOf(station);
                if (neighbours.size() >= domain.size()) {
                    log.trace("Station {} has {} neighbours but only {} channels, so the channel counting heuristic will not work", station, neighbours.size(), domain.size());
                    continue;
                }
                final long interferingStationsMaxChannelSpread = neighbours.stream() // for each neighbour
                        .mapToLong(neighbour -> domains.get(neighbour).stream() // for each channel in the neighbour's domain
                                        .mapToLong(neighbourChannel -> domain.stream() // for each of my channel's
                                                .filter(myChannel -> !constraintManager.isSatisfyingAssignment(station, myChannel, neighbour, neighbourChannel))
                                                .count() // count the number of my channels that would be invalid if my neighbour were assigned to neighbourChannel
                                        )
                                        .max() // max over all neighbour's channels
                                        .getAsLong()
                        )
                        .sum();

                if (interferingStationsMaxChannelSpread < domain.size()) {
                    log.trace("Station {} is underconstrained as it has {} domain channels, but the neighbouring interfering stations can only spread to a max of {} of them", station, domain.size(), interferingStationsMaxChannelSpread);
                    underconstrainedStations.add(station);
                }
            }
        }

        return underconstrainedStations;
    }


}