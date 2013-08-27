package ca.ubc.cs.beta.stationpacking.daemon.datamanager;

import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;

/**
 * Creates an ISolver factory that will be using the given SAT Solver, encoder and grouper.  Note
 * that the same instance of SAT solver, encoder and grouper will be used in all the ISolvers created 
 * (i.e. calls to create).
 */
public class SATBasedSolverFactory implements ISolverFactory {

	private ISATSolver fSATSolver;
	private IComponentGrouper fGrouper;
	
	/**
	 * Creates a new factory that will use the given solver and grouper to create ISolvers.
	 * @param solver solver to be used to create ISolvers.
	 * @param grouper grouper to be used to create ISolvers.
	 */
	public SATBasedSolverFactory(ISATSolver solver, IComponentGrouper grouper)
	{
		fSATSolver = solver;
		fGrouper = grouper;
	}
	
	@Override
	public ISolver create(IStationManager stationManager, IConstraintManager constraintManager) {
		ISATEncoder encoder = new SATEncoder(stationManager, constraintManager);
		ISolver solver = new SATBasedSolver(fSATSolver, encoder, constraintManager, fGrouper);
		return solver;
	}

}
