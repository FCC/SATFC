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

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Created by newmanne on 06/03/15.
 */
@Slf4j
public abstract class AConstraintManager implements IConstraintManager {

    protected final Map<Station, Map<Integer, Set<Station>>> fCOConstraints;

    /*
     * Map taking subject station to map taking channel to interfering station that cannot be
     * on channel+1 concurrently with subject station.
     */
    protected final Map<Station, Map<Integer, Set<Station>>> fADJp1Constraints;
    protected String fHash;

    protected AConstraintManager(IStationManager aStationManager, String aInterferenceConstraintsFilename) throws FileNotFoundException {
        fCOConstraints = new HashMap<>();
        fADJp1Constraints = new HashMap<>();
    }

    protected enum ConstraintKey {
        //Co-channel constraints,
        CO,
        //ADJ+1 channel constraints,
        ADJp1,
        //ADJ-1 channel constraints (should not appear in new format),
        ADJm1;
    }

    @Override
    public Set<Station> getCOInterferingStations(
            Station aStation, int aChannel) {
        Map<Integer, Set<Station>> subjectStationConstraints = fCOConstraints.get(aStation);
        //No constraint for this station.
        if (subjectStationConstraints == null) {
            return Collections.emptySet();
        }

        Set<Station> interferingStations = subjectStationConstraints.get(aChannel);
        //No constraint for this station on this channel.
        if (interferingStations == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(interferingStations);
    }

    @Override
    public Set<Station> getADJplusInterferingStations(
            Station aStation,
            int aChannel) {
        Map<Integer, Set<Station>> subjectStationConstraints = fADJp1Constraints.get(aStation);
        //No constraint for this station.
        if (subjectStationConstraints == null) {
            return Collections.emptySet();
        }

        Set<Station> interferingStations = subjectStationConstraints.get(aChannel);
        //No constraint for this station on this channel.
        if (interferingStations == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(interferingStations);
    }

    @Override
    public boolean isSatisfyingAssignment(Map<Integer, Set<Station>> aAssignment) {

        Set<Station> allStations = new HashSet<Station>();

        for(Integer channel : aAssignment.keySet())
        {
            Set<Station> channelStations = aAssignment.get(channel);

            for(Station station1 : channelStations)
            {
                //Check if we have already seen station1
                if(allStations.contains(station1))
                {
                    log.debug("Station {} is assigned to multiple channels.");
                    return false;
                }

                //Make sure current station does not CO interfere with other stations.
                Collection<Station> coInterferingStations = getCOInterferingStations(station1, channel);
                for(Station station2 : channelStations)
                {
                    if(coInterferingStations.contains(station2))
                    {
                        log.debug("Station {} and {} share channel {} on which they CO interfere.", station1, station2, channel);
                        return false;
                    }
                }

                //Make sure current station does not ADJ+1 interfere with other stations.
                Collection<Station> adjInterferingStations = getADJplusInterferingStations(station1, channel);
                int channelp1 = channel+1;
                Set<Station> channelp1Stations = aAssignment.get(channelp1);
                if(channelp1Stations!=null)
                {
                    for(Station station2 : channelp1Stations)
                    {
                        if(adjInterferingStations.contains(station2))
                        {
                            log.debug("Station {} is on channel {}, and station {} is on channel {}, causing ADJ+1 interference.", station1, channel, station2, channelp1);
                            return false;
                        }
                    }
                }
            }
            allStations.addAll(channelStations);
        }
        return true;
    }

    protected HashCode computeHash() {
        final Funnel<Map<Station, Map<Integer, Set<Station>>>> funnel = (from, into) -> from.keySet().stream().sorted().forEach(s -> {
            into.putInt(s.getID());
            from.get(s).keySet().stream().sorted().forEach(c -> {
                into.putInt(c);
                from.get(s).get(c).stream().sorted().forEach(s2 -> {
                    into.putInt(s2.getID());
                });
            });
        });

        HashFunction hf = Hashing.murmur3_32();
        return hf.newHasher()
                .putObject(fCOConstraints, funnel)
                .putObject(fADJp1Constraints, funnel)
                .hash();
    }

    @Override
    public String getHashCode() {
        return fHash;
    }

}
