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
package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 2016-02-16.
 */
@Slf4j
public class FileCutoffChooser implements ICutoffChooser {

    private final Map<String, Double> nameToRuntime;
    private final double defaultCutoff;

    public FileCutoffChooser(String cutoffFile, double defaultCutoff) throws FileNotFoundException {
        this.defaultCutoff = defaultCutoff;
        nameToRuntime = new HashMap<>();
        final CSVReader csvReader = new CSVReader(new FileReader(cutoffFile), ',');
        String[] line = null;
        try {
            while ((line = csvReader.readNext()) != null) {
                String instanceName = line[0];
                double cutoff = Double.parseDouble(line[1]);
                nameToRuntime.put(instanceName, cutoff);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse line " + Arrays.toString(line), e);
        }
    }

    @Override
    public double getCutoff(SATFCFacadeProblem problem) {
        return nameToRuntime.getOrDefault(problem.getInstanceName(), defaultCutoff);
    }

}
