package ca.ubc.cs.beta.stationpacking.execution.parameters;

import ca.ubc.cs.beta.aeatk.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.ProblemSamplerFactory;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.StationSamplerFactory;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Created by emily404 on 5/28/15.
 */
@UsageTextField(title="Extended Cache Problem Generator Parameters",description="Parameters needed to generate new problems based on existing cache entries")
public class ExtendedCacheProblemProducerParameters extends AbstractOptions {

    @Parameter(names = "-INTERFERENCES-FOLDER", description = "folder containing all the other interference folders")
    public String fInterferencesFolder;

    @Parameter(names = "-CLEARING-TARGET", description = "the highest channel a station can be packed on")
    public int fClearingTarget;

    @Parameter(names = "-PROBLEM-SAMPLER", description = "sampling method to produce next cache entry to augment")
    public ProblemSamplerFactory.ProblemSamplingMethod fProblemSampler;

    @Parameter(names = "-STATION-SAMPLER", description = "sampling method to produce next station to add to a cache entry")
    public StationSamplerFactory.StationSamplingMethod fStationSampler;

    @ParametersDelegate
    public RedisParameters fRedisParameters = new RedisParameters();

    /**
     * Logging options.
     */
    @ParametersDelegate
    public ComplexLoggingOptions fLoggingOptions = new ComplexLoggingOptions();
}
