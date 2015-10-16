package ca.ubc.cs.beta.stationpacking.consistency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.infinite.NeverEndingTerminationCriterion;

/**
* Created by newmanne on 10/06/15.
*/
@Slf4j
public class AC3Enforcer {

    private final IConstraintManager constraintManager;

    public AC3Enforcer(IConstraintManager constraintManager) {
        this.constraintManager = constraintManager;
    }

    /**
     * Enforces arc consistency using AC3 (see https://en.wikipedia.org/wiki/AC-3_algorithm)
     * Will fail at the first indication of inconsistency.
     */
    public AC3Output AC3(StationPackingInstance instance, ITerminationCriterion criterion) {
        // Deep copy map
        final Map<Station, Set<Integer>> reducedDomains = instance.getDomains().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashSet<>(entry.getValue())));
        final AC3Output output = new AC3Output(reducedDomains);
        final NeighborIndex<Station, DefaultEdge> neighborIndex = new NeighborIndex<>(ConstraintGrouper.getConstraintGraph(instance.getDomains(), constraintManager));
        final LinkedBlockingQueue<Pair<Station, Station>> workList = getInterferingStationPairs(neighborIndex, instance);
        while (!workList.isEmpty()) {
            if (criterion.hasToStop()) {
                log.debug("AC3 timed out");
                output.setTimedOut(true);
                return output;
            }
            final Pair<Station, Station> pair = workList.poll();
            if (removeInconsistentValues(pair, output)) {
                final Station referenceStation = pair.getLeft();
                if (reducedDomains.get(referenceStation).isEmpty()) {
                    log.debug("Reduced a domain to empty! Problem is solved UNSAT");
                    output.setNoSolution(true);
                    return output;
                } else {
                    reenqueueAllAffectedPairs(workList, pair, neighborIndex);
                }
            }
        }
        return output;
    }

    public AC3Output AC3(StationPackingInstance instance) {
        return AC3(instance, new NeverEndingTerminationCriterion());
    }

    private void reenqueueAllAffectedPairs(Queue<Pair<Station, Station>> interferingStationPairs,
                                           Pair<Station, Station> modifiedPair, NeighborIndex<Station, DefaultEdge> neighborIndex) {
        final Station x = modifiedPair.getLeft();
        final Station y = modifiedPair.getRight();

        neighborIndex.neighborsOf(x).stream().filter(neighbor -> !neighbor.equals(y)).forEach(neighbor -> {
            interferingStationPairs.add(Pair.of(neighbor, x));
        });
    }

    private LinkedBlockingQueue<Pair<Station, Station>> getInterferingStationPairs(NeighborIndex<Station, DefaultEdge> neighborIndex, StationPackingInstance instance) {
        final LinkedBlockingQueue<Pair<Station, Station>> workList = new LinkedBlockingQueue<>();
        for (Station referenceStation : instance.getStations()) {
            for (Station neighborStation : neighborIndex.neighborsOf(referenceStation)) {
                workList.add(Pair.of(referenceStation, neighborStation));
            }
        }
        return workList;
    }

    /**
     * @return true if x's domain changed
     */
    private boolean removeInconsistentValues(Pair<Station, Station> pair, AC3Output output) {
        final Map<Station, Set<Integer>> domains = output.getReducedDomains();
        final Station x = pair.getLeft();
        final Station y = pair.getRight();
        final List<Integer> xValuesToPurge = new ArrayList<>();
        for (int vx : domains.get(x)) {
            if (channelViolatesArcConsistency(x, vx, y, domains.get(y))) {
                log.debug("Purging channel {} from station {}'s domain", vx, x.getID());
                output.setNumReducedChannels(output.getNumReducedChannels() + 1);
                xValuesToPurge.add(vx);
            }
        }
        return domains.get(x).removeAll(xValuesToPurge);
    }

    private boolean channelViolatesArcConsistency(Station x, int vx, Station y, Set<Integer> yDomain) {
        return yDomain.stream().noneMatch(vy -> constraintManager.isSatisfyingAssignment(x, vx, y, vy));
    }

}
