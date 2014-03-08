package ca.ubc.cs.beta.stationpacking.execution.parameters;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.base.StationPackingQuestionParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="SATFC Executable Parameters",description="Parameters needed to execute SATFC on a single instance.")
public class SATFCExecutableParameters extends AbstractOptions {
	
	@ParametersDelegate
	public SATFCParameters SATFCParameters = new SATFCParameters();
	
	@ParametersDelegate
	public StationPackingQuestionParameters QuestionParameters = new StationPackingQuestionParameters();
	
	@Parameter(names = "-WORKDIR", description = "Working directory (especially where to find problem data).")
	public String WorkDirectory = "";
}
