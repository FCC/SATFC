/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.Converter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Created by newmanne on 21/04/15.
 */
public class StationPackingTestUtils {

    public static StationPackingInstance getSimpleInstance() {
        return new StationPackingInstance(ImmutableMap.of(new Station(1), ImmutableSet.of(1)));
    }

    public static Map<Integer, Set<Station>> getSimpleInstanceAnswer() {
        return ImmutableMap.of(1, ImmutableSet.of(new Station(1)));
    }

    public static StationPackingInstance instanceFromSpecs(Converter.StationPackingProblemSpecs specs, IStationManager stationManager) {
        final Map<Station, Set<Integer>> domains = new HashMap<>();
        for (Integer stationID : specs.getDomains().keySet()) {
            Station station = stationManager.getStationfromID(stationID);

            Set<Integer> domain = specs.getDomains().get(stationID);
            Set<Integer> stationDomain = stationManager.getDomain(station);

            Set<Integer> truedomain = Sets.intersection(domain, stationDomain);

            domains.put(station, truedomain);
        }

        final Map<Station, Integer> previousAssignment = new HashMap<>();
        for (Station station : domains.keySet()) {
            Integer previousChannel = specs.getPreviousAssignment().get(station.getID());
            if (previousChannel != null && previousChannel > 0) {
                previousAssignment.put(station, previousChannel);
            }
        }

        return new StationPackingInstance(domains, previousAssignment);
    }

}
