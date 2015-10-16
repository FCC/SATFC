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
package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;

/**
 * Created by newmanne on 12/05/15.
 */
public class ProblemGeneratorFactory {

    public static IProblemReader createFromParameters(SATFCFacadeParameters parameters) {
        IProblemReader reader;
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
            reader = new SingleSrpkProblemReader(parameters.fsrpkFile, parameters.fInterferencesFolder);
        } else if (parameters.fRedisParameters.areValid() && parameters.fInterferencesFolder != null && parameters.cachingParams.extendedCacheProblem){
            reader = new ExtendedCacheProblemReader(parameters.fRedisParameters.getJedis(), parameters.fRedisParameters.fRedisQueue, parameters.fInterferencesFolder);
        } else if (parameters.fRedisParameters.areValid() && parameters.fInterferencesFolder != null) {
            reader = new RedisProblemReader(parameters.fRedisParameters.getJedis(), parameters.fRedisParameters.fRedisQueue, parameters.fInterferencesFolder);
        } else if (parameters.fFileOfInstanceFiles != null && parameters.fInterferencesFolder != null) {
            reader = new FileProblemReader(parameters.fFileOfInstanceFiles, parameters.fInterferencesFolder);
        } else {
            throw new IllegalArgumentException("Illegal parameters provided. Must provide -DATA-FOLDERNAME and -DOMAINS. Please consult the SATFC manual for examples");
        }
        return reader;
    }
}
