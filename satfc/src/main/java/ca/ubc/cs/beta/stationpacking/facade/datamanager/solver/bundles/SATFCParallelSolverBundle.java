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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetUNSATCertifier;
import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.CacherProxy;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories.Clasp3ISolverFactory;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories.ClaspLibraryGenerator;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.ConstraintGraphNeighborhoodPresolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies.AddNeighbourLayerStrategy;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.strategies.IterativeDeepeningConfigurationStrategy;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.composites.ISolverFactory;
import ca.ubc.cs.beta.stationpacking.solvers.composites.ParallelNoWaitSolverComposite;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.AssignmentVerifierDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ConnectedComponentGroupingDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ResultSaverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.UnderconstrainedStationRemoverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.CacheResultDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SubsetCacheUNSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SupersetCacheSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.consistency.ArcConsistencyEnforcerDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.underconstrained.HeuristicUnderconstrainedStationFinder;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

/**
 * Created by newmanne on 14/05/15.
 * SATFC Solver bundle that performs executions in parallel
 */
@Slf4j
public class SATFCParallelSolverBundle extends ASolverBundle {

    public static final int PORTFOLIO_SIZE = 4;

    private final ISolver fUHFSolver;
    private final ISolver fVHFSolver;

    /**
     * Create a SATFC solver bundle.
     *
     * @param aClaspLibraryPath  - library for the clasp to use.
     * @param aStationManager    - station manager.
     * @param aConstraintManager - constraint manager.
     * @param aResultFile        - file to which results should be written (optional).
     */
    public SATFCParallelSolverBundle(
            String aClaspLibraryPath,
            IStationManager aStationManager,
            IConstraintManager aConstraintManager,
            String aResultFile,
            final boolean presolve,
            final boolean decompose,
            final boolean underconstrained,
            final String serverURL,
            int numCores,
            final boolean cacheResults
    ) {
        super(aStationManager, aConstraintManager);
        log.info("Initializing solver with the following solver options: presolve {}, decompose {}, underconstrained {}, serverURL {}", presolve, decompose, underconstrained, serverURL);
        boolean useCache = serverURL != null;
        final SATCompressor aCompressor = new SATCompressor(this.getConstraintManager());
        final Clasp3ISolverFactory clasp3ISolverFactory = new Clasp3ISolverFactory(new ClaspLibraryGenerator(aClaspLibraryPath), aCompressor, getConstraintManager());

        log.debug("SATFC solver bundle.");

        /**
         * Decorate solvers - remember that the decorator that you put first is applied last
         */
        final CacheCoordinate cacheCoordinate = new CacheCoordinate(aStationManager.getDomainHash(), aConstraintManager.getConstraintHash());

        final List<ISolverFactory> parallelUHFSolvers = new ArrayList<>();

        // BEGIN UHF
        // NOTE: The sorted order of this list matters if there are more solver paths than there are cores (i.e. first in list will go first)

        // Presolvers: these guys will expand range and use up all available time
        if (presolve) {
            log.debug("Adding neighborhood presolvers.");
            parallelUHFSolvers.add(s -> new ConstraintGraphNeighborhoodPresolver(s,
                    new StationSubsetSATCertifier(clasp3ISolverFactory.create(ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1)),
                    new IterativeDeepeningConfigurationStrategy(new AddNeighbourLayerStrategy(), 10.0),
                    getConstraintManager()));
        }

        // Hit the cache at the instance level - we don't really count this one towards our numCores limit, because it will be I/O bound
        if (useCache) {
            parallelUHFSolvers.add(s -> {
                final ContainmentCacheProxy containmentCacheProxy = new ContainmentCacheProxy(serverURL, cacheCoordinate);
                ISolver UHFSolver = new SubsetCacheUNSATDecorator(s, containmentCacheProxy);// note that there is no need to check cache for UNSAT again, the first one would have caught it
                return new SupersetCacheSATDecorator(UHFSolver, containmentCacheProxy, cacheCoordinate);
            });
        }

        // Straight to clasp
        log.debug("Initializing base configured clasp solvers.");
        parallelUHFSolvers.add(s -> clasp3ISolverFactory.create(ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1));
        parallelUHFSolvers.add(s -> {
            ISolver solver = clasp3ISolverFactory.create(ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1, 1);
            solver = new ConstraintGraphNeighborhoodPresolver(s,
                    new StationSubsetUNSATCertifier(clasp3ISolverFactory.create(ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1)),
                    new IterativeDeepeningConfigurationStrategy(new AddNeighbourLayerStrategy(), 10.0),
                    getConstraintManager());
            return solver;
        }); // offset the seed a bit

        // Decompose the problem and then hit the cache and then clasp
        if (decompose || underconstrained) {
            final IComponentGrouper aGrouper = new ConstraintGrouper();
            parallelUHFSolvers.add(s -> {
                ISolver UHFSolver = clasp3ISolverFactory.create(ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h2);
                if (useCache) {
                    final ContainmentCacheProxy containmentCacheProxy = new ContainmentCacheProxy(serverURL, cacheCoordinate);
                    UHFSolver = new SupersetCacheSATDecorator(UHFSolver, containmentCacheProxy, cacheCoordinate); // note that there is no need to check cache for UNSAT again, the first one would have caught it
                    if (cacheResults) {
                        UHFSolver = new AssignmentVerifierDecorator(UHFSolver, getConstraintManager()); // let's be careful and verify the assignment before we cache it
                        UHFSolver = new CacheResultDecorator(UHFSolver, new CacherProxy(serverURL, cacheCoordinate), cacheCoordinate);
                    }
                }
                if (decompose) {
                    log.debug("Decorate solver to split the graph into connected components and then merge the results");
                    UHFSolver = new ConnectedComponentGroupingDecorator(UHFSolver, aGrouper, getConstraintManager());
                }
                if (underconstrained) {
                    log.debug("Decorate solver to first remove underconstrained stations.");
                    UHFSolver = new UnderconstrainedStationRemoverSolverDecorator(UHFSolver, getConstraintManager(), new HeuristicUnderconstrainedStationFinder(getConstraintManager(), true), true);
                }
                UHFSolver = new ArcConsistencyEnforcerDecorator(UHFSolver, getConstraintManager());
                return UHFSolver;
            });
        }

        // Init the parallel solvers
        ISolver UHFsolver = new ParallelNoWaitSolverComposite(numCores + 1, parallelUHFSolvers);
        // END UHF

        // BEGIN VHF
        final double SATcertifiercutoff = 5.0;
        ISolver VHFsolver = clasp3ISolverFactory.create(ClaspLibSATSolverParameters.HVHF_CONFIG_09_13_MODIFIED);
        if (decompose) {
            final IComponentGrouper aGrouper = new ConstraintGrouper();
            VHFsolver = new ConnectedComponentGroupingDecorator(VHFsolver, aGrouper, getConstraintManager());
        }
        if (underconstrained) {
            VHFsolver = new UnderconstrainedStationRemoverSolverDecorator(VHFsolver, getConstraintManager(), new HeuristicUnderconstrainedStationFinder(getConstraintManager(), false), false);
        }
        if (presolve) {
            VHFsolver = new ConstraintGraphNeighborhoodPresolver(VHFsolver,
                                            new StationSubsetSATCertifier(clasp3ISolverFactory.create(ClaspLibSATSolverParameters.HVHF_CONFIG_09_13_MODIFIED)),
                                            new IterativeDeepeningConfigurationStrategy(new AddNeighbourLayerStrategy(1), SATcertifiercutoff), getConstraintManager());
        }
        // END VHF

        // Save results, if needed.
        if (aResultFile != null) {
            log.debug("Decorate solver to save results.");
            UHFsolver = new ResultSaverSolverDecorator(UHFsolver, aResultFile);
            VHFsolver = new ResultSaverSolverDecorator(VHFsolver, aResultFile);
        }

        //Verify results.
        /*
         * NOTE: this is a MANDATORY decorator, and any decorator placed below this must not alter the answer or the assignment returned.
         */
        UHFsolver = new AssignmentVerifierDecorator(UHFsolver, getConstraintManager());
        VHFsolver = new AssignmentVerifierDecorator(VHFsolver, getConstraintManager());

        // Cache entire instance. Placed below assignment verifier because we wouldn't want to cache something incorrect
        if (useCache && cacheResults) {
            UHFsolver = new CacheResultDecorator(UHFsolver, new CacherProxy(serverURL, cacheCoordinate), cacheCoordinate);
        }

        fUHFSolver = UHFsolver;
        fVHFSolver = VHFsolver;
    }

    @Override
    public ISolver getSolver(StationPackingInstance aInstance) {
        //Return the right solver based on the channels in the instance.
        if (StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getAllChannels()) || StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getAllChannels())) {
            log.debug("Returning clasp configured for VHF");
            return fVHFSolver;
        } else {
            log.debug("Returning clasp configured for UHF");
            return fUHFSolver;
        }
    }

    @Override
    public void close() {
        fUHFSolver.notifyShutdown();
        fVHFSolver.notifyShutdown();
    }

}