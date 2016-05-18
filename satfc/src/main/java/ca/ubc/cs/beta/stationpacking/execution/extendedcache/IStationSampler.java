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

import java.util.Set;

/**
 * Created by emily404 on 6/4/15.
 */
public interface IStationSampler {

    /**
     * Determine a new station to add to the problem based on the sampling method
     * {@link ca.ubc.cs.beta.stationpacking.execution.extendedcache.StationSamplerParameters.StationSamplingMethod}
     * @param stationsInProblem a set representing stations that are present in a problem
     * @return stationID of the station to be added
     */
    Integer sample(Set<Integer> stationsAlreadyInProblem);
}
