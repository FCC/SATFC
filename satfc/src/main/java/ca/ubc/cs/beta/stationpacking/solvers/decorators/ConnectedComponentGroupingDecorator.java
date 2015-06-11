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

import static ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors.toImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SolverHelper;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Created by newmanne on 28/11/14.
 */
@Slf4j
public class ConnectedComponentGroupingDecorator extends ASolverDecorator {

    private final IComponentGrouper fComponentGrouper;
    private final IConstraintManager fConstraintManager;
    private final boolean fSolveEverything;

    /**
     * @param aSolveEverythingForCaching if true, solve every component, even when you know the problem is logically finished. (Used for caching results)
     * @param aComponentGrouper
     */
    public ConnectedComponentGroupingDecorator(ISolver aSolver, IComponentGrouper aComponentGrouper, IConstraintManager aConstraintManager, boolean aSolveEverythingForCaching) {
        super(aSolver);
        fComponentGrouper = aComponentGrouper;
        fConstraintManager = aConstraintManager;
        fSolveEverything = aSolveEverythingForCaching;
    }

    public ConnectedComponentGroupingDecorator(ISolver aSolver, IComponentGrouper aComponentGrouper, IConstraintManager aConstraintManger) {
        this(aSolver, aComponentGrouper, aConstraintManger, false);
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, final ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        log.debug("Solving instance of {}...", aInstance.getInfo());

        // Split into groups
        final Set<Set<Station>> stationComponents = fComponentGrouper.group(aInstance, fConstraintManager);
        SATFCMetrics.postEvent(new SATFCMetrics.TimingEvent(aInstance.getName(), SATFCMetrics.TimingEvent.CONNECTED_COMPONENTS, watch.getElapsedTime()));
        log.debug("Problem separated in {} groups.", stationComponents.size());

        // sort the components in ascending order of size. The idea is that this would decrease runtime if one of the small components was UNSAT
        final List<Set<Station>> sortedStationComponents = stationComponents.stream().sorted((o1, o2) -> Integer.compare(o1.size(), o2.size())).collect(Collectors.toList());
        
        // convert components into station packing problems
        final AtomicInteger componentIndex = new AtomicInteger();
        final List<StationPackingInstance> componentInstances = sortedStationComponents.stream().map(stationComponent -> {
        	componentIndex.incrementAndGet();
            final ImmutableMap<Station, Set<Integer>> subDomains = aInstance.getDomains().entrySet()
                    .stream()
                    .filter(entry -> stationComponent.contains(entry.getKey()))
                    .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
            final Map<Station, Integer> previousAssignment = aInstance.getPreviousAssignment();
            final String name = aInstance.getName() + "_component" + componentIndex;
            // update name to include a component prefix
            Map<String, Object> metadata = new HashMap<>(aInstance.getMetadata());
            metadata.put(StationPackingInstance.NAME_KEY, name);
            return new StationPackingInstance(subDomains, previousAssignment, metadata);
        }).collect(GuavaCollectors.toImmutableList());

        SATFCMetrics.postEvent(new SATFCMetrics.SplitIntoConnectedComponentsEvent(aInstance.getName(), componentInstances));

        final AtomicInteger idTracker = new AtomicInteger();
        final Map<Integer, SolverResult> solverResults = Maps.newLinkedHashMap();
        watch.stop();
        componentInstances.stream()
            // Note that anyMatch is a short-circuiting operation
            // If any component matches this clause (is not SAT), the whole instance cannot be SAT, might as well stop then
            .anyMatch(stationComponent -> {
                int id = idTracker.getAndIncrement();
                log.debug("Solving component {}...", id);
                log.debug("Component {} has {} stations.", id, stationComponent.getStations().size());
                final SolverResult componentResult = fDecoratedSolver.solve(stationComponent, aTerminationCriterion, aSeed);
                SATFCMetrics.postEvent(new SATFCMetrics.InstanceSolvedEvent(stationComponent.getName(), componentResult.getResult(), componentResult.getRuntime()));
                solverResults.put(id, componentResult);
                return !componentResult.getResult().equals(SATResult.SAT) && !fSolveEverything;
            });
        SolverResult result = SolverHelper.mergeComponentResults(solverResults.values());
        result = SolverResult.addTime(result, watch.getElapsedTime());
        SATFCMetrics.postEvent(new SATFCMetrics.SolvedByEvent(aInstance.getName(), SATFCMetrics.SolvedByEvent.CONNECTED_COMPONENTS, result.getResult()));

        if (result.getResult() == SATResult.SAT)
        {
            Preconditions.checkState(solverResults.size() == stationComponents.size(), "Determined result was SAT without looking at every component!");
        }
        log.debug("Result:" + System.lineSeparator() + result.toParsableString());
        return result;
    }

}
