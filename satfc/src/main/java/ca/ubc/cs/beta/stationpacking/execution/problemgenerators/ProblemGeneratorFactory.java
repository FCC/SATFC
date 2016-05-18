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

import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.problemparsers.CsvToProblem;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.problemparsers.IProblemParser;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.problemparsers.SrpkToProblem;

/**
 * Created by newmanne on 12/05/15.
 */
public class ProblemGeneratorFactory {

    public static IProblemReader createFromParameters(SATFCFacadeParameters parameters) {
        IProblemReader reader;
        IProblemParser nameToProblem = parameters.fCsvRoot == null ? new SrpkToProblem(parameters.fInterferencesFolder) : new CsvToProblem(parameters.fInterferencesFolder, parameters.fCsvRoot, parameters.checkForSolution);
        if (parameters.fInstanceParameters.fDataFoldername != null && parameters.fInstanceParameters.getDomains() != null) {
            reader = new SingleProblemFromCommandLineProblemReader(new SATFCFacadeProblem(
                    parameters.fInstanceParameters.getPackingStationIDs(),
                    parameters.fInstanceParameters.getPackingChannels(),
                    parameters.fInstanceParameters.getDomains(),
                    parameters.fInstanceParameters.getPreviousAssignment(),
                    parameters.fInstanceParameters.fDataFoldername,
                    null
            ));
        } else if (parameters.fsrpkFile != null) {
            reader = new SingleSrpkProblemReader(parameters.fsrpkFile, nameToProblem);
        } else if (parameters.fRedisParameters.areValid() && parameters.fInterferencesFolder != null) {
            reader = new RedisProblemReader(parameters.fRedisParameters.getJedis(), parameters.fRedisParameters.fRedisQueue, nameToProblem, parameters.checkForSolution);
        } else if (parameters.fFileOfInstanceFiles != null && parameters.fInterferencesFolder != null) {
            reader = new FileProblemReader(parameters.fFileOfInstanceFiles, nameToProblem);
        } else {
            throw new IllegalArgumentException("Illegal parameters provided. Must provide -DATA-FOLDERNAME and -DOMAINS. Please consult the SATFC manual for examples");
        }
        return reader;
    }
}
