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
package ca.ubc.cs.beta.stationpacking.solvers.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * An immutable container class that represents assignments of stations to channels (i.e. solutions to station packing problems)
 *  in an implementation-independent manner.

 * @author pcernek
 */
public class Assignment {

	private final ImmutableMap<Integer, ImmutableSet<Station>> channelStationMap;
	private final ImmutableMap<Station, Integer> stationChannelMap;
	
	/**
	 * This constructor is private to force use of the class's static maker methods. 
	 * This makes the Assignment class more agnostic to its underlying implementation.
	 * @param stationChannelMap - a mapping from channels to sets of stations, representing an assignment of stations to channels.
	 */
	private Assignment(Map<Station, Integer> stationChannelMap) {
		this.channelStationMap = toChannelStationMap(stationChannelMap);
		this.stationChannelMap = ImmutableMap.copyOf(stationChannelMap);
	}
	
	/**
	 * Returns an immutable set of the stations in this assignment.
	 * @return the set of stations described by this assignment
	 */
	public Set<Station> getStations() {
		return this.stationChannelMap.keySet();
	}
	
	/**
	 * Returns an immutable set of the channels in this assignment.
	 * @return the set of channels to which stations have been assigned in this assignment
	 */
	public Set<Integer> getChannels() {
		return this.channelStationMap.keySet();
	}
	
	/**
	 * Returns an immutable set of stations that have been assigned a given channel, or an empty set if
	 *  the channel is not contained in this assignment.
	 * @param channel - a channel for which we want to know which stations have been assigned to it.
	 * @return -the set of stations assigned to that channel, if the channel is contained in this assignment;
	 * 	otherwise, an empty set.
	 */
	public Set<Station> getStationsOnChannel(int channel) {
		if (this.channelStationMap.keySet().contains(channel)) {
			return this.channelStationMap.get(channel);
		}
		return Collections.emptySet();
	}
	
	/**
	 * Returns the channel to which a given station was assigned.
	 * @param station - the station for which we want to know the channel to which it was assigned.
	 * @throws IllegalArgumentException - if the given station is not contained in this assignment.
	 * @return - the channel to which the given station was assigned.
	 */
	public Integer getChannelOfStation(Station station) {
		if (this.stationChannelMap.keySet().contains(station)) {
			return this.stationChannelMap.get(station);
		}
		throw new IllegalArgumentException("Station " + station + " is not contained in this assignment.");
	}

	/**
	 * This method was introduced to facilitate the process of phasing over from various maps of different types,
	 *  to a single unified Assignment type. It should be removed once that transition is complete.
	 * @return - a mapping from stations to channels, representing an assignment of stations to channels.
	 */
	@Deprecated
	public Map<Station, Integer> toStationChannelMap() {
		return new HashMap<>(this.stationChannelMap);
	}

	/**
	 * This method was introduced to facilitate the process of phasing over from various maps of different types,
	 *  to a single unified Assignment type. It should be removed once that transition is complete.
	 * @return - a mapping from channels to sets of stations, representing an assignment of stations to channels.
	 */
	@Deprecated
	public Map<Integer, Set<Station>> toChannelStationMap() {
		return new HashMap<>(this.channelStationMap);
	}

	/**
	 * Constructs an Assignment object from the given mapping of stations to channels.
	 * @param stationChannelMap - the mapping from stations to integers to convert into an Assignment.
	 * @return - an Assignment object corresponding to the given map.
	 */
	public static Assignment fromStationChannelMap(Map<Station, Integer> stationChannelMap) {
		return new Assignment(stationChannelMap);
	}
	
	/**
	 * Constructs an Assignment object from the given mapping of channels to sets of stations.
	 * @param channelStationMap
	 * @return - an Assignment object corresponding to the given map.
	 */
	public static Assignment fromChannelStationMap(Map<Integer, Set<Station>> channelStationMap) {
		ImmutableMap<Station, Integer> stationChannelMap = toStationChannelMap(channelStationMap);
		return new Assignment(stationChannelMap);
	}

	private static ImmutableMap<Station, Integer> toStationChannelMap(Map<Integer, Set<Station>> channelStationMap )
	{
		ImmutableMap.Builder<Station, Integer> mapBuilder = new ImmutableMap.Builder<>();
		for (Integer channel: channelStationMap.keySet()) {
			for (Station station: channelStationMap.get(channel)) {
				mapBuilder.put(station, channel);
			}
		}
		return mapBuilder.build();
	}

	private static ImmutableMap<Integer, ImmutableSet<Station>> toChannelStationMap(
			Map<Station, Integer> stationIntegerMap ) 
	{
		Map<Integer, Set<Station>> tempMapWithMutableSets = swapStationsAndChannels(stationIntegerMap);
		Map<Integer, ImmutableSet<Station>> tempMapWithImmutableSets = makeSetsImmutable(tempMapWithMutableSets);
		
		return ImmutableMap.copyOf(tempMapWithImmutableSets);
	}

	private static Map<Integer, Set<Station>> swapStationsAndChannels(
			Map<Station, Integer> stationIntegerMap) {
		Map<Integer, Set<Station>> tempMapWithMutableSets = new HashMap<>();
		for (Station station: stationIntegerMap.keySet()) {
			Integer channel = stationIntegerMap.get(station);
			tempMapWithMutableSets.computeIfAbsent(channel, c -> new HashSet<>());
			tempMapWithMutableSets.get(channel).add(station);
		}
		return tempMapWithMutableSets;
	}

	private static Map<Integer, ImmutableSet<Station>> makeSetsImmutable(
			Map<Integer, Set<Station>> mapWithMutableSets)
		{
		Map<Integer, ImmutableSet<Station>> tempMapWithImmutableSets = new HashMap<>();
		for (Integer channel: mapWithMutableSets.keySet()) {
			ImmutableSet<Station> immutableStations = ImmutableSet.copyOf(mapWithMutableSets.get(channel));
			tempMapWithImmutableSets.put(channel, immutableStations);
		}
		return tempMapWithImmutableSets;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Assignment that = (Assignment) o;

		return channelStationMap.equals(that.channelStationMap);

	}

	@Override
	public int hashCode() {
		return channelStationMap.hashCode();
	}
}
