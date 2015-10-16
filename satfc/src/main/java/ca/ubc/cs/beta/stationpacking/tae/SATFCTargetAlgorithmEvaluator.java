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
package ca.ubc.cs.beta.stationpacking.tae;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.Data;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.StatusVariableKillHandler;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.stationpacking.execution.parameters.smac.SATFCHydraParams;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.SATFCFacadeProblem;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.SingleSrpkProblemReader;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder.DeveloperOptions;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverChoice;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;

import com.google.common.base.Preconditions;

/**
 * Target algorithm evaluator that wraps around the SATFC facade and only
 * solves, synchronously, station packing feasibility problems.
 *
 * @author afrechet, seramage
 */
public class SATFCTargetAlgorithmEvaluator extends AbstractSyncTargetAlgorithmEvaluator {

    private static final Logger log = LoggerFactory.getLogger(SATFCTargetAlgorithmEvaluator.class);

    //Context key for a SATFC specific TAE run config.
    public final static String SATFC_CONTEXT_KEY = "SATFC_CONTEXT";

    private final String fStationConfigFolder;
    private final String fLibPath;

    private final ScheduledExecutorService fObserverThreadPool;
    private final DataManager dataManager;
    private final PythonInterpreter python;

    public SATFCTargetAlgorithmEvaluator(SATFCTargetAlgorithmEvaluatorOptions options) {

	    /*
         * Since asynchronous evaluations are guaranteed by the {@link AbstractSyncTargetAlgorithmEvaluator}, but the SATFCTAE is inherently synchronous,
	     * we constrained the AbstractSyncTargetAlgorithmEvaluator supertype to only use 2 threads.
	     */
        super(2);
        Preconditions.checkNotNull(options.fStationConfigFolder);
        Preconditions.checkArgument(new File(options.fStationConfigFolder).exists(), "Provided station config folder " + options.fStationConfigFolder + " does not exist.");
        fStationConfigFolder = options.fStationConfigFolder;
        Preconditions.checkNotNull(options.fLibrary);
        fLibPath = options.fLibrary;
        fObserverThreadPool = Executors.newScheduledThreadPool(2, new SequentiallyNamedThreadFactory("SATFC Observer Thread", true));
        // Create the DataManager (will be reused so we don't keep loading in constraints)
        dataManager = new DataManager();

        // Clasp parameters are super annoying to convert between the parameters that SMAC returns and an actual command line string, so we use the provided python library (invoked via jython) to figrue things out)
        python = new PythonInterpreter();
        python.execfile(getClass().getClassLoader().getResourceAsStream("hydra/claspParamPcsParser.py"));
    }

    @Override
    public boolean areRunsObservable() {
        return true;
    }

    @Override
    public boolean areRunsPersisted() {
        return false;
    }

    @Override
    public boolean isRunFinal() {
        return false;
    }

    @Override
    protected void subtypeShutdown() {
    }

    @Override
    public synchronized List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> aRuns, final TargetAlgorithmEvaluatorRunObserver aObserver) {

        final List<AlgorithmRunResult> results = new ArrayList<>(aRuns.size());

        // Initialize observation structures.
        final Map<AlgorithmRunConfiguration, AlgorithmRunResult> resultMap = Collections.synchronizedMap(new LinkedHashMap<>());
        final Map<AlgorithmRunConfiguration, StopWatch> watchMap = Collections.synchronizedMap(new LinkedHashMap<>());
        final Map<AlgorithmRunConfiguration, StatusVariableKillHandler> runconfigToKillMap = new ConcurrentHashMap<>();

        for (AlgorithmRunConfiguration config : aRuns) {
            final StatusVariableKillHandler killHandler = new StatusVariableKillHandler();
            resultMap.put(config, new RunningAlgorithmRunResult(config, 0.0, 0.0, 0.0, config.getProblemInstanceSeedPair().getSeed(), 0.0, killHandler));
            runconfigToKillMap.put(config, killHandler);
            watchMap.put(config, new StopWatch());
        }

        // Observer thread: Provides observation of the run along with estimated wallclock time.
        final Runnable observerThread = new SATFCTAEObserverThread(aObserver, resultMap, watchMap, runconfigToKillMap);
        // Run once for setup
        observerThread.run();
        // Start observer thread.
        ScheduledFuture<?> observerFuture = fObserverThreadPool.scheduleAtFixedRate(observerThread, 0, 5, TimeUnit.SECONDS);


        //Process the runs.
        for (AlgorithmRunConfiguration config : aRuns) {

            final ExistingAlgorithmRunResult runResult;

            if (!runconfigToKillMap.get(config).isKilled()) {
                StopWatch configWatch = watchMap.get(config);
                synchronized (configWatch) {
                    configWatch.start();
                }

                log.debug("Solving instance corresponding to algo run config \"{}\"", config);

                log.debug("Transforming config into SATFC problem...");

                //Read the feasibility checking instance from the additional run info.
                final ProblemInstance instance = config.getProblemInstanceSeedPair().getProblemInstance();
                final String srpkFile = instance.getInstanceName();
                final SATFCFacadeProblem problem = new SingleSrpkProblemReader(srpkFile, fStationConfigFolder).getNextProblem();

                log.debug("Parameters are :" + System.lineSeparator() + config.getParameterConfiguration().getFormattedParameterString(ParameterConfiguration.ParameterStringFormat.NODB_SYNTAX));

                //Read the straightforward parameters from the problem instance.
                final double cutoff = config.getCutoffTime();
                final long seed = config.getProblemInstanceSeedPair().getSeed();

                final Set<String> activeParameters = config.getParameterConfiguration().getActiveParameters();
                List<String> commandLine = new ArrayList<>();
                final Iterator<String> iterator = activeParameters.iterator();
                final StringBuilder clasp = new StringBuilder();
                while (iterator.hasNext()) {
                    final String next = iterator.next();
                    if (next.startsWith("_AT_")) { // CLASP PARAMETER
                        clasp.append("-").append(next).append(" ").append(config.getParameterConfiguration().get(next)).append(" ");
                    } else {
                        commandLine.add("-" + next);
                        commandLine.add(config.getParameterConfiguration().get(next));
                    }
                }
                String claspParams;
                try {
                    claspParams = parseClasp(clasp.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't parse out claps params! " + clasp.toString(), e);
                }
                final SATFCHydraParams params = new SATFCHydraParams();
                JCommanderHelper.parseCheckingForHelpAndVersion(commandLine.toArray(new String[commandLine.size()]), params);
                params.claspConfig = claspParams;
                params.validate();
                SATFCFacadeBuilder satfcFacadeBuilder = new SATFCFacadeBuilder()
                        .setLibrary(fLibPath)
                        .setSolverChoice(SolverChoice.HYDRA)
                        .setParallelismLevel(1)
                        .setDeveloperOptions(DeveloperOptions
                                .builder()
                                .dataManager(dataManager)
                                .hydraParams(params)
                                .build());
                try (final SATFCFacade facade = satfcFacadeBuilder.build()) {
                    log.debug("Giving problem to SATFC facade...");
                    // Solve the problem.
                    final SATFCResult result = facade.solve(
                            problem.getDomains(),
                            problem.getPreviousAssignment(),
                            cutoff,
                            seed,
                            problem.getStationConfigFolder(),
                            srpkFile
                    );
                    log.debug("Transforming SATFC facade result to TAE result...");
                    runResult = getAlgorithmRunResult(result, config, configWatch, seed);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                log.debug("Run has been preemptively killed in SATFC TAE");
                runResult = new ExistingAlgorithmRunResult(
                        config,
                        RunStatus.KILLED,
                        0,
                        0,
                        0,
                        config.getProblemInstanceSeedPair().getSeed(),
                        "Killed Preemptively before starting in SATFC TAE by Steve Ramage, Handsome Developer",
                        0);
            }

            resultMap.put(config, runResult);

            results.add(runResult);
        }

        observerFuture.cancel(false);

        return results;
    }

    public String parseClasp(final String claspParams) {
        final String evalString = "get_commmand(\"" + claspParams + "\")";
        log.debug("Eval command: " + evalString);
        final PyObject eval = python.eval(evalString);
        final String claspConfig = eval.toString();
        log.debug("Eval output: " + claspConfig);
        return claspConfig;
    }

    /**
     * Creates the algorithm run result corresponding to the given SATFC result and scenario, and stop the given watch as well.
     *
     * @param result      - the SATFC result.
     * @param config      - the config corresponding to the run executed.
     * @param configWatch - the running watch used for the run.
     * @param seed        - the seed used for the run.
     * @return the algorithm run result corresponding to the given results and scenario.
     */
    private static ExistingAlgorithmRunResult getAlgorithmRunResult(SATFCResult result, AlgorithmRunConfiguration config, StopWatch configWatch, long seed) {
        // Transform result to algorithm run result.
        final RunStatus status;
        final String additionalRunData;
        switch (result.getResult()) {
            case SAT:
                status = RunStatus.SAT;
                //Send back the witness assignment.
                Map<Integer, Integer> witness = result.getWitnessAssignment();
                StringBuilder sb = new StringBuilder();
                Iterator<Entry<Integer, Integer>> entryIterator = witness.entrySet().iterator();
                while (entryIterator.hasNext()) {

                    Entry<Integer, Integer> entry = entryIterator.next();

                    int stationID = entry.getKey();
                    int channel = entry.getValue();

                    sb.append(stationID).append("=").append(channel);

                    if (entryIterator.hasNext()) {
                        sb.append(",");
                    }
                }
                additionalRunData = sb.toString();
                break;
            case UNSAT:
                status = RunStatus.UNSAT;
                additionalRunData = "";
                break;
            case TIMEOUT:
                status = RunStatus.TIMEOUT;
                additionalRunData = "";
                break;
            default:
                status = RunStatus.CRASHED;
                additionalRunData = "";
                break;
        }

        synchronized (configWatch) {
            configWatch.stop();
            return new ExistingAlgorithmRunResult(
                    config,
                    status,
                    result.getRuntime(),
                    result.getRuntime(),
                    result.getRuntime(),
                    seed,
                    additionalRunData,
                    configWatch.time() / 1000.0);
        }
    }


    /**
     * Observer runnable that provides ongoing runs with estimate wall clock time to use the real observer on.
     *
     * @author afrechet
     */
    @Data
    private static class SATFCTAEObserverThread implements Runnable {
        private final TargetAlgorithmEvaluatorRunObserver fObserver;

        private final Map<AlgorithmRunConfiguration, AlgorithmRunResult> fResultMap;
        private final Map<AlgorithmRunConfiguration, StopWatch> fWatchMap;
        private final Map<AlgorithmRunConfiguration, StatusVariableKillHandler> fRunconfigToKillMap;

        @Override
        public synchronized void run() {

            //Update current status for wallclock time.
            List<AlgorithmRunResult> currentResults = new ArrayList<AlgorithmRunResult>(fResultMap.size());

            for (AlgorithmRunConfiguration config : fResultMap.keySet()) {
                final AlgorithmRunResult result = fResultMap.get(config);
                if (result.getRunStatus().equals(RunStatus.RUNNING)) {
                    final StopWatch configWatch = fWatchMap.get(config);

                    synchronized (configWatch) {
                        currentResults.add(new RunningAlgorithmRunResult(
                                config,
                                0.0,
                                0.0,
                                0.0,
                                config.getProblemInstanceSeedPair().getSeed(),
                                configWatch.time() / 1000.0,
                                fRunconfigToKillMap.get(config)));
                    }
                } else {
                    currentResults.add(result);
                }
            }

            //Trigger observer.
            fObserver.currentStatus(currentResults);
        }
    }

}
