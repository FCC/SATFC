package ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.CompressedSATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;

/**
 * Creates an ISolver factory that will be using the given SAT Solver, encoder and grouper.  Note
 * that the same instance of SAT solver, encoder and grouper will be used in all the ISolvers created 
 * (i.e. calls to create).
 */
public class CompressedSATBasedSolverFactory implements ISolverFactory {

	private AbstractCompressedSATSolver fSATSolver;
	private IComponentGrouper fGrouper;
	
	/**
	 * Creates a new factory that will use the given solver and grouper to create ISolvers.
	 * @param solver solver to be used to create ISolvers.
	 * @param grouper grouper to be used to create ISolvers.
	 */
	public CompressedSATBasedSolverFactory(AbstractCompressedSATSolver solver, IComponentGrouper grouper)
	{
		fSATSolver = solver;
		fGrouper = grouper;
	}
	
	@Override
	public ISolver create(IStationManager stationManager, IConstraintManager constraintManager) {
		SATCompressor encoder = new SATCompressor(stationManager, constraintManager);
		ISolver solver = new CompressedSATBasedSolver(fSATSolver, encoder, constraintManager, fGrouper);
		return solver;
	}

}
