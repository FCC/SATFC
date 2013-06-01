package ca.ubc.cs.beta.stationpacking.execution.incremental;


import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.ImplementedSolverParameterValidator;
import ca.ubc.cs.beta.stationpacking.execution.parameters.RepackingDataParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parser for main parameters related to starting a (non-incremental) solver
 * @author afrechet, narnosti
 *
 */
@UsageTextField(title="FCC StationPacking Incremental Solver Options",description="Parameters required to start the solver.")
public class IncrementalSolverParameterParser extends AbstractOptions {	
	
	//Data parameters
	@ParametersDelegate
	private RepackingDataParameters fRepackingDataParameters = new RepackingDataParameters();
	public RepackingDataParameters getRepackingDataParameters()
	{
		return fRepackingDataParameters;
	}
	

	@Parameter(names = "-SOLVER", description = "SAT solver to use.", required=true, validateWith = ImplementedSolverParameterValidator.class)
	private String fSolver;
	public String getSolver()
	{
		return fSolver;
	}
	

	@Parameter(names = "-SEED", description = "Seed.")
	private long fSeed = 1;
	public long getSeed()
	{
		return fSeed;
	}

	@Parameter(names = "-numDummyVars", description = "The number of runs for which we can effectively delete clauses")
	private Integer fNumDummyVars = 100;
	public int getNumDummyVars()
	{
		return fNumDummyVars;
	}
}