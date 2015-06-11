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
package ca.ubc.cs.beta.stationpacking.execution;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.execution.metricwriters.IMetricWriter;
import ca.ubc.cs.beta.stationpacking.execution.metricwriters.MetricWriterFactory;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.IProblemReader;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.ProblemGeneratorFactory;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.SATFCFacadeProblem;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;

import com.beust.jcommander.ParameterException;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * Executes a SATFC facade built from parameters on an instance given in parameters.
 *
 * @author afrechet
 */
public class SATFCFacadeExecutor {

    /**
     * @param args - parameters satisfying {@link SATFCFacadeParameters}.
     */
    public static void main(String[] args) {
        //Parse the command line arguments in a parameter object.
        SATFCFacadeParameters parameters = new SATFCFacadeParameters();
        Logger log = parseParameter(args, parameters);
        logVersionInfo(log);
        try {
            log.info("Initializing facade.");
            SATFCFacadeBuilder satfcBuilder = new SATFCFacadeBuilder();
            try(final SATFCFacade satfc = satfcBuilder.buildFromParameters(parameters)) {
                IProblemReader problemGenerator = ProblemGeneratorFactory.createFromParameters(parameters);
                IMetricWriter metricWriter = MetricWriterFactory.createFromParameters(parameters);
                SATFCFacadeProblem problem;
                while ((problem = problemGenerator.getNextProblem()) != null) {
                    SATFCMetrics.postEvent(new SATFCMetrics.NewStationPackingInstanceEvent(problem.getStationsToPack(), problem.getInstanceName()));
                    log.info("Beginning problem {}", problem.getInstanceName());
                    log.info("Solving ...");
                    SATFCResult result = satfc.solve(
                            problem.getStationsToPack(),
                            problem.getChannelsToPackOn(),
                            problem.getDomains(),
                            problem.getPreviousAssignment(),
                            parameters.fInstanceParameters.Cutoff,
                            parameters.fInstanceParameters.Seed,
                            problem.getStationConfigFolder(),
                            problem.getInstanceName()
                    );
                    log.info("..done!");
                    System.out.println(result.getResult());
                    System.out.println(result.getRuntime());
                    System.out.println(result.getWitnessAssignment());
                    SATFCMetrics.postEvent(new SATFCMetrics.InstanceSolvedEvent(problem.getInstanceName(), result.getResult(), result.getRuntime()));
                    problemGenerator.onPostProblem(problem, result);
                    metricWriter.writeMetrics();
                    SATFCMetrics.clear();
                }
                log.info("Finished all of the problems!");
                problemGenerator.onFinishedAllProblems();
                metricWriter.onFinished();
            }
        } catch (ParameterException e) {
            log.error("Invalid parameter argument detected ({}).", e.getMessage());
            e.printStackTrace();
            System.exit(AEATKReturnValues.PARAMETER_EXCEPTION);
        } catch (RuntimeException e) {
            log.error("Runtime exception encountered ({})", e.getMessage());
            e.printStackTrace();
            System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
        } catch (UnsatisfiedLinkError e) {
            log.error("Couldn't initialize facade, see previous log messages and/or try logging with DEBUG.", e);
            System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
        } catch (Throwable t) {
            log.error("Throwable encountered ({})", t.getMessage());
            t.printStackTrace();
            System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
        }
        log.info("Normal termination. Goodbye");
    }

    private static Logger parseParameter(String[] args, SATFCFacadeParameters parameters) {
        Logger log;
        try {
            //Check for help
            JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters, TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
            SATFCFacade.initializeLogging(parameters.fLoggingOptions.logLevel);
            JCommanderHelper.logCallString(args, SATFCFacadeExecutor.class);
        } finally {
            log = LoggerFactory.getLogger(SATFCFacadeExecutor.class);
        }
        return log;
    }

    private static void logVersionInfo(Logger log) {
        try {
            log.info("Version info: " + System.lineSeparator() + Resources.toString(Resources.getResource("version.properties"), Charsets.UTF_8));
        } catch (IllegalArgumentException | IOException e) {
            log.error("Could not log version info.");
        }
    }

}