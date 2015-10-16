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
package ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies.IStationPackingConfigurationStrategy;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies.StationPackingConfiguration;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ASolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

/**
 * Pre-solve by applying a sequence of station subsets certifiers based on
 * the neighborhood of stations missing from a problem instance's previous assignment.
 * <p/>
 * If an UNSAT result is found for the immediate local neighborhood of the missing stations, the search
 * is expanded to include the neighbors' neighbors. This is iterated until any of the following conditions is met:
 * <ol>
 * <li>A SAT result is found</li>
 * <li>The expanded neighborhood contains all the stations connected (directly or indirectly) to the original
 * missing stations</li>
 * <li>The search times out</li>
 * </ol>
 *
 * @author afrechet
 * @author pcernek
 */
@Slf4j
public class ConstraintGraphNeighborhoodPresolver extends ASolverDecorator {

    private final IStationSubsetCertifier fCertifier;
    private final IStationPackingConfigurationStrategy fStationAddingStrategy;
    private final IConstraintManager constraintManager;

    /**
     * @param aCertifier             -the certifier to use to evaluate the satisfiability of station subsets.
     * @param aStationAddingStrategy - determines which stations to fix / unfix, and how long to attempt at each expansion
     */
    public ConstraintGraphNeighborhoodPresolver(ISolver decoratedSolver, IStationSubsetCertifier aCertifier, IStationPackingConfigurationStrategy aStationAddingStrategy, IConstraintManager constraintManager) {
        super(decoratedSolver);
        this.fCertifier = aCertifier;
        this.fStationAddingStrategy = aStationAddingStrategy;
        this.constraintManager = constraintManager;
    }

    @Override
    public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion, long aSeed) {
        final Watch watch = Watch.constructAutoStartWatch();

        if (aInstance.getPreviousAssignment().isEmpty()) {
            log.debug("No previous assignment given! Nothing to do here...");
            return fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed);
        }

        final Set<Station> stationsWithNoPreviousAssignment = getStationsNotInPreviousAssignment(aInstance);
        log.debug("There are {} stations that are not part of previous assignment.", stationsWithNoPreviousAssignment.size());

        SolverResult result = null;
        final SimpleGraph<Station, DefaultEdge> constraintGraph = ConstraintGrouper.getConstraintGraph(aInstance.getDomains(), constraintManager);
        for (final StationPackingConfiguration configuration : fStationAddingStrategy.getConfigurations(constraintGraph, stationsWithNoPreviousAssignment)) {
            if (aTerminationCriterion.hasToStop()) {
                log.debug("All time spent.");
                return SolverResult.createTimeoutResult(watch.getElapsedTime());
            }
            log.debug("Configuration is {} stations to pack, and {} seconds cutoff", configuration.getPackingStations().size(), configuration.getCutoff());
            final ITerminationCriterion criterion = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(aTerminationCriterion, new WalltimeTerminationCriterion(configuration.getCutoff())));

            result = fCertifier.certify(aInstance, configuration.getPackingStations(), criterion, aSeed);

            if (result.getResult().isConclusive()) {
                log.debug("Conclusive result from certifier");
                break;
            }
        }

        if (result == null || !result.isConclusive()) {
            log.debug("Ran out of of configurations to try and no conclusive results. Passing onto next decorator...");
            result = SolverResult.relabelTime(fDecoratedSolver.solve(aInstance, aTerminationCriterion, aSeed), watch.getElapsedTime());
        } else {
            result = SolverResult.relabelTime(result, watch.getElapsedTime());
        }

        log.debug("Result:" + System.lineSeparator() + result.toParsableString());

        return result;
    }

    private Set<Station> getStationsNotInPreviousAssignment(StationPackingInstance aInstance) {
        return aInstance.getStations().stream().filter(station -> !aInstance.getPreviousAssignment().containsKey(station)).collect(Collectors.toSet());
    }

    @Override
    public void notifyShutdown() {
        super.notifyShutdown();
        fCertifier.notifyShutdown();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        fCertifier.interrupt();
    }

}
