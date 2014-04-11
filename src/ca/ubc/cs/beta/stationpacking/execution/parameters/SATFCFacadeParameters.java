package ca.ubc.cs.beta.stationpacking.execution.parameters;

import ca.ubc.cs.beta.aeatk.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.base.InstanceParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="SATFC Facade Parameters",description="Parameters needed to execute SATFC facade on a single instance.")
public class SATFCFacadeParameters extends AbstractOptions {
	
	@ParametersDelegate
	public InstanceParameters fInstanceParameters = new InstanceParameters();
	
	@Parameter(names = "-DATA-FOLDERNAME",description = "station config data folder name", required=true)
	public String fDataFoldername;
	
	@Parameter(names = "-CLASP-LIBRARY",description = "clasp library file", required=true)
	public String fClaspLibrary;
	
	@ParametersDelegate
	public ComplexLoggingOptions fLoggingOptions = new ComplexLoggingOptions();
	
}
