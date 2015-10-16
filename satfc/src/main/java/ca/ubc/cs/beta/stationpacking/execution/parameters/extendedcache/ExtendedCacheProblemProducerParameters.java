package ca.ubc.cs.beta.stationpacking.execution.parameters.extendedcache;

import java.util.concurrent.TimeUnit;

import ca.ubc.cs.beta.aeatk.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.ProblemSamplerParameters;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.StationSamplerParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.RedisParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Created by emily404 on 5/28/15.
 */
@UsageTextField(title="Extended Cache Problem Generator Parameters",description="Parameters needed to generate new problems based on existing cache entries", level = OptionLevel.DEVELOPER)
public class ExtendedCacheProblemProducerParameters extends AbstractOptions {

    @Parameter(names = "-INTERFERENCES-FOLDER", description = "folder containing all the other interference folders")
    public String fInterferencesFolder;

    @Parameter(names = "-CLEARING-TARGET", description = "the highest channel a station can be packed on")
    public int fClearingTarget;

    @Parameter(names = "-CLEAN-QUEUE", description = "boolean indicating whether to clean up all the related queues")
    public boolean fCleanQueue;

    @ParametersDelegate
    public RedisParameters fRedisParameters = new RedisParameters();

    @ParametersDelegate
    public ProblemSamplerParameters fProblemSamplerParameters = new ProblemSamplerParameters();

    @ParametersDelegate
    public StationSamplerParameters fStationSamplerParameters = new StationSamplerParameters();

    @Parameter(names = "-SLEEP-INTERVAL", description = "the time in milliseconds thread sleeps for when queue is low, default 5 seconds")
    public long fSleepInterval = TimeUnit.SECONDS.toMillis(5);

    @Parameter(names = "-QUEUE-SIZE-THRESHOLD", description = "threshold indicating the number of keys in queue is low")
    public int fQueueSizeThreshold = 100;


    /**
     * Logging options.
     */
    @ParametersDelegate
    public ComplexLoggingOptions fLoggingOptions = new ComplexLoggingOptions();
}
