package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.RedisParameters;

import com.beust.jcommander.Parameter;

/**
 * Created by emily404 on 6/17/15.
 */
public class ProblemSamplerParameters extends AbstractOptions {

    @Parameter(names = "-PROBLEM-SAMPLER", description = "sampling method to produce next cache entry to augment")
    public ProblemSamplingMethod fProblemSampler;

    public IProblemSampler getProblemSampler(RedisParameters redisParameters){
        switch(fProblemSampler){
            case STATION_SIZE_SQUARED:
                return new StationSizeSquaredProblemSampler(redisParameters.getJedis());
            default:
                throw new IllegalStateException("Specified --PROBLEM-SAMPLER is invalid");
        }
    }

    public enum ProblemSamplingMethod {
        STATION_SIZE_SQUARED
    }
}