package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles;

import java.util.Arrays;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.VoidSolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.ConstraintGraphNeighborhoodPresolver;
import ca.ubc.cs.beta.stationpacking.solvers.certifiers.cgneighborhood.StationSubsetSATCertifier;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.composites.SequentialSolversComposite;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator.ICNFSaver;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.ConnectedComponentGroupingDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.UnderconstrainedStationRemoverSolverDecorator;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterionFactory;

/**
 * Created by newmanne on 24/04/15.
 */
public class CNFSolverBundle extends ASolverBundle {

    private ISolver cnfOnlySolver;

    public CNFSolverBundle(
            IStationManager aStationManager,
            IConstraintManager aConstraintManager,
            ICNFSaver aCNFSaver
    ) {
        super(aStationManager, aConstraintManager);
        cnfOnlySolver = new VoidSolver();
        // save the cnf for each component
        cnfOnlySolver = new CNFSaverSolverDecorator(cnfOnlySolver, getConstraintManager(), aCNFSaver, false);
        IComponentGrouper aGrouper = new ConstraintGrouper();
        cnfOnlySolver = new ConnectedComponentGroupingDecorator(cnfOnlySolver, aGrouper, getConstraintManager(), true);
        cnfOnlySolver = new UnderconstrainedStationRemoverSolverDecorator(cnfOnlySolver, aConstraintManager);
        cnfOnlySolver = new SequentialSolversComposite(
                Arrays.asList(
                        new ConstraintGraphNeighborhoodPresolver(aConstraintManager,
                                Arrays.asList(
                                        new StationSubsetSATCertifier(new CNFSaverSolverDecorator(new VoidSolver(), getConstraintManager(), aCNFSaver, true), new CPUTimeTerminationCriterionFactory(60.0))
                                )),
                        cnfOnlySolver
                )
        );

        // save the cnf for the full problem
        cnfOnlySolver = new CNFSaverSolverDecorator(cnfOnlySolver,getConstraintManager(), aCNFSaver, false);
    }

    @Override
    public ISolver getSolver(StationPackingInstance aInstance) {
        return cnfOnlySolver;
    }

    @Override
    public void close() throws Exception {
        cnfOnlySolver.notifyShutdown();
    }

}
