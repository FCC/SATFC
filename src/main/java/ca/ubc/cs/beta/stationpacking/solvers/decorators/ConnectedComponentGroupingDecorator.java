package ca.ubc.cs.beta.stationpacking.solvers.decorators;

import static ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors.toImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ca.ubc.cs.beta.stationpacking.metrics.InstanceInfo;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SolverHelper;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Created by newmanne on 28/11/14.
 */

public class ConnectedComponentGroupingDecorator extends ASolverDecorator {

    private static Logger log = LoggerFactory.getLogger(ConnectedComponentGroupingDecorator.class);

    private final IComponentGrouper fComponentGrouper;
    private final IConstraintManager fConstraintManager;

    /**
     * @param aSolver           - decorated ISolver.
     * @param aComponentGrouper
     */
    public ConnectedComponentGroupingDecorator(ISolver aSolver, IComponentGrouper aComponentGrouper, IConstraintManager aConstraintManager) {
        super(aSolver);
        fComponentGrouper = aComponentGrouper;
        fConstraintManager = aConstraintManager;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, final ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        log.debug("Solving instance of {}...", aInstance.getInfo());

        // Split into groups
        final Set<Set<Station>> stationComponents = fComponentGrouper.group(aInstance, fConstraintManager);
        // sort the components in ascending order of size. The idea is that this would decrease runtime if one of the small components was UNSAT
        final List<Set<Station>> sortedStationComponents = stationComponents.stream().sorted((o1, o2) -> Integer.compare(o1.size(), o2.size())).collect(Collectors.toList());
        premetrics(sortedStationComponents);
        log.debug("Problem separated in {} groups.", stationComponents.size());

        final AtomicInteger idTracker = new AtomicInteger();
        final Map<Integer, SolverResult> solverResults = Maps.newLinkedHashMap();
        sortedStationComponents.stream()
            // Note that anyMatch is a short-circuiting operation
            // If any component matches this clause (is not SAT), the whole instance cannot be SAT, might as well stop then
            .anyMatch(stationComponent -> {
                int id = idTracker.getAndIncrement();
                log.debug("Solving component {}...", id);
                final ImmutableMap<Station, Set<Integer>> subDomains = aInstance.getDomains().entrySet()
                        .stream()
                        .filter(entry -> stationComponent.contains(entry.getKey()))
                        .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
                log.debug("Component {} has {} stations.", id, subDomains.size());
                final Map<Station, Integer> previousAssignment = aInstance.getPreviousAssignment();
                final StationPackingInstance componentInstance = new StationPackingInstance(subDomains, previousAssignment);
                final SolverResult componentResult = fDecoratedSolver.solve(componentInstance, aTerminationCriterion, aSeed);
                solverResults.put(id, componentResult);
                return !componentResult.getResult().equals(SATResult.SAT);
            });
        watch.stop();
        postmetrics(solverResults);
        final SolverResult result = SolverHelper.mergeComponentResults(solverResults.values(), watch.getElapsedTime());
        
        if (result.getResult() == SATResult.SAT)
        {
            Preconditions.checkState(solverResults.size() == stationComponents.size(), "Determined result was SAT without looking at every component!");
        }
        log.debug("\nResult:");
        log.debug(result.toParsableString());
        return result;
    }

    private void postmetrics(Map<Integer, SolverResult> solverResults) {
        final InstanceInfo outermostInstance = SATFCMetrics.getMostRecentOutermostInstanceInfo();
        for (int i = 0; i < solverResults.size(); i++) {
            final InstanceInfo componentInfo = outermostInstance.getComponents().get(i);
            componentInfo.setResult(solverResults.get(i).getResult());
            componentInfo.setRuntime(solverResults.get(i).getRuntime());
        }
    }

    private void premetrics(List<Set<Station>> stationComponents) {
        int i = 1;
        final InstanceInfo outermostInstance = SATFCMetrics.getMostRecentOutermostInstanceInfo();
        for (Set<Station> component : stationComponents) {
            final InstanceInfo instanceInfo = new InstanceInfo();
            instanceInfo.setName(outermostInstance.getName() + "_component" + Integer.toString(i));
            instanceInfo.setNumStations(component.size());
            instanceInfo.setStations(component.stream().map(Station::getID).collect(Collectors.toSet()));
            outermostInstance.getComponents().add(instanceInfo);
            i++;
        }
    }
}
