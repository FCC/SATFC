package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.TAESATSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SATBasedSolver;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;

@UsageTextField(title="FCC Station Packing Packing SAT Solver Based Feasibility Checker",description="Parameters defining a SAT solver based feasibility checker.")
public class SATBasedSolverParameters extends AbstractOptions implements ISolverParameters{
	
	public static enum SATSolverChoice
	{
		TAE;
	};

	@Parameter(names = "-SAT-SOLVER-TYPE",description = "the type of SAT solver that will be used.", required=true)
	public SATSolverChoice SATSolverChoice;

	@ParametersDelegate
	public TAESATSolverParameters TAESATSolverParameters = new TAESATSolverParameters();
	
	@Override
	public ISolver getSolver(IStationManager aStationManager,
			IConstraintManager aConstraintManager) {
		
		Logger log = LoggerFactory.getLogger(SATBasedSolverParameters.class);
		
		log.info("Creating a SAT solver based feasibility checker.");
		
		ISATSolver aSATSolver;
		switch(SATSolverChoice)
		{
			case TAE:
				aSATSolver = TAESATSolverParameters.getSATSolver();
				log.info("Using a TAE based SAT solver.");
				break;
			default:
				throw new ParameterException("Unrecognized SAT solver choice "+SATSolverChoice);
		}
		
		ISATEncoder aSATEncoder = new SATEncoder(aStationManager, aConstraintManager);
		
		//Decided to go with a no grouper.
		log.info("Not grouping stations in any way.");
		IComponentGrouper aComponentGrouper = new NoGrouper();
		
		return new SATBasedSolver(aSATSolver, aSATEncoder, aConstraintManager, aComponentGrouper);
		
	}


	

}
