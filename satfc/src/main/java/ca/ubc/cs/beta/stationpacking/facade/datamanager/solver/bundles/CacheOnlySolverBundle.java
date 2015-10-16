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

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.VoidSolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.AssignmentVerifierDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ConnectedComponentGroupingDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.UnderconstrainedStationRemoverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SubsetCacheUNSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.SupersetCacheSATDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.underconstrained.MIPUnderconstrainedStationFinder;

/**
 * Created by newmanne on 27/04/15.
 */

/**
 * This bundle is to evaluate the cache performance (without any additional solvers. It does not add anything to the cache.
 */
public class CacheOnlySolverBundle extends ASolverBundle {

    private ISolver cacheOnlySolver;

    public CacheOnlySolverBundle(IStationManager aStationManager, IConstraintManager aConstraintManager, String serverURL, boolean components) {
        super(aStationManager, aConstraintManager);

        CacheCoordinate cacheCoordinate = new CacheCoordinate(aStationManager.getDomainHash(), aConstraintManager.getConstraintHash());
        ContainmentCacheProxy containmentCache = new ContainmentCacheProxy(serverURL, cacheCoordinate);
        IComponentGrouper aGrouper = new ConstraintGrouper();

        cacheOnlySolver = new VoidSolver();
        cacheOnlySolver = new SupersetCacheSATDecorator(cacheOnlySolver, containmentCache, cacheCoordinate);
        cacheOnlySolver = new ConnectedComponentGroupingDecorator(cacheOnlySolver, aGrouper, getConstraintManager(), components);
        cacheOnlySolver = new UnderconstrainedStationRemoverSolverDecorator(cacheOnlySolver, aConstraintManager, new MIPUnderconstrainedStationFinder(aConstraintManager), false);
        if (!components) {
            cacheOnlySolver = new SubsetCacheUNSATDecorator(cacheOnlySolver, containmentCache);
            cacheOnlySolver = new SupersetCacheSATDecorator(cacheOnlySolver, containmentCache, cacheCoordinate);
        }
        cacheOnlySolver = new AssignmentVerifierDecorator(cacheOnlySolver, getConstraintManager());
    }

    @Override
    public ISolver getSolver(StationPackingInstance aInstance) {
        return cacheOnlySolver;
    }

    @Override
    public void close() throws Exception {
        cacheOnlySolver.notifyShutdown();
    }
}
