package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.daemon;

import ca.ubc.cs.beta.aclib.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.SolverManagerParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters for an executable solver.
 * @author afrechet
 *
 */
@UsageTextField(title="FCC StationPacking Daemon Solver Options",description="Parameters required to launch a daemon solver.")
public class ThreadedSolverServerParameters extends AbstractOptions {	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@ParametersDelegate
	public SolverManagerParameters SolverManagerParameters = new SolverManagerParameters();
	
	@Parameter(names= "-ALLOW-ANYONE", description = "whether the server should listen to all message on its port, or just localhost ones.")
	public boolean AllowAnyone = false;
	

	
	@Parameter(names = "-PORT",description = "the localhost UDP port to listen to", required=true, validateWith=PortValidator.class)
	public int Port;
	
	@ParametersDelegate
	public ComplexLoggingOptions LoggingOptions = new ComplexLoggingOptions();
	

	
	
	
}