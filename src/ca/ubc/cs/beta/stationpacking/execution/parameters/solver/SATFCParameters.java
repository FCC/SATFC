package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import ca.ubc.cs.beta.aclib.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;

import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="SATFC Parameters",description="Parameters needed to instantiate a SATFC.")
public class SATFCParameters extends AbstractOptions {

	@ParametersDelegate
	public SolverManagerParameters SolverManagerParameters = new SolverManagerParameters();
	
	@ParametersDelegate
	public ComplexLoggingOptions LoggingOptions = new ComplexLoggingOptions();
}
