package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;


import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parameter class to merge different types of solvers (TAE and Incremental).
 * @author afrechet
 * TODO for future usage.
 */
@UsageTextField(title="FCC Station Packing Solver Options",description="Parameters defining a feasibility checker SAT solver.")
public class SolverParameters extends AbstractOptions implements ISolverParameters{
	
	public static enum SolverChoice
	{
		INCREMENTAL,TAE;
	};

	@Parameter(names = "-SOLVER-TYPE",description = "the type of solver that will be executed.", required=true)
	public SolverChoice SolverChoice;
	
	@ParametersDelegate
	public TAESolverParameters TAESolverParameters = new TAESolverParameters();
	
	@ParametersDelegate
	public IncrementalSolverParameters IncrementalSolverParameters = new IncrementalSolverParameters();
	
	@Override
	public ISolver getSolver(IStationManager aStationManager, IConstraintManager aConstraintManager)
	{
		if(SolverChoice==null)
		{
			throw new ParameterException("Solver choice (--solver-type) must be defined!");
		}
		switch(SolverChoice)
		{
			case TAE:
				return TAESolverParameters.getSolver(aStationManager,aConstraintManager);
			case INCREMENTAL:
				return IncrementalSolverParameters.getSolver(aStationManager,aConstraintManager);
			default:
				throw new ParameterException("Unrecognized solver choice "+SolverChoice);
		}
	}
	
	

}
