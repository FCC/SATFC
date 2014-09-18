package ca.ubc.cs.beta.stationpacking.execution.parameters;

import ca.ubc.cs.beta.aeatk.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.SATFCSolverManagerParameters;

import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters to build SATFC.
 * @author afrechet
 */
@UsageTextField(title="SATFC Parameters",description="Parameters needed to build SATFC.")
public class SATFCParameters extends AbstractOptions {
    
    /**
     * Parameters needed to build SATFC solver manager.
     */
	@ParametersDelegate
	public SATFCSolverManagerParameters SolverManagerParameters = new SATFCSolverManagerParameters();
	
	/**
	 * Logging options.
	 */
	@ParametersDelegate
	public ComplexLoggingOptions LoggingOptions = new ComplexLoggingOptions();
}
