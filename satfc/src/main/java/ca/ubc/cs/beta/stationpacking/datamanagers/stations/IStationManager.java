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
package ca.ubc.cs.beta.stationpacking.datamanagers.stations;

import java.util.HashSet;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;


/**
 * Interface for objects in charge of managing a collection of stations.
 * @author afrechet
 */
public interface IStationManager {

	/**
	 * @return - all the stations represented by the station manager.
	 */
	Set<Station> getStations();
	
	/**
	 * 
	 * @param aID - a station ID.
	 * @return the station for the particular ID.
	 * @throws IllegalArgumentException - if the provided ID cannot be found in the stations.
	 */
	Station getStationfromID(Integer aID) throws IllegalArgumentException;
	
	/**
	 * @param aStation - a station.
	 * @return the channels on which this station can be packed.
	 */
	Set<Integer> getDomain(Station aStation);

	default Set<Integer> getRestrictedDomain(Station station, int maxChannel, boolean UHFOnly) {
		final Set<Integer> domain = new HashSet<>(getDomain(station));
		domain.removeIf(chan -> chan > maxChannel || (UHFOnly && chan < StationPackingUtils.UHFmin));
		return domain;
	}
	
	/**
	 * @return a hash of the domain
	 */
    String getDomainHash();
	
}
