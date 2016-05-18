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
package ca.ubc.cs.beta.stationpacking.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StationPackingUtils {

    private StationPackingUtils()
	{
		//Cannot construct a utils class.
	}
	
	public static final Integer LVHFmin = 2, LVHFmax = 6, UVHFmin=7, UVHFmax = 13, UHFmin = 14, UHFmax = 51;
	public static final HashSet<Integer> LVHF_CHANNELS = new HashSet<Integer>(Arrays.asList(2,3,4,5,6));
	public static final HashSet<Integer> HVHF_CHANNELS = new HashSet<Integer>(Arrays.asList(7,8,9,10,11,12,13));
	public static final HashSet<Integer> UHF_CHANNELS = new HashSet<Integer>(Arrays.asList(14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,38,39,40,41,42,43,44,45,46,47,48,49,50,51));

    public static Map<Station, Integer> stationToChannelFromChannelToStation(Map<Integer, Set<Station>> channelToStation) {
        final Map<Station, Integer> stationToChannel = new HashMap<>();
        channelToStation.entrySet().forEach(entry -> {
            entry.getValue().forEach(station -> {
                stationToChannel.put(station, entry.getKey());
            });
        });
        return stationToChannel;
    }

    public static String parseAuctionFromName(String name) {
        if (name != null) {
            try {
                return Splitter.on('_').splitToList(name).get(0);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static Map<Integer, Set<Station>> channelToStationFromStationToChannel(Map<Integer, Integer> stationToChannel) {
        final HashMultimap<Integer, Station> channelAssignment = HashMultimap.create();
        stationToChannel.entrySet().forEach(entry -> {
            channelAssignment.get(entry.getValue()).add(new Station(entry.getKey()));
        });
        return Multimaps.asMap(channelAssignment);
    }

    public static boolean weakVerify(IStationManager stationManager, IConstraintManager constraintManager, Map<Integer, Integer> solution) {
        return solution.keySet().stream().allMatch(s -> stationManager.getStations().contains(new Station(s))) && solution.entrySet().stream().allMatch(e -> stationManager.getDomain(stationManager.getStationfromID(e.getKey())).contains(e.getValue())) && constraintManager.isSatisfyingAssignment(channelToStationFromStationToChannel(solution));
    }

}
