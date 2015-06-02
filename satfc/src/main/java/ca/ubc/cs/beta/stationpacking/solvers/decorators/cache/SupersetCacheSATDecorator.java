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
package ca.ubc.cs.beta.stationpacking.solvers.decorators.cache;

import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.ICacher;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATResult;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ASolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
* Created by newmanne on 28/01/15.
*/
@Slf4j
public class SupersetCacheSATDecorator extends ASolverDecorator {

    private final ContainmentCacheProxy proxy;

    public SupersetCacheSATDecorator(ISolver aSolver, ContainmentCacheProxy proxy, ICacher.CacheCoordinate coordinate) {
        super(aSolver);
        this.proxy = proxy;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();

        // test sat cache - supersets of the problem that are SAT directly correspond to solutions to the current problem!
        final SolverResult result;
        log.debug("Sending query to cache");
        final ContainmentCacheSATResult containmentCacheSATResult = proxy.proveSATBySuperset(aInstance, aTerminationCriterion);
        SATFCMetrics.postEvent(new SATFCMetrics.TimingEvent(aInstance.getName(), SATFCMetrics.TimingEvent.FIND_SUPERSET, watch.getElapsedTime()));
        if (containmentCacheSATResult.isValid()) {
            final Map<Integer, Set<Station>> assignment = containmentCacheSATResult.getResult();
            log.debug("Found a superset in the SAT cache - declaring result SAT because of " + containmentCacheSATResult.getKey());
            final Map<Integer, Set<Station>> reducedAssignment = Maps.newHashMap();
            for (Integer channel : assignment.keySet()) {
                assignment.get(channel).stream().filter(station -> aInstance.getStations().contains(station)).forEach(station -> {
                    if (reducedAssignment.get(channel) == null) {
                        reducedAssignment.put(channel, Sets.newHashSet());
                    }
                    reducedAssignment.get(channel).add(station);
                });
            }
            result = new SolverResult(SATResult.SAT, watch.getElapsedTime(), reducedAssignment);
            SATFCMetrics.postEvent(new SATFCMetrics.SolvedByEvent(aInstance.getName(), SATFCMetrics.SolvedByEvent.SUPERSET_CACHE, result.getResult()));
            SATFCMetrics.postEvent(new SATFCMetrics.JustifiedByCacheEvent(aInstance.getName(), containmentCacheSATResult.getKey()));
        } else {
            log.debug("Cache query unsuccessful");
            final double preTime = watch.getElapsedTime();
            final SolverResult decoratedResult = fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
            result = SolverResult.addTime(decoratedResult, preTime);
        }
        return result;
    }

    @Override
    public void interrupt() {
        proxy.interrupt();
        super.interrupt();
    }
}
