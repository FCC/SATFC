package ca.ubc.cs.beta.stationpacking.webapp.parameters;

import java.io.File;

import lombok.Getter;
import lombok.ToString;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;

/**
* Created by newmanne on 30/06/15.
*/
@ToString
@Parameters(separators = "=")
@UsageTextField(title="SATFCServer Parameters",description="Parameters needed to build SATFCServer")
public class SATFCServerParameters extends AbstractOptions {
    @Parameter(names = "--redis.host", description = "host for redis")
    @Getter
    private String redisURL =  "localhost";
    @Parameter(names = "--redis.port", description = "port for redis")
    @Getter
    private int redisPort = 6379;
    @Parameter(names = "--constraint.folder", description = "Folder containing all of the station configuration folders", required = true)
    @Getter
    private String constraintFolder;

    public void validate() {
        Preconditions.checkArgument(new File(constraintFolder).isDirectory(), "Provided constraint folder is not a directory", constraintFolder);
    }

}
