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

import java.io.IOException;

import ca.ubc.cs.beta.stationpacking.execution.AProblemReader;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.problemparsers.IProblemParser;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 11/06/15.
 */
@Slf4j
public class SingleSrpkProblemReader extends AProblemReader {

    private final String srpkFile;
    private final IProblemParser nameToProblem;

    public SingleSrpkProblemReader(String srpkFile, IProblemParser nameToProblem) {
        this.srpkFile = srpkFile;
        this.nameToProblem = nameToProblem;
    }

    @Override
    public SATFCFacadeProblem getNextProblem() {
        if (index != 1) {
            return null;
        }
        final SATFCFacadeProblem problem;
        try {
            problem = nameToProblem.problemFromName(srpkFile);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing file " + srpkFile, e);
        }
        return problem;
    }

}
