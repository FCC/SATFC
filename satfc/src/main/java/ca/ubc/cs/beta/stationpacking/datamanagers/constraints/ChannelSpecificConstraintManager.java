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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

import com.google.common.hash.HashCode;

/**
 * Channel specific interference constraint data manager.
 *
 * @author afrechet, narnosti, tqichen
 */
@Slf4j
public class ChannelSpecificConstraintManager extends AConstraintManager {

    /*
     * Map taking subject station to map taking channel to interfering station that cannot be
     * on channel concurrently with subject station.
     */

    /**
     * Type of possible constraints.
     *
     * @author afrechet
     */

    /**
     * Add the constraint to the constraint manager represented by subject station, target station, subject channel and constraint key.
     *
     * @param aSubjectStation
     * @param aTargetStation
     * @param aSubjectChannel
     * @param aConstraintKey
     */
    private void addConstraint(Station aSubjectStation,
                               Station aTargetStation,
                               Integer aSubjectChannel,
                               ConstraintKey aConstraintKey) {
        Map<Integer, Set<Station>> subjectStationConstraints;
        Set<Station> interferingStations;

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

                subjectStationConstraints = fCOConstraints.get(aSubjectStation);
                if (subjectStationConstraints == null) {
                    subjectStationConstraints = new HashMap<Integer, Set<Station>>();
                }

                interferingStations = subjectStationConstraints.get(aSubjectChannel);
                if (interferingStations == null) {
                    interferingStations = new HashSet<Station>();
                }

                interferingStations.add(aTargetStation);

                subjectStationConstraints.put(aSubjectChannel, interferingStations);
                fCOConstraints.put(aSubjectStation, subjectStationConstraints);
                break;

            case ADJp1:

                //Add CO constraints;
                addConstraint(aSubjectStation, aTargetStation, aSubjectChannel, ConstraintKey.CO);
                addConstraint(aSubjectStation, aTargetStation, aSubjectChannel + 1, ConstraintKey.CO);

                //Add +1 constraint;
                subjectStationConstraints = fADJp1Constraints.get(aSubjectStation);
                if (subjectStationConstraints == null) {
                    subjectStationConstraints = new HashMap<Integer, Set<Station>>();
                }

                interferingStations = subjectStationConstraints.get(aSubjectChannel);
                if (interferingStations == null) {
                    interferingStations = new HashSet<Station>();
                }

                interferingStations.add(aTargetStation);

                subjectStationConstraints.put(aSubjectChannel, interferingStations);
                fADJp1Constraints.put(aSubjectStation, subjectStationConstraints);

                break;

            case ADJm1:
                //Add corresponding reverse ADJ+1 constraint.
                addConstraint(aTargetStation, aSubjectStation, aSubjectChannel - 1, ConstraintKey.ADJp1);
                break;

            default:
                throw new IllegalStateException("Unrecognized constraint key " + aConstraintKey);
        }
    }

    /**
     * Construct a Channel Specific Constraint Manager from a station manager and an interference constraints filename.
     *
     * @param aStationManager                  - station manager.
     * @param aInterferenceConstraintsFilename - name of the file containing interference constraints.
     * @throws FileNotFoundException - if indicated file cannot be found.
     */
    public ChannelSpecificConstraintManager(IStationManager aStationManager, String aInterferenceConstraintsFilename) throws FileNotFoundException {
        super(aStationManager, aInterferenceConstraintsFilename);
        try {
            try (CSVReader reader = new CSVReader(new FileReader(aInterferenceConstraintsFilename))) {
                String[] line;
                while ((line = reader.readNext()) != null) {
                    try {
                        String key = line[0].trim();
                        ConstraintKey constraintKey;
                        if (key.equals("CO")) {
                            constraintKey = ConstraintKey.CO;
                        } else if (key.equals("ADJ+1")) {
                            constraintKey = ConstraintKey.ADJp1;
                        } else if (key.equals("ADJ-1")) {
                            constraintKey = ConstraintKey.ADJm1;
                        } else {
                            throw new IllegalArgumentException("Unrecognized constraint key " + key);
                        }

                        int lowChannel = Integer.valueOf(line[1].trim());
                        int highChannel = Integer.valueOf(line[2].trim());
                        if (lowChannel > highChannel) {
                            throw new IllegalStateException("Low channel greater than high channel.");
                        }

                        int subjectStationID = Integer.valueOf(line[3].trim());
                        Station subjectStation = aStationManager.getStationfromID(subjectStationID);

                        for (int subjectChannel = lowChannel; subjectChannel <= highChannel; subjectChannel++) {
                            for (int i = 4; i < line.length; i++) {
                                if (line[i].trim().isEmpty()) {
                                    break;
                                }
                                int targetStationID = Integer.valueOf(line[i].trim());

                                Station targetStation = aStationManager.getStationfromID(targetStationID);

                                addConstraint(subjectStation, targetStation, subjectChannel, constraintKey);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Could not read constraint from line:\n{}", StringUtils.join(line, ','));
                        throw e;
                    }


                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not read interference constraints filename.");
        }

        HashCode hc = computeHash();
        fHash = hc.toString();
    }





}
