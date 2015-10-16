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

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.CacherProxy;
import ca.ubc.cs.beta.stationpacking.cache.ICacher;
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
import ca.ubc.cs.beta.stationpacking.solvers.decorators.AssignmentVerifierDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ConnectedComponentGroupingDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ResultSaverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.UnderconstrainedStationRemoverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.CacheResultDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SubsetCacheUNSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SupersetCacheSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.underconstrained.HeuristicUnderconstrainedStationFinder;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

/**
 * SATFC solver bundle that lines up pre-solving and main solver.
 *
 * @author afrechet
 */
@Slf4j
public class SATFCSolverBundle extends ASolverBundle {

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
    public SATFCSolverBundle(
            String aClaspLibraryPath,
            IStationManager aStationManager,
            IConstraintManager aConstraintManager,
            String aResultFile,
            boolean presolve,
            boolean decompose,
            boolean underconstrained,
            String serverURL,
            boolean cacheResults
    ) {
        super(aStationManager, aConstraintManager);
        log.info("Initializing solver with the following solver options: presolve {}, decompose {}, underconstrained {}, serverURL {}", presolve, decompose, underconstrained, serverURL);
        boolean useCache = serverURL != null;

        log.debug("SATFC solver bundle.");

        SATCompressor aCompressor = new SATCompressor(this.getConstraintManager());
        final Clasp3ISolverFactory clasp3ISolverFactory = new Clasp3ISolverFactory(new ClaspLibraryGenerator(aClaspLibraryPath), aCompressor, getConstraintManager());

        log.debug("Initializing base configured clasp solvers.");

        ISolver UHFClaspBasedSolver = clasp3ISolverFactory.create(ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h2);
        ISolver VHFClaspBasedSolver = clasp3ISolverFactory.create(ClaspLibSATSolverParameters.HVHF_CONFIG_09_13_MODIFIED);

        //Chain pre-solving and main solver.
        final double SATcertifiercutoff = 5;

        ISolver UHFsolver = UHFClaspBasedSolver;
        ISolver VHFsolver = VHFClaspBasedSolver;

        /**
         * Decorate solvers - remember that the decorator that you put first is applied last
         */
        ContainmentCacheProxy containmentCache = null;
        ICacher cacher = null;
        CacheCoordinate cacheCoordinate = null;
        if (useCache) {
            cacheCoordinate = new CacheCoordinate(aStationManager.getDomainHash(), aConstraintManager.getConstraintHash());
            cacher = new CacherProxy(serverURL, cacheCoordinate);
            containmentCache = new ContainmentCacheProxy(serverURL, cacheCoordinate);
        }

        if (useCache) {
            UHFsolver = new SupersetCacheSATDecorator(UHFsolver, containmentCache, cacheCoordinate); // note that there is no need to check cache for UNSAT again, the first one would have caught it
            if (cacheResults) {
                UHFsolver = new AssignmentVerifierDecorator(UHFsolver, getConstraintManager()); // let's be careful and verify the assignment before we cache it
                UHFsolver = new CacheResultDecorator(UHFsolver, cacher, cacheCoordinate);
            }
        }

        if (decompose)
        {
            // Split into components
            IComponentGrouper aGrouper = new ConstraintGrouper();
            log.debug("Decorate solver to split the graph into connected components and then merge the results");
            UHFsolver = new ConnectedComponentGroupingDecorator(UHFsolver, aGrouper, getConstraintManager());
            VHFsolver = new ConnectedComponentGroupingDecorator(VHFsolver, aGrouper, getConstraintManager());
        }

        if (underconstrained)
        {
            //Remove unconstrained stations.
            log.debug("Decorate solver to first remove underconstrained stations.");
            UHFsolver = new UnderconstrainedStationRemoverSolverDecorator(UHFsolver, getConstraintManager(), new HeuristicUnderconstrainedStationFinder(getConstraintManager(), false), false);
            VHFsolver = new UnderconstrainedStationRemoverSolverDecorator(VHFsolver, getConstraintManager(), new HeuristicUnderconstrainedStationFinder(getConstraintManager(), false), false);
        }

        if (presolve)
        {
            log.debug("Adding neighborhood presolvers.");
            UHFsolver = new ConstraintGraphNeighborhoodPresolver(UHFsolver,
                                new StationSubsetSATCertifier(clasp3ISolverFactory.create(ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1)),
                                new IterativeDeepeningConfigurationStrategy(new AddNeighbourLayerStrategy(1), SATcertifiercutoff), getConstraintManager());

            VHFsolver = new ConstraintGraphNeighborhoodPresolver(VHFsolver,
                                    new StationSubsetSATCertifier(clasp3ISolverFactory.create(ClaspLibSATSolverParameters.HVHF_CONFIG_09_13_MODIFIED)),
                                    new IterativeDeepeningConfigurationStrategy(new AddNeighbourLayerStrategy(1), SATcertifiercutoff), getConstraintManager());
        }

        if (useCache) {
            UHFsolver = new SubsetCacheUNSATDecorator(UHFsolver, containmentCache); // note that there is no need to check cache for UNSAT again, the first one would have caught it
            UHFsolver = new SupersetCacheSATDecorator(UHFsolver, containmentCache, cacheCoordinate);
        }

        //Save results, if needed.
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
            UHFsolver = new CacheResultDecorator(UHFsolver, cacher, cacheCoordinate);
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