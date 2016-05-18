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

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import ca.ubc.cs.beta.stationpacking.execution.AProblemReader;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.problemparsers.IProblemParser;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 12/05/15.
 */
@Slf4j
public class FileProblemReader extends AProblemReader {

    private final IProblemParser nameToProblem;
    private final List<String> instanceFiles;
    private int listIndex = 0;

    public FileProblemReader(String fileOfSrpkFiles, IProblemParser nameToProblem) {
        this.nameToProblem = nameToProblem;
        log.info("Reading instances from file {}", fileOfSrpkFiles);
        try {
            instanceFiles = Files.readLines(new File(fileOfSrpkFiles), Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read instance files from " + fileOfSrpkFiles, e);
        }
    }

    @Override
    public SATFCFacadeProblem getNextProblem() {
        SATFCFacadeProblem problem = null;
        while (listIndex < instanceFiles.size()) {
            String instanceFile = instanceFiles.get(listIndex++);
            try {
                problem = nameToProblem.problemFromName(instanceFile);
                break;
            } catch (IOException e) {
                log.warn("Error parsing file " + instanceFile, e);
            }
        }
        if (problem != null) {
            log.info("This is problem {} out of {}", index, instanceFiles.size());
            return problem;
        } else {
            return null;
        }
    }

}
