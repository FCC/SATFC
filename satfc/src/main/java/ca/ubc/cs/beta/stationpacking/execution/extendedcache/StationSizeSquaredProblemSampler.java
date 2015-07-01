package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

/**
 * Created by emily404 on 6/5/15.
 */
public class StationSizeSquaredProblemSampler implements IProblemSampler {

    /**
     * Sample problems based on station size of each problem
     * station size is squared to skew probability towards larger problems
     * problems with larger station size are more likely to be selected
     * @return key of cache entry to be added to keyQueue
     */
    @Override
    public String sample() {
        return "";
    }
}
