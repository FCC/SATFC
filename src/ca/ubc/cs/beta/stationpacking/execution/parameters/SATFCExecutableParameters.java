package ca.ubc.cs.beta.stationpacking.execution.parameters;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.base.QuestionInstanceParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters for the stand-alone SATFC executable.
 * @author afrechet
 */
@UsageTextField(title="SATFC Executable Parameters",description="Parameters needed to execute SATFC on a single instance.")
public class SATFCExecutableParameters extends AbstractOptions {
	
    /**
     * SATFC solver parameters.
     */
	@ParametersDelegate
	public SATFCParameters SATFCParameters = new SATFCParameters();
	
	/**
	 * Question defining station packing instance to solve.
	 */
	@ParametersDelegate
	public QuestionInstanceParameters QuestionParameters = new QuestionInstanceParameters();
	
	/**
	 * Working directory.
	 */
	@Parameter(names = "-WORKDIR", description = "Working directory (especially where to find problem data).")
	public String WorkDirectory = "";
	
}
