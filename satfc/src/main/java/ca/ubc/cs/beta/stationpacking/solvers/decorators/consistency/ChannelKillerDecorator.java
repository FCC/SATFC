package ca.ubc.cs.beta.stationpacking.solvers.decorators.consistency;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult.SolvedBy;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ASolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.collect.ImmutableSet;

/**
 * Created by newmanne on 10/08/15.
 */
@Slf4j
public class ChannelKillerDecorator extends ASolverDecorator {

    private final double subProblemCutoff;
    private final ISolver SATSolver;
    private final IConstraintManager constraintManager;

    public ChannelKillerDecorator(ISolver aSolver, ISolver SATSolver, IConstraintManager constraintManager, double subProblemCutoff) {
        super(aSolver);
        this.SATSolver = SATSolver;
        this.constraintManager = constraintManager;
        this.subProblemCutoff = subProblemCutoff;
    }

    public ChannelKillerDecorator(ISolver aSolver, ISolver SATSolver, IConstraintManager constraintManager) {
        this(aSolver, SATSolver, constraintManager, 0.01);
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();
        // Deep copy map
        final Map<Station, Set<Integer>> domainsCopy = aInstance.getDomains().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashSet<>(entry.getValue())));
        final SimpleGraph<Station, DefaultEdge> constraintGraph = ConstraintGrouper.getConstraintGraph(domainsCopy, constraintManager);
        final NeighborIndex<Station, DefaultEdge> neighborIndex = new NeighborIndex<>(constraintGraph);
        final LinkedHashSet<Station> stationQueue = new LinkedHashSet<>(aInstance.getStations());
        int numChannelsRemoved = 0;
        int numTimeouts = 0;
        final Set<Station> changedStations = new HashSet<>();
        while (!stationQueue.isEmpty()) {
            final Station station = stationQueue.iterator().next();
            final Set<Integer> domain = domainsCopy.get(station);
            log.info("Beginning station {} with domain {}", station, domain);
            final Set<Station> neighbours = neighborIndex.neighborsOf(station);
            final Map<Station, Set<Integer>> neighbourDomains = neighbours.stream().collect(Collectors.toMap(Function.identity(), domainsCopy::get));
            final Set<Integer> SATChannels = new HashSet<>();
            final Set<Integer> UNSATChannels = new HashSet<>();
            boolean changed = false;
            for (int channel : domain) {
                if (SATChannels.contains(channel)) {
                    log.trace("Channel {} is already known to be SAT, skipping...", channel);
                    continue;
                }
                neighbourDomains.put(station, ImmutableSet.of(channel));
                final StationPackingInstance reducedInstance = new StationPackingInstance(neighbourDomains);
                final ITerminationCriterion subCriterion = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(aTerminationCriterion, new WalltimeTerminationCriterion(subProblemCutoff)));
                final SolverResult subResult = SATSolver.solve(reducedInstance, subCriterion, aSeed);
                if (subResult.getResult().equals(SATResult.UNSAT)) {
                    log.debug("Station {} on channel {} is UNSAT with its neigbhours, removing channel!", station, channel);
                    UNSATChannels.add(channel);
                    numChannelsRemoved++;
                    changed = true;
                    changedStations.add(station);
                } else if (subResult.getResult().equals(SATResult.SAT)) {
                    // What other channels would have also satisfied this assignment? We can skip those
                    SATChannels.add(channel);
                    final Set<Integer> unknownChannels = domain.stream().filter(c -> !SATChannels.contains(c) && !UNSATChannels.contains(c)).collect(Collectors.toSet());
                    final Map<Integer, Set<Station>> mutableAssignment = subResult.getAssignment().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashSet<>(entry.getValue())));
                    mutableAssignment.get(channel).remove(station);
                    unknownChannels.stream().forEach(unknownChannel -> {
                        mutableAssignment.putIfAbsent(unknownChannel, new HashSet<>());
                        mutableAssignment.get(unknownChannel).add(station);
                        if (constraintManager.isSatisfyingAssignment(mutableAssignment)) {
                            log.trace("No need to check channel {} for station {} because it also a SAT to the previously checked problem", unknownChannel, station);
                            SATChannels.add(unknownChannel);
                        }
                        mutableAssignment.get(unknownChannel).remove(station);
                        if (mutableAssignment.get(unknownChannel).isEmpty()) {
                            mutableAssignment.remove(unknownChannel);
                        }
                    });
                } else {
                    if (subResult.getResult().equals(SATResult.TIMEOUT)) {
                        numTimeouts++;
                    }
                    log.trace("Sub result was {}", subResult.getResult());
                }
                neighbourDomains.remove(station);
            }
            domain.removeAll(UNSATChannels);
            log.info("Done with station {}, now with domain {}", station, domain);
            if (domain.isEmpty()) {
                log.debug("Station {} has an empty domain, instance is UNSAT", station);
                return SolverResult.createNonSATResult(SATResult.UNSAT, watch.getElapsedTime(), SolvedBy.CHANNEL_KILLER);
            } else if (changed) {
                // re-enqueue all neighbors
                stationQueue.addAll(neighborIndex.neighborsOf(station));
            }
            stationQueue.remove(station);
        }
        log.info("Removed {} channels from {} stations, had {} timeouts", numChannelsRemoved, changedStations.size(), numTimeouts);
        final StationPackingInstance reducedInstance = new StationPackingInstance(domainsCopy, aInstance.getPreviousAssignment(), aInstance.getMetadata());
        // TODO: time is wrong
        return fDecoratedSolver.solve(reducedInstance, aTerminationCriterion, aSeed);
    }

    @Override
    public void notifyShutdown() {
        super.notifyShutdown();
        SATSolver.notifyShutdown();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        SATSolver.interrupt();
    }
}
