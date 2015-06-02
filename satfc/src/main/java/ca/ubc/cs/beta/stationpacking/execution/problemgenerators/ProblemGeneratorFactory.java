package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;

/**
 * Created by newmanne on 12/05/15.
 */
public class ProblemGeneratorFactory {

    public static IProblemReader createFromParameters(SATFCFacadeParameters parameters) {
        IProblemReader generator;
        if (parameters.fInstanceParameters.fDataFoldername != null && parameters.fInstanceParameters.getDomains() != null) {
            generator = new SingleProblemFromCommandLineProblemReader(new SATFCFacadeProblem(
                    parameters.fInstanceParameters.getPackingStationIDs(),
                    parameters.fInstanceParameters.getPackingChannels(),
                    parameters.fInstanceParameters.getDomains(),
                    parameters.fInstanceParameters.getPreviousAssignment(),
                    parameters.fInstanceParameters.fDataFoldername,
                    null
            ));
        } else if (parameters.fRedisParameters.areValid() && parameters.fInterferencesFolder != null) {
            generator = new RedisProblemReader(parameters.fRedisParameters.getJedis(), parameters.fRedisParameters.fRedisQueue, parameters.fInterferencesFolder);
        } else if (parameters.fFileOfInstanceFiles != null && parameters.fInterferencesFolder != null && parameters.fMetricsFile != null) {
            generator = new FileProblemReader(parameters.fFileOfInstanceFiles, parameters.fInterferencesFolder, parameters.fMetricsFile);
        } else {
            throw new IllegalArgumentException("Illegal parameters provided. Must provide -DATA-FOLDERNAME and -DOMAINS. Please consult the SATFC manual for examples");
        }
        return generator;
    }
}
