package ca.ubc.cs.beta.stationpacking.solvers.sat;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractSATSolver;

/**
 * SAT based feasibility checking solver.
 * @author afrechet
 */
public class SATBasedSolver extends GenericSATBasedSolver {

	public SATBasedSolver(AbstractSATSolver aSATSolver, SATEncoder aSATEncoder, IConstraintManager aConstraintManager, IComponentGrouper aComponentGrouper)
	{
		super(aSATSolver,aSATEncoder,aConstraintManager,aComponentGrouper);
	}

}
