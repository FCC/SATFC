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

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

public abstract class AMapBasedConstraintManager extends AConstraintManager {

    /*
     * Map taking subject station to map taking channel to interfering station that cannot be
     * on channel concurrently with subject station.
     */
    protected final Map<Station, Map<Integer, Set<Station>>> fCOConstraints;

    /*
     * Map taking subject station to map taking channel to interfering station that cannot be
     * on channel+1 concurrently with subject station.
     */
    protected final Map<Station, Map<Integer, Set<Station>>> fADJp1Constraints;

    /*
     * Map taking subject station to map taking channel to interfering station that cannot be
     * on channel+2 concurrently with subject station.
     */
    protected final Map<Station, Map<Integer, Set<Station>>> fADJp2Constraints;

    protected String fHash;

    public AMapBasedConstraintManager(IStationManager aStationManager, String aInterferenceConstraintsFilename) throws FileNotFoundException {
        fCOConstraints = new HashMap<>();
        fADJp1Constraints = new HashMap<>();
        fADJp2Constraints = new HashMap<>();
        loadConstraints(aStationManager, aInterferenceConstraintsFilename);
        final HashCode hc = computeHash();
        fHash = hc.toString();
    }

    private Set<Station> getInterferingStations(Station aStation, int aChannel, Map<Station, Map<Integer, Set<Station>>> constraintMap) {
        final Map<Integer, Set<Station>> subjectStationConstraints = constraintMap.getOrDefault(aStation, Collections.emptyMap());
        final Set<Station> interferingStations = subjectStationConstraints.getOrDefault(aChannel, Collections.emptySet());
        return Collections.unmodifiableSet(interferingStations);
    }

    @Override
    public Set<Station> getCOInterferingStations(Station aStation, int aChannel) {
        return getInterferingStations(aStation, aChannel, fCOConstraints);
    }

    @Override
    public Set<Station> getADJplusOneInterferingStations(Station aStation, int aChannel) {
        return getInterferingStations(aStation, aChannel, fADJp1Constraints);
    }

    @Override
    public Set<Station> getADJplusTwoInterferingStations(Station aStation, int aChannel) {
        return getInterferingStations(aStation, aChannel, fADJp2Constraints);
    }

    /**
     * Add the constraint to the constraint manager represented by subject station, target station, subject channel and constraint key.
     *
     * @param aSubjectStation
     * @param aTargetStation
     * @param aSubjectChannel
     * @param aConstraintKey
     */
    protected void addConstraint(Station aSubjectStation,
                                 Station aTargetStation,
                                 Integer aSubjectChannel,
                                 ConstraintKey aConstraintKey) {
        final Map<Integer, Set<Station>> subjectStationConstraints;
        final Set<Station> interferingStations;

        switch (aConstraintKey) {
            case CO:
                /*
                 * Switch subject station for target station depending on the ID of the stations
                 * to remove possible duplicate CO interference clauses.
                 */
                if (aSubjectStation.getID() > aTargetStation.getID()) {
                    Station tempStation = aSubjectStation;
                    aSubjectStation = aTargetStation;
                    aTargetStation = tempStation;
                }

                subjectStationConstraints = fCOConstraints.getOrDefault(aSubjectStation, new HashMap<>());
                interferingStations = subjectStationConstraints.getOrDefault(aSubjectChannel, new HashSet<>());

                interferingStations.add(aTargetStation);

                subjectStationConstraints.put(aSubjectChannel, interferingStations);
                fCOConstraints.put(aSubjectStation, subjectStationConstraints);
                break;
            case ADJp1:
                //Add +1 constraint;
                subjectStationConstraints = fADJp1Constraints.getOrDefault(aSubjectStation, new HashMap<>());
                interferingStations = subjectStationConstraints.getOrDefault(aSubjectChannel, new HashSet<>());

                interferingStations.add(aTargetStation);

                subjectStationConstraints.put(aSubjectChannel, interferingStations);
                fADJp1Constraints.put(aSubjectStation, subjectStationConstraints);
                break;
            case ADJm1:
                //Add corresponding reverse ADJ+1 constraint.
                addConstraint(aTargetStation, aSubjectStation, aSubjectChannel - 1, ConstraintKey.ADJp1);
                break;
            case ADJp2:
                //Add +2 constraint;
                subjectStationConstraints = fADJp2Constraints.getOrDefault(aSubjectStation, new HashMap<>());
                interferingStations = subjectStationConstraints.getOrDefault(aSubjectChannel, new HashSet<>());

                interferingStations.add(aTargetStation);

                subjectStationConstraints.put(aSubjectChannel, interferingStations);
                fADJp2Constraints.put(aSubjectStation, subjectStationConstraints);
                break;
            case ADJm2:
                //Add corresponding reverse ADJ+2 constraint.
                addConstraint(aTargetStation, aSubjectStation, aSubjectChannel - 2, ConstraintKey.ADJp2);
                break;
            default:
                throw new IllegalStateException("Unrecognized constraint key " + aConstraintKey);
        }
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

        final HashFunction hf = Hashing.murmur3_32();
        return hf.newHasher()
                .putObject(fCOConstraints, funnel)
                .putObject(fADJp1Constraints, funnel)
                .putObject(fADJp2Constraints, funnel)
                .hash();
    }

    protected abstract void loadConstraints(IStationManager stationManager, String fileName) throws FileNotFoundException;

    @Override
    public String getConstraintHash() {
        return fHash;
    }

}
