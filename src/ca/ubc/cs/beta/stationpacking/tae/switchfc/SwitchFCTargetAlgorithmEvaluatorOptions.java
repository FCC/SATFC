package ca.ubc.cs.beta.stationpacking.tae.switchfc;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

public class SwitchFCTargetAlgorithmEvaluatorOptions extends AbstractOptions {

    @Parameter(names = {"--switchtae-tmpdir"},description = "Temporary directory to store CNF files.",required=true)
    public String fTmpDir;
}
