package ca.ubc.cs.beta.stationpacking.tae.switchfc;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

@UsageTextField(title="SWITCHFC Target Algorithm Evaluator Options",description="Options needed to create a SWITCHFC target algorithm evaluator.",claimRequired="--switchtae-tmpdir")
public class SwitchFCTargetAlgorithmEvaluatorOptions extends AbstractOptions {

    @Parameter(names = {"--switchtae-tmpdir"},description = "Temporary directory to store CNF files.")
    public String fTmpDir;
}
