package ca.ubc.cs.beta.stationpacking.tae;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

@UsageTextField(title="SATFC Target Algorithm Evaluator Options",description="Options needed to create a SATFC target algorithm evaluator.",claimRequired="--satfctae-station-folder,--satfctae-library")
public class SATFCTargetAlgorithmEvaluatorOptions extends AbstractOptions {

	@Parameter(names = "--satfctae-config-folder",description = "where to find station config data folders")
	public String fStationConfigFolder;
	
	@Parameter(names = {"--satfctae-library"},description = "the location of the necessary (clasp) SAT solver library")
	public String fLibrary;
	
}
