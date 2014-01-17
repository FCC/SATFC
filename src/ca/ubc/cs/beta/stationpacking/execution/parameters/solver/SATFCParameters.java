package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.aclib.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aclib.logging.LoggingOptions;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.base.StationPackingQuestionParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="SATFC Parameters",description="Parameters needed to execute SATFC on a single instance.")
public class SATFCParameters extends AbstractOptions {

	@ParametersDelegate
	public SolverManagerParameters SolverManagerParameters = new SolverManagerParameters();
	
	@ParametersDelegate
	public StationPackingQuestionParameters QuestionParameters = new StationPackingQuestionParameters();
	
	@ParametersDelegate
	public LoggingOptions LoggingOptions = new ComplexLoggingOptions();
	
	@Parameter(names = "-WORKDIR", description = "Working directory (especially where to find problem data).")
	public String WorkDirectory = "";
}
