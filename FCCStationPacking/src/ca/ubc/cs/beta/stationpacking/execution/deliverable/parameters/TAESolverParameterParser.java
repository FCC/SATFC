package ca.ubc.cs.beta.stationpacking.execution.deliverable.parameters;


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
@UsageTextField(title="FCC StationPacking Main Method Options",description="Parameters required to start the solver.")
public class TAESolverParameterParser extends AbstractOptions {	
	
	//Data parameters
	@ParametersDelegate
	private RepackingDataParameters fRepackingDataParameters = new RepackingDataParameters();
	public RepackingDataParameters getRepackingDataParameters()
	{
		return fRepackingDataParameters;
	}
	
	//TAE parameters
	@ParametersDelegate
	private AlgorithmExecutionOptions fAlgorithmExecutionOptions = new AlgorithmExecutionOptions();
	public AlgorithmExecutionOptions getAlgorithmExecutionOptions() {
		return fAlgorithmExecutionOptions;
	}

	@Parameter(names = "-SOLVER", description = "SAT solver to use.", required=true, validateWith = ImplementedSolverParameterValidator.class)
	private String fSolver;
	public String getSolver()
	{
		return fSolver;
	}
	
	//Experiment parameters
	@Parameter(names = "-CNF_DIR", description = "Directory location of where to write CNFs.", required=true)
	private String fCNFDirectory;
	public String getCNFDirectory(){
		return fCNFDirectory;
	}

	@Parameter(names = "-SEED", description = "Seed.")
	private long fSeed = 1;
	public long getSeed()
	{
		return fSeed;
	}

	@Parameter(names = "-CNFLOOKUP_OUTPUT_FILE", description = "File to store CNF results.")
	private String fCNFOutputName = "CNFOutput";
	public String getCNFOutputName()
	{
		return fCNFOutputName;
	}
}