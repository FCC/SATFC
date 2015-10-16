package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.io.IOException;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

/**
 * Created by emily404 on 6/17/15.
 */
public class StationSamplerParameters extends AbstractOptions {

    @Parameter(names = "-STATION-SAMPLER", description = "sampling method to produce next station to add to a cache entry")
    public StationSamplingMethod fStationSamplingMethod;

    @Parameter(names = "-STATION-SAMPLER-SOURCE-FILE", description = "supplementary file for station sampling")
    public String fStationSamplingFile;

    public IStationSampler getStationSampler() throws IOException {
        switch(fStationSamplingMethod){
            case POPULATION_SIZE:
                if(fStationSamplingFile == null) throw new IllegalStateException("Need to specify " + StationSamplingMethod.POPULATION_SIZE + " source file using flag -STATION-SAMPLER-SOURCE-FILE");
                return new PopulationSizeStationSampler(fStationSamplingFile);
            default:
                throw new IllegalStateException("Specified --STATION-SAMPLER is invalid");
        }
    }

    public enum StationSamplingMethod {
        POPULATION_SIZE
    }
}
