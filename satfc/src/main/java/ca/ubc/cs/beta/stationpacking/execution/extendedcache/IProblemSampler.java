package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.util.List;

/**
 * Created by emily404 on 6/4/15.
 */
public interface IProblemSampler {

    /**
     * Determine a list of new problems to extend based on a sampling method
     * {@link ca.ubc.cs.beta.stationpacking.execution.extendedcache.ProblemSamplerParameters.ProblemSamplingMethod}
     * @param counter put counter number of new problems to keyQueue
     * @return keys of cache entries to be added to keyQueue
     */
    List<String> sample(int counter);

    /**
     * Determine a new problems to extend based on a sampling method
     * {@link ca.ubc.cs.beta.stationpacking.execution.extendedcache.ProblemSamplerParameters.ProblemSamplingMethod}
     * @return key of cache entry to be added to keyQueue
     */
    String sample();
}
