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

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import au.com.bytecode.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;

/**
* Created by newmanne on 15/10/15.
*/
@Slf4j
public class CSVStationDB implements IStationDB {

    final Map<Integer, Integer> stationToPopulation;
    final Map<Integer, Double> stationToVolume;

    /**
     * @param stationFile a path to a csv file with a header and columns "id, population, volume"
     */
    public CSVStationDB(String stationFile) {
        log.info("Parsing {} for station info", stationFile);
        stationToPopulation = new HashMap<>();
        stationToVolume = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(stationFile))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                final int id = Integer.parseInt(line[0].trim());
                final int population = Integer.parseInt(line[1].trim());
                final double volume = Double.parseDouble(line[2].trim());
                Preconditions.checkState(!stationToVolume.containsKey(id) && !stationToVolume.containsKey(id), "Previous value associated with station id %d", id);
                stationToPopulation.put(id, population);
                stationToVolume.put(id, volume);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read station information file: " + stationFile, e);
        }
    }

    @Override
    public int getPopulation(int id) {
        Preconditions.checkState(stationToPopulation.containsKey(id), "No known population for station with id %d", id);
        return stationToPopulation.get(id);
    }

    @Override
    public double getVolume(int id) {
        Preconditions.checkState(stationToPopulation.containsKey(id), "No known population for station with id %d", id);
        return stationToVolume.get(id);
    }
}
