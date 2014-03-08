package ca.ubc.cs.beta.stationpacking.execution.parameters;

import ca.ubc.cs.beta.aclib.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.SolverManagerParameters;

import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="SATFC Facade Parameters",description="Parameters needed to solve an instance with a SATFC facade.")
public class SATFCParameters extends AbstractOptions {
	
	@ParametersDelegate
	public SolverManagerParameters SolverManagerParameters = new SolverManagerParameters();
	
	@ParametersDelegate
	public ComplexLoggingOptions LoggingOptions = new ComplexLoggingOptions();
}
