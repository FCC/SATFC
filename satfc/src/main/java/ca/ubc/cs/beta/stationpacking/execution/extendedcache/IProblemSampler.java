package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

/**
 * Created by emily404 on 6/4/15.
 */
public interface IProblemSampler {

    /**
     * Determine a new problem to extend based on a sampling method
     * {@link ca.ubc.cs.beta.stationpacking.execution.extendedcache.ProblemSamplerFactory.ProblemSamplingMethod}
     * @return key of cache entry to be added to keyQueue
     */
    String sample();
}
