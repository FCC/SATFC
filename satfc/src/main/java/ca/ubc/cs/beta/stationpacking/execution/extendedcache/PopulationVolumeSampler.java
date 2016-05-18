/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.execution.extendedcache;

import java.io.IOException;
import java.util.Random;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by emily404 on 6/4/15.
 */
@Slf4j
public class PopulationVolumeSampler implements IStationSampler {

    private WeightedCollection<Integer> weightedStationMap;
    /**
     * @param keep The set of stations we actually want to sample from
     * @throws IOException
     */
    public PopulationVolumeSampler(IStationDB stationDB, Set<Integer> keep, int seed)  {
        weightedStationMap = new WeightedCollection<>(new Random(seed));
        for (Integer stationID : keep) {
            double weight = ((double) stationDB.getPopulation(stationID)) / stationDB.getVolume(stationID);
            weightedStationMap.add(weight, stationID);
        }
    }

    @Override
    public Integer sample(Set<Integer> stationsAlreadyInProblem) {
        Integer station = weightedStationMap.next();
        log.debug("Sampling station...");

        while(stationsAlreadyInProblem.contains(station)){
            station = weightedStationMap.next();
        }

        log.debug("Sampled stationID: {}", station);
        return station;
    }

}
