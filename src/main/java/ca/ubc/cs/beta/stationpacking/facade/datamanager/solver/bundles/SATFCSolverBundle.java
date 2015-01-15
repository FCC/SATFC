/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
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

import ca.ubc.cs.beta.stationpacking.execution.SATFCFacadeExecutor;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.database.CachingDecoratorFactory;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCCachingParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.ConstraintGraphNeighborhoodPresolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetUNSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.composites.SequentialSolversComposite;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ConnectedComponentGroupingDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.AssignmentVerifierDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.RedisCachingSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ResultSaverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.UnderconstrainedStationRemoverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ClaspSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterionFactory;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * SATFC solver bundle that lines up pre-solving and main solver.
 *
 * @author afrechet
 */
public class SATFCSolverBundle extends ASolverBundle {

    private static Logger log = LoggerFactory.getLogger(SATFCSolverBundle.class);

    private final ISolver fUHFSolver;
    private final ISolver fVHFSolver;

    @Override
    public ISolver getSolver(StationPackingInstance aInstance) {

        //Return the right solver based on the channels in the instance.

        if (StationPackingUtils.HVHF_CHANNELS.containsAll(aInstance.getAllChannels()) || StationPackingUtils.LVHF_CHANNELS.containsAll(aInstance.getAllChannels())) {
            log.debug("Returning clasp configured for VHF (September 2013) with Ilya's station subset pre-solving.");
            return fVHFSolver;
        } else {
            log.debug("Returning clasp configured for UHF (November 2013) with Ilya's station subset pre-solving.");
            return fUHFSolver;
        }
    }

    /**
     * Create a SATFC solver bundle.
     *
     * @param aClaspLibraryPath  - library for the clasp to use.
     * @param aStationManager    - station manager.
     * @param aConstraintManager - constraint manager.
     * @param aCNFDirectory      - directory in which CNFs should be saved (optional).
     * @param aResultFile        - file to which results should be written (optional).
     */
    public SATFCSolverBundle(
            String aClaspLibraryPath,
            IStationManager aStationManager,
            IConstraintManager aConstraintManager,
            String aCNFDirectory,
            String aResultFile,
            SATFCFacadeParameter.SolverCustomizationOptions solverOptions
    ) {
        super(aStationManager, aConstraintManager);
        log.info("Initializing solver with the following solver options {}", solverOptions);

        log.debug("SATFC solver bundle.");

        SATCompressor aCompressor = new SATCompressor(this.getConstraintManager());

        log.debug("Initializing base configured clasp solvers.");

        AbstractCompressedSATSolver aUHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.ALL_CONFIG_11_13);
        ISolver UHFClaspBasedSolver = new CompressedSATBasedSolver(aUHFClaspSATsolver, aCompressor, this.getConstraintManager());

        AbstractCompressedSATSolver aHVHFClaspSATsolver = new ClaspSATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.HVHF_CONFIG_09_13);
        ISolver VHFClaspBasedSolver = new CompressedSATBasedSolver(aHVHFClaspSATsolver, aCompressor, this.getConstraintManager());

        //Chain pre-solving and main solver.
        final double UNSATcertifiercutoff = 5;
        final double SATcertifiercutoff = 5;

        ISolver UHFsolver = UHFClaspBasedSolver;
        ISolver VHFsolver = VHFClaspBasedSolver;

        /**
         * Decorate solvers - remember that the decorator that you put first is applied last
         */

        // Check the cache - this is at the component level
        if (solverOptions.isCache()) {
            log.debug("Decorate solver to check the cache at the component level");
            UHFsolver = solverOptions.getCachingDecoratorFactory().createCachingDecorator(UHFsolver, solverOptions.getCacheGraphKey());
            VHFsolver = solverOptions.getCachingDecoratorFactory().createCachingDecorator(VHFsolver, solverOptions.getCacheGraphKey());
        }

        if (solverOptions.isPresolve())
        {
            log.debug("Adding neighborhood presolvers.");
            UHFsolver = new SequentialSolversComposite(
                    Arrays.asList(
                            new ConstraintGraphNeighborhoodPresolver(aConstraintManager,
                                    Arrays.asList(
                                            new StationSubsetUNSATCertifier(UHFClaspBasedSolver, new CPUTimeTerminationCriterionFactory(UNSATcertifiercutoff)),
                                            new StationSubsetSATCertifier(UHFClaspBasedSolver, new CPUTimeTerminationCriterionFactory(SATcertifiercutoff))
                                    )),
                            UHFsolver
                    )
            );

            VHFsolver = new SequentialSolversComposite(
                    Arrays.asList(
                            new ConstraintGraphNeighborhoodPresolver(aConstraintManager,
                                    Arrays.asList(
                                            new StationSubsetUNSATCertifier(VHFClaspBasedSolver, new CPUTimeTerminationCriterionFactory(UNSATcertifiercutoff)),
                                            new StationSubsetSATCertifier(VHFClaspBasedSolver, new CPUTimeTerminationCriterionFactory(SATcertifiercutoff))
                                    )),
                            VHFsolver
                    )
            );
        }

        if (solverOptions.isDecompose())
        {
            // Split into components
            IComponentGrouper aGrouper = new ConstraintGrouper();
            log.debug("Decorate solver to split the graph into connected components and then merge the results");
            UHFsolver = new ConnectedComponentGroupingDecorator(UHFsolver, aGrouper, getConstraintManager());
            VHFsolver = new ConnectedComponentGroupingDecorator(VHFsolver, aGrouper, getConstraintManager());
        }

        if (solverOptions.isUnderconstrained())
        {
            //Remove unconstrained stations.
            log.debug("Decorate solver to first remove underconstrained stations.");
            UHFsolver = new UnderconstrainedStationRemoverSolverDecorator(UHFsolver, getConstraintManager());
            VHFsolver = new UnderconstrainedStationRemoverSolverDecorator(VHFsolver, getConstraintManager());
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

        fUHFSolver = UHFsolver;
        fVHFSolver = VHFsolver;

    }

    @Override
    public void close() {
        fUHFSolver.notifyShutdown();
        fVHFSolver.notifyShutdown();
    }


}
