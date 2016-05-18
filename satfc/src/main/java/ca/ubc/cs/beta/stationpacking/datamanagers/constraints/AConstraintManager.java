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
package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 06/03/15.
 */
@Slf4j
public abstract class AConstraintManager implements IConstraintManager {

    @Override
    public boolean isSatisfyingAssignment(Map<Integer, Set<Station>> aAssignment) {
        final Set<Station> allStations = new HashSet<Station>();
        for (Map.Entry<Integer, Set<Station>> entry : aAssignment.entrySet()) {
            final int channel = entry.getKey();
            final Set<Station> channelStations = aAssignment.get(channel);

            for (Station station1 : channelStations) {
                //Check if we have already seen station1, and add it to the set of seen stations
                if (!allStations.add(station1)) {
                    log.error("Station {} is assigned to multiple channels", station1);
                    return false;
                }
                //Make sure current station does not CO interfere with other stations.
                final Set<Station> coInterferingStations = getCOInterferingStations(station1, channel);
                for (Station station2 : channelStations) {
                    if (coInterferingStations.contains(station2)) {
                        log.trace("Station {} and {} share channel {} on which they CO interfere.", station1, station2, channel);
                        return false;
                    }
                }
                //Make sure current station does not ADJ+1 interfere with other stations.
                final Set<Station> adjInterferingStations = getADJplusOneInterferingStations(station1, channel);
                int channelp1 = channel + 1;
                final Set<Station> channelp1Stations = aAssignment.getOrDefault(channelp1, Collections.emptySet());
                for (Station station2 : channelp1Stations) {
                    if (adjInterferingStations.contains(station2)) {
                        log.trace("Station {} is on channel {}, and station {} is on channel {}, causing ADJ+1 interference.", station1, channel, station2, channelp1);
                        return false;
                    }
                }
                //Make sure current station does not ADJ+2 interfere with other stations.
                final Collection<Station> adjPlusTwoInterferingStations = getADJplusTwoInterferingStations(station1, channel);
                int channelp2 = channel + 2;
                final Set<Station> channelp2Stations = aAssignment.getOrDefault(channelp2, Collections.emptySet());
                for (Station station2 : channelp2Stations) {
                    if (adjPlusTwoInterferingStations.contains(station2)) {
                        log.trace("Station {} is on channel {}, and station {} is on channel {}, causing ADJ+2 interference.", station1, channel, station2, channelp2);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Iterable<Constraint> getAllRelevantConstraints(Map<Station, Set<Integer>> domains) {
        final Set<Station> stations = domains.keySet();
        final Collection<Constraint> constraintCollection = new ArrayList<>();
        for (Station sourceStation : stations) {
            for (Integer sourceChannel : domains.get(sourceStation)) {
                for (Station targetStation : getCOInterferingStations(sourceStation, sourceChannel)) {
                    if (stations.contains(targetStation) && domains.get(targetStation).contains(sourceChannel)) {
                        constraintCollection.add(new Constraint(sourceStation, targetStation, sourceChannel, sourceChannel));
                    }
                }
                for (Station targetStation : getADJplusOneInterferingStations(sourceStation, sourceChannel)) {
                    if (stations.contains(targetStation) && domains.get(targetStation).contains(sourceChannel + 1)) {
                        constraintCollection.add(new Constraint(sourceStation, targetStation, sourceChannel, sourceChannel + 1));
                    }
                }
                for (Station targetStation : getADJplusTwoInterferingStations(sourceStation, sourceChannel)) {
                    if (stations.contains(targetStation) && domains.get(targetStation).contains(sourceChannel + 2)) {
                        constraintCollection.add(new Constraint(sourceStation, targetStation, sourceChannel, sourceChannel + 2));
                    }
                }
            }
        }
        return constraintCollection;
    }

}
