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
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Unabridged format constraint manager for specific format that is released to the public by the FCC.
 *
 * @author afrechet
 */
@Slf4j
public class UnabridgedFormatConstraintManager extends AMapBasedConstraintManager {

    /**
     * Construct a Channel Specific Constraint Manager from a station manager and an interference constraints filename.
     *
     * @param aStationManager                  - station manager.
     * @param aInterferenceConstraintsFilename - name of the file containing interference constraints.
     * @throws FileNotFoundException - if indicated file cannot be found.
     */
    public UnabridgedFormatConstraintManager(IStationManager aStationManager, String aInterferenceConstraintsFilename) throws FileNotFoundException {
        super(aStationManager, aInterferenceConstraintsFilename);
    }

    @Override
    protected void loadConstraints(IStationManager aStationManager, String aInterferenceConstraintsFilename) throws FileNotFoundException {
        try {
            try (CSVReader reader = new CSVReader(new FileReader(aInterferenceConstraintsFilename))) {
                String[] line;
                while ((line = reader.readNext()) != null) {
                    final String key = line[0].trim();
                    final ConstraintKey constraintKey = ConstraintKey.fromString(key);
                    final int subjectChannel = Integer.valueOf(line[1].trim());
                    final int targetChannel = Integer.valueOf(line[2].trim());

                    if (constraintKey.equals(ConstraintKey.CO) && subjectChannel != targetChannel) {
                        throw new IllegalArgumentException(Arrays.toString(line) + System.lineSeparator() + "Constraint key is " + constraintKey + " but subject channel (" + subjectChannel + ") is different than target channel (" + targetChannel + ").");
                    } else if (constraintKey.equals(ConstraintKey.ADJp1) && subjectChannel != targetChannel - 1) {
                        throw new IllegalArgumentException(Arrays.toString(line) + System.lineSeparator() + "Constraint key is " + constraintKey + " but subject channel (" + subjectChannel + ") is not one less than target channel (" + targetChannel + ").");
                    } else if (constraintKey.equals(ConstraintKey.ADJm1) && subjectChannel != targetChannel + 1) {
                        throw new IllegalArgumentException(Arrays.toString(line) + System.lineSeparator() + "Constraint key is " + constraintKey + " but subject channel (" + subjectChannel + ") is not one more than target channel (" + targetChannel + ").");
                    } else if (constraintKey.equals(ConstraintKey.ADJp2) && subjectChannel != targetChannel - 2) {
                        throw new IllegalArgumentException(Arrays.toString(line) + System.lineSeparator() + "Constraint key is " + constraintKey + " but subject channel (" + subjectChannel + ") is not two less than target channel (" + targetChannel + ").");
                    } else if (constraintKey.equals(ConstraintKey.ADJm2) && subjectChannel != targetChannel + 2) {
                        throw new IllegalArgumentException(Arrays.toString(line) + System.lineSeparator() + "Constraint key is " + constraintKey + " but subject channel (" + subjectChannel + ") is not two more than target channel (" + targetChannel + ").");
                    }

                    final int subjectStationID = Integer.valueOf(line[3].trim());
                    final Station subjectStation = aStationManager.getStationfromID(subjectStationID);

                    for (int i = 4; i < line.length; i++) {
                        if (line[i].trim().isEmpty()) {
                            break;
                        }
                        final int targetStationID = Integer.valueOf(line[i].trim());
                        final Station targetStation = aStationManager.getStationfromID(targetStationID);

                        addConstraint(subjectStation, targetStation, subjectChannel, constraintKey);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read interference constraints filename", e);
        }
    }

}
