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
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Channel specific interference constraint data manager.
 *
 * @author afrechet, narnosti, tqichen
 */
@Slf4j
public class ChannelSpecificConstraintManager extends AMapBasedConstraintManager {

    /**
     * Construct a Channel Specific Constraint Manager from a station manager and an interference constraints filename.
     *
     * @param aStationManager                  - station manager.
     * @param aInterferenceConstraintsFilename - name of the file containing interference constraints.
     * @throws FileNotFoundException - if indicated file cannot be found.
     */
    public ChannelSpecificConstraintManager(IStationManager aStationManager, String aInterferenceConstraintsFilename) throws FileNotFoundException {
        super(aStationManager, aInterferenceConstraintsFilename);
    }

    /**
     * Add the constraint to the constraint manager represented by subject station, target station, subject channel and constraint key.
     *
     * @param aSubjectStation
     * @param aTargetStation
     * @param aSubjectChannel
     * @param aConstraintKey
     */
    @Override
    protected void addConstraint(Station aSubjectStation,
                               Station aTargetStation,
                               Integer aSubjectChannel,
                               ConstraintKey aConstraintKey) {
        super.addConstraint(aSubjectStation, aTargetStation, aSubjectChannel, aConstraintKey);
        switch (aConstraintKey) {
            case CO:
                break;
            case ADJp1:
                //Add implied CO constraints;
                super.addConstraint(aSubjectStation, aTargetStation, aSubjectChannel, ConstraintKey.CO);
                super.addConstraint(aSubjectStation, aTargetStation, aSubjectChannel + 1, ConstraintKey.CO);
                break;
            case ADJp2:
                // Add implied CO constraints
                super.addConstraint(aSubjectStation, aTargetStation, aSubjectChannel, ConstraintKey.CO);
                super.addConstraint(aSubjectStation, aTargetStation, aSubjectChannel + 1, ConstraintKey.CO);
                super.addConstraint(aSubjectStation, aTargetStation, aSubjectChannel + 2, ConstraintKey.CO);

                // Add impied +1 constraints
                super.addConstraint(aSubjectStation, aTargetStation, aSubjectChannel, ConstraintKey.ADJp1);
                super.addConstraint(aSubjectStation, aTargetStation, aSubjectChannel + 1, ConstraintKey.ADJp1);
                break;
            default:
                throw new IllegalStateException("Unrecognized constraint key " + aConstraintKey);
        }
    }

    @Override
    protected void loadConstraints(IStationManager aStationManager, String aInterferenceConstraintsFilename) throws FileNotFoundException {
        try {
            try (CSVReader reader = new CSVReader(new FileReader(aInterferenceConstraintsFilename))) {
                String[] line;
                while ((line = reader.readNext()) != null) {
                    try {
                        final String key = line[0].trim();
                        final ConstraintKey constraintKey = ConstraintKey.fromString(key);
                        if (constraintKey.equals(ConstraintKey.ADJm1) || constraintKey.equals(ConstraintKey.ADJm2)) {
                            throw new IllegalArgumentException("ADJ-1 and ADJ-2 constraints are not part of the compact format, but were seen in line:" + Arrays.toString(line));
                        }

                        final int lowChannel = Integer.valueOf(line[1].trim());
                        final int highChannel = Integer.valueOf(line[2].trim());
                        if (lowChannel > highChannel) {
                            throw new IllegalStateException("Low channel greater than high channel on line " + Arrays.toString(line));
                        }

                        final int subjectStationID = Integer.valueOf(line[3].trim());
                        final Station subjectStation = aStationManager.getStationfromID(subjectStationID);

                        for (int subjectChannel = lowChannel; subjectChannel <= highChannel; subjectChannel++) {
                            for (int i = 4; i < line.length; i++) {
                                if (line[i].trim().isEmpty()) {
                                    break;
                                }
                                final int targetStationID = Integer.valueOf(line[i].trim());
                                final Station targetStation = aStationManager.getStationfromID(targetStationID);

                                addConstraint(subjectStation, targetStation, subjectChannel, constraintKey);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Could not read constraint from line:\n{}", StringUtils.join(line, ','));
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read interference constraints file: " + aInterferenceConstraintsFilename, e);
        }
    }

}
