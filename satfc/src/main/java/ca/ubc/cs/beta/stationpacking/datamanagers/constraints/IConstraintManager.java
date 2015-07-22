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
package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.util.*;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;

import com.google.common.collect.*;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Manages co- and adjacent- channel constraints.
 *
 * @author afrechet
 */
public interface IConstraintManager {

    /**
     * @param aStation - a (source) station of interest.
     * @param aChannel - a channel on which we wish to know interfering stations.
     * @return all the (target) stations that cannot be on the same given channel, <i> i.e. </i> if s is the
     * source station and c the given channel, then the set of stations T returned is such that, for all t in T,
     * <p>
     * s and t cannot be both on c
     * </p>
     */
    Set<Station> getCOInterferingStations(Station aStation, int aChannel);

    /**
     * @param aStation - a (source) station of interest.
     * @param aChannel - a channel on which we wish to know interfering stations.
     * @return all the (target) stations that cannot be on a channel that is one above the given channel on which the source station is, <i> i.e. </i> if s is the
     * source station and c the given channel, then the set of stations T returned is such that, for all t in T,
     * <p>
     * s cannot be on c at the same time as t is on c+1 for all c in C
     * </p>
     */
    Set<Station> getADJplusOneInterferingStations(Station aStation, int aChannel);

    /**
     * @param aStation - a (source) station of interest.
     * @param aChannel - a channel on which we wish to know interfering stations.
     * @return all the (target) stations that cannot be on a channel that is two above the given channel on which the source station is, <i> i.e. </i> if s is the
     * source station and c the given channel, then the set of stations T returned is such that, for all t in T,
     * <p>
     * s cannot be on c at the same time as t is on c+2 for all c in C
     * </p>
     */
    Set<Station> getADJplusTwoInterferingStations(Station aStation, int aChannel);

    /**
     * @param aAssignment - an assignment of channels to (set of) stations.
     * @return true if and only if the assignment satisfies all the constraints represented by the constraint manager.
     */
    boolean isSatisfyingAssignment(Map<Integer, Set<Station>> aAssignment);

    /**
     * @return a hash of the constraints
     */
    String getConstraintHash();

    /**
     * @param domains - The domain of a problem
     * @return all the constraints that apply to that problem
     */
    Iterable<Constraint> getAllRelevantConstraints(Map<Station, Set<Integer>> domains);

}
