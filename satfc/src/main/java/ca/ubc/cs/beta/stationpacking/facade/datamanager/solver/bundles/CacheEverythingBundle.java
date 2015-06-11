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

import java.util.Arrays;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.CacherProxy;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.ConstraintGraphNeighborhoodPresolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.composites.SequentialSolversComposite;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.AssignmentVerifierDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ConnectedComponentGroupingDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.UnderconstrainedStationRemoverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.CacheResultDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SubsetCacheUNSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SupersetCacheSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.Clasp3SATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterionFactory;

/**
 * Created by newmanne on 28/04/15.
 */
public class CacheEverythingBundle extends ASolverBundle {

    ISolver solver;

    public CacheEverythingBundle(
            String aClaspLibraryPath,
            IStationManager aStationManager,
            IConstraintManager aConstraintManager,
            String serverURL
    ) {
        super(aStationManager, aConstraintManager);

        SATCompressor aCompressor = new SATCompressor(this.getConstraintManager());
        AbstractCompressedSATSolver aUHFClaspSATsolver = new Clasp3SATSolver(aClaspLibraryPath, ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1);
        ISolver UHFClaspBasedSolver = new CompressedSATBasedSolver(aUHFClaspSATsolver, aCompressor, this.getConstraintManager());


        solver = UHFClaspBasedSolver;

        CacheCoordinate cacheCoordinate = new CacheCoordinate(aStationManager.getHashCode(), aConstraintManager.getHashCode());
        CacherProxy cacher = new CacherProxy(serverURL, cacheCoordinate);
        ContainmentCacheProxy containmentCache = new ContainmentCacheProxy(serverURL, cacheCoordinate);

        solver = new SupersetCacheSATDecorator(solver, containmentCache, cacheCoordinate);
        solver = new AssignmentVerifierDecorator(solver, getConstraintManager());
        solver = new CacheResultDecorator(solver, cacher, cacheCoordinate);

        IComponentGrouper aGrouper = new ConstraintGrouper();
        solver = new ConnectedComponentGroupingDecorator(solver, aGrouper, getConstraintManager(), true);

        solver = new UnderconstrainedStationRemoverSolverDecorator(solver, getConstraintManager());

        final double SATcertifiercutoff = 5;
        solver = new SequentialSolversComposite(
                Arrays.asList(
                        new ConstraintGraphNeighborhoodPresolver(aConstraintManager,
                                Arrays.asList(
                                        new StationSubsetSATCertifier(UHFClaspBasedSolver, new CPUTimeTerminationCriterionFactory(SATcertifiercutoff))
                                )),
                        solver
                )
        );

        solver = new SubsetCacheUNSATDecorator(solver, containmentCache); // note that there is no need to check cache for UNSAT again, the first one would have caught it
        solver = new SupersetCacheSATDecorator(solver, containmentCache, cacheCoordinate);

        // cache entire instance
        solver = new AssignmentVerifierDecorator(solver, getConstraintManager());
        solver = new CacheResultDecorator(solver, cacher, cacheCoordinate);
    }

    @Override
    public void close() throws Exception {
        solver.notifyShutdown();
    }

    @Override
    public ISolver getSolver(StationPackingInstance aInstance) {
        return solver;
    }
}
