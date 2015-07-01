package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

/**
 * Created by emily404 on 6/5/15.
 */
public class ProblemSamplerFactory {
    public static IProblemSampler getProblemSampler(ProblemSamplingMethod sampler){
        switch(sampler){
            case STATION_SIZE_SQUARED:
                return new StationSizeSquaredProblemSampler();
        }
        throw new IllegalStateException();
    }

    public enum ProblemSamplingMethod {
        STATION_SIZE_SQUARED
    }
}
