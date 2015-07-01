package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

/**
 * Created by emily404 on 6/4/15.
 */
public class StationSamplerFactory{

    public static IStationSampler getStationSampler(StationSamplingMethod sampler){
        switch(sampler){
            case POPULATION_SIZE:
                return new PopulationSizeStationSampler();
            case ADD_SMALLEST:
                return new AddSmallestStationSampler();
        }
        throw new IllegalStateException();
    }

    public enum StationSamplingMethod {
        POPULATION_SIZE,
        ADD_SMALLEST
    }
}
