package ca.ubc.cs.beta.stationpacking.tae;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

@UsageTextField(title="SATFC Target Algorithm Evaluator Options",description="Options needed to create a SATFC target algorithm evaluator.")
public class SATFCTargetAlgorithmEvaluatorOptions extends AbstractOptions {
	
	@UsageTextField(claimRequired="--station-config-folder")
	@Parameter(names = "--station-config-folder",description = "where to find station config data folders")
	public String fStationConfigFolder;
	
	@UsageTextField(claimRequired="--instances-folder")
	@Parameter(names = "--instances-folder",description = "where to look for problem instances")
	public String fInstancesFolder;
	
	@UsageTextField(claimRequired="--library")
	@Parameter(names = "--library",description = "the location of the necessary (clasp) SAT solver library")
	public String fLibrary;
	
}
