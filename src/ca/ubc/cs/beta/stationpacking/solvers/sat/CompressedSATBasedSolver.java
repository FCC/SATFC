package ca.ubc.cs.beta.stationpacking.solvers.sat;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;

/**
 * SAT based feasibility checking solver for SAT solvers that required compressed CNFs.
 * @author afrechet
 */
public class CompressedSATBasedSolver extends GenericSATBasedSolver {

	public CompressedSATBasedSolver(AbstractCompressedSATSolver aSATSolver, SATCompressor aSATCompressor, IConstraintManager aConstraintManager, IComponentGrouper aComponentGrouper)
	{
		super(aSATSolver,aSATCompressor,aConstraintManager,aComponentGrouper);
	}

}
