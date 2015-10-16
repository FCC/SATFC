package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by emily404 on 6/4/15.
 */
@Slf4j
public class PopulationSizeStationSampler implements IStationSampler {

    private WeightedCollection<Integer> populationMap = new WeightedCollection<>();

    public PopulationSizeStationSampler(String populationFile) throws IOException{
        // load population file, put into data structure
        Path path = FileSystems.getDefault().getPath(populationFile);
        Files.lines(path)
                .skip(1) // skip csv header line
                .forEach(line -> {
                    String[] split = line.split(",");
                    Integer station = Integer.parseInt(split[0]);
                    Double density = Double.parseDouble(split[1]);
                    populationMap.add(density, station);
                });

    }

    /**
     * Sample stations based on population density that the station covers,
     * stations with higher population density are more likely to be selected
     * @param stationsInProblem a set representing stations that are present in a problem
     * @return stationID of the station to be added
     */
    @Override
    public Integer sample(Set<Integer> stationsInProblem) {
        Integer station = populationMap.next();
        log.debug("Sampling station...");

        while(stationsInProblem.contains(station)){
            station = populationMap.next();
        }

        log.debug("Sampled stationID: " + station);
        return station;
    }

}
