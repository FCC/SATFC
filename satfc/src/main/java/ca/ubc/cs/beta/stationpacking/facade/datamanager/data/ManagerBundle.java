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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.data;

import com.google.common.collect.ImmutableBiMap;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import containmentcache.util.PermutationUtils;
import lombok.Getter;

/**
 * Bundle object containing a station and a constraint manager.
 */
public class ManagerBundle {

	@Getter
	private IStationManager stationManager;
	@Getter
	private IConstraintManager constraintManager;
	@Getter
	private final String interferenceFolder;
	@Getter
	private CacheCoordinate cacheCoordinate;
	@Getter
	private final ImmutableBiMap<Station, Integer> permutation;

	/**
	 * Creates a new bundle containing the given station and constraint manager.
	 * @param stationManager station manager to be bundled.
	 * @param constraintManager constraint manager to be bundled.
	 */
	public ManagerBundle(IStationManager stationManager, IConstraintManager constraintManager, String interferenceFolder) {
		this.stationManager = stationManager;
		this.constraintManager = constraintManager;
		this.interferenceFolder = interferenceFolder;
		cacheCoordinate = new CacheCoordinate(stationManager.getDomainHash(), constraintManager.getConstraintHash());
		permutation = PermutationUtils.makePermutation(getStationManager().getStations());
	}

    public boolean isCompactInterference() {
        return getConstraintManager() instanceof ChannelSpecificConstraintManager;
    }

}
