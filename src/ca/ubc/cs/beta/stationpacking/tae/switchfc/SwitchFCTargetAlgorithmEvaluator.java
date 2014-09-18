package ca.ubc.cs.beta.stationpacking.tae.switchfc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.execution.EncodedInstanceToCNFConverter;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import ca.ubc.cs.beta.stationpacking.tae.SATFCTargetAlgorithmEvaluator;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;

public class SwitchFCTargetAlgorithmEvaluator extends AbstractSyncTargetAlgorithmEvaluator {

    private static final Logger log = LoggerFactory.getLogger(SwitchFCTargetAlgorithmEvaluator.class);

    private final SATFCTargetAlgorithmEvaluator fSatfcTae;
    private final CommandLineTargetAlgorithmEvaluator fCliTae;

    private final DataManager fDataManager;
    private final Map<ManagerBundle,ISATEncoder> fSATencoders;

    private final String fInterferencesConfigFolderDirname;
    private final String fTmpDirname;

    SwitchFCTargetAlgorithmEvaluator(
            SATFCTargetAlgorithmEvaluator aSatfcTae,
            CommandLineTargetAlgorithmEvaluator aCliTae,
            String aInterferencesConfigFolderDir,
            String aTmpDirname) {
        if (aSatfcTae == null) throw new IllegalArgumentException("SATFC TAE cannot be null.");
        if (aCliTae == null) throw new IllegalArgumentException("CLI TAE cannot be null.");
        if (aInterferencesConfigFolderDir == null) throw new IllegalArgumentException("Interferences folder cannot be null.");
        if (!new File(aInterferencesConfigFolderDir).isDirectory()) {
            throw new IllegalArgumentException("Interferences folder is not a directory.");
        }

        fSatfcTae = aSatfcTae;
        fCliTae = aCliTae;

        fDataManager = new DataManager();
        fSATencoders = new HashMap<ManagerBundle,ISATEncoder>();

        fInterferencesConfigFolderDirname = aInterferencesConfigFolderDir;

        File aTmpDir = new File(aTmpDirname);        

        if (!aTmpDir.exists()) {
            if (!aTmpDir.mkdirs()) {
                throw new IllegalStateException("Failed to create temp directory " + aTmpDir.getAbsolutePath());
            }
        } else if (!aTmpDir.isDirectory()){
            throw new IllegalArgumentException(aTmpDir.getAbsolutePath() + " is not a directory.");
        }

        fTmpDirname = aTmpDirname;

    }

    @Override
    public boolean isRunFinal() {
        return false;
    }

    @Override
    public boolean areRunsPersisted() {
        return false;
    }

    @Override
    public boolean areRunsObservable() {
        return true;
    }

    @Override
    protected void subtypeShutdown() {
        fSatfcTae.close();
        fCliTae.close();
    }

    @Override
    public List<AlgorithmRunResult> evaluateRun(
            final List<AlgorithmRunConfiguration> originalRunConfigurations, 
            final TargetAlgorithmEvaluatorRunObserver runStatusObserver) {

        List<AlgorithmRunConfiguration> satfcConfigurations = new ArrayList<AlgorithmRunConfiguration>();
        Map<AlgorithmRunConfiguration, AlgorithmRunConfiguration> transformedToOriginalCliConfigurationsMapping = new HashMap<AlgorithmRunConfiguration, AlgorithmRunConfiguration>();

        Map<AlgorithmRunConfiguration, ISATDecoder> transformedCliConfigurationsToCNFDecoderMapping = new HashMap<AlgorithmRunConfiguration, ISATDecoder>();

        Collection<String> tmpFiles = new ArrayList<String>();
        
        /*
         * Partition run configurations into either SATFC or CLI.
         * For the CLI run configurations, we must transform the encoded instance problem to a CNF file.
         */
        for (AlgorithmRunConfiguration runConfig : originalRunConfigurations) {

            if (runConfig.getAlgorithmExecutionConfiguration()
                    .getTargetAlgorithmExecutionContext()
                    .containsKey(SATFCTargetAlgorithmEvaluator.SATFC_CONTEXT_KEY)) 
            {                
                satfcConfigurations.add(runConfig);

            } else {

                String encodedInstanceString = runConfig.getProblemInstanceSeedPair().getProblemInstance().getInstanceSpecificInformation();

                // transform problem instance to CLI-friendly
                
                Pair<StationPackingInstance, String> decodedInstanceAndConfigFoldername;
                
                try {
                    decodedInstanceAndConfigFoldername = EncodedInstanceToCNFConverter.getInstanceFromSQLString(encodedInstanceString, fInterferencesConfigFolderDirname, fDataManager);
                } catch (IllegalArgumentException e) {
                    log.error("Failed to parse encoded instance string: " + encodedInstanceString, e);
                    throw new IllegalStateException("Instance specific information of run config " + runConfig + " parsing failed.", e);
                }

                String cnfFilename = fTmpDirname+ File.separator +decodedInstanceAndConfigFoldername.getFirst().getHashString()+".cnf";
                tmpFiles.add(cnfFilename);                

                ISATDecoder SATdecoder = encodeInstanceToCNFFile(
                        decodedInstanceAndConfigFoldername.getFirst(), cnfFilename, getSATEncoder(decodedInstanceAndConfigFoldername.getSecond()));

                ProblemInstanceSeedPair transformedProblemInstance = new ProblemInstanceSeedPair(
                        new ProblemInstance(cnfFilename), runConfig.getProblemInstanceSeedPair().getSeed());

                AlgorithmRunConfiguration transformedCliRunConfig = new AlgorithmRunConfiguration(
                        transformedProblemInstance, runConfig.getParameterConfiguration(), runConfig.getAlgorithmExecutionConfiguration());                

                transformedToOriginalCliConfigurationsMapping.put(transformedCliRunConfig, runConfig);
                transformedCliConfigurationsToCNFDecoderMapping.put(transformedCliRunConfig, SATdecoder);
            }
        }

        final Map<AlgorithmRunConfiguration, AlgorithmRunConfiguration> unmodifiableTransformedToOriginalCliConfigurationsMapping = Collections.unmodifiableMap(transformedToOriginalCliConfigurationsMapping);
        final Map<AlgorithmRunConfiguration, ISATDecoder> unmodifiableTransformedCliConfigurationsToCNFDecoderMapping = Collections.unmodifiableMap(transformedCliConfigurationsToCNFDecoderMapping);

        // Ensure is valid partition.
        if (!Sets.union(Sets.newHashSet(transformedToOriginalCliConfigurationsMapping.values()), Sets.newHashSet(satfcConfigurations)).equals(Sets.newHashSet(originalRunConfigurations))) {
            throw new IllegalStateException("Failed to partition configurations.");
        }

        /*
         * Create default results for all run configurations.
         */
        final ConcurrentMap<AlgorithmRunConfiguration, AlgorithmRunResult> resultsMapping = new ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
        for (final AlgorithmRunConfiguration runConfig : originalRunConfigurations) {
            KillHandler stubKillHander = new KillHandler() {

                AtomicBoolean b = new AtomicBoolean(false);
                @Override
                public void kill() {
                    b.set(true);

                    // Change run result to KILLED
                    log.debug("Killing job {}", runConfig);

                    resultsMapping.put(runConfig, new ExistingAlgorithmRunResult(runConfig, RunStatus.KILLED, 0.0, 0.0, 0.0, runConfig.getProblemInstanceSeedPair().getSeed()));

                }

                @Override
                public boolean isKilled() {
                    return b.get();
                }

            };

            AlgorithmRunResult stubRunResult = new RunningAlgorithmRunResult(runConfig, 0.0, 0.0, 0.0, runConfig.getProblemInstanceSeedPair().getSeed(), 0.0, stubKillHander);

            resultsMapping.put(runConfig, stubRunResult);
        }

        /*
         * Create observer that is passed to the child TAEs.
         */
        TargetAlgorithmEvaluatorRunObserver decoratedRunStatusObserver = new TargetAlgorithmEvaluatorRunObserver() {

            Lock fObserverLock = new ReentrantLock();

            @Override
            public void currentStatus(List<? extends AlgorithmRunResult> runs) {                    

                /*
                 * Parse results and ensure that these are the ones that the observer expects.
                 */

                Map<AlgorithmRunConfiguration, AlgorithmRunResult> newResults = parseRunResults(
                        unmodifiableTransformedToOriginalCliConfigurationsMapping, unmodifiableTransformedCliConfigurationsToCNFDecoderMapping, resultsMapping, runs);

                // Ensure that the following section of code is executable by only one thread at a time.

                fObserverLock.lock();

                log.trace("Updating run results.");

                try {

                    /*
                     * Update mapping with new results.
                     */

                    resultsMapping.putAll(newResults);

                    /*
                     * Build the list of results.
                     */

                    List<AlgorithmRunResult> resultsList = new ArrayList<AlgorithmRunResult>();

                    for (AlgorithmRunConfiguration runConfig : originalRunConfigurations) {

                        AlgorithmRunResult result = resultsMapping.get(runConfig);

                        if (result == null) {
                            throw new IllegalStateException("Result is null for run configuration " +runConfig);
                        }

                        resultsList.add(result);

                    }

                    /*
                     * Send to observer
                     */

                    runStatusObserver.currentStatus(resultsList);

                } finally {

                    fObserverLock.unlock();

                }
            }

        };

        /*
         * Run the configurations in the corresponding target algorithm evaluators. 
         */

        final AtomicBoolean satfcDone = new AtomicBoolean(false);
        final AtomicBoolean cliDone = new AtomicBoolean(false);

        TargetAlgorithmEvaluatorCallback satfcCallback = new TargetAlgorithmEvaluatorCallback() {

            @Override
            public void onSuccess(List<AlgorithmRunResult> runs) {

                // Ensure that all are finished.
                for (AlgorithmRunResult runResult : runs) {
                    if (runResult.getRunStatus().equals(RunStatus.RUNNING)) {
                        throw new IllegalStateException("SATFC TAE returned a run result that is RUNNING, " +runResult);
                    }
                }

                Map<AlgorithmRunConfiguration, AlgorithmRunResult> results = parseRunResults(unmodifiableTransformedToOriginalCliConfigurationsMapping, unmodifiableTransformedCliConfigurationsToCNFDecoderMapping, resultsMapping, runs);
                resultsMapping.putAll(results);

                log.trace("{} runs solved by SATFC.", runs.size());

                satfcDone.set(true);
            }

            @Override
            public void onFailure(RuntimeException e) {

                satfcDone.set(true);

                log.error("Failed in SATFC TAE.", e);

                throw e;
            }
        };

        TargetAlgorithmEvaluatorCallback cliCallback = new TargetAlgorithmEvaluatorCallback() {

            @Override
            public void onSuccess(List<AlgorithmRunResult> runs) {

                // Ensure that all are finished.
                for (AlgorithmRunResult runResult : runs) {
                    if (runResult.getRunStatus().equals(RunStatus.RUNNING)) {
                        throw new IllegalStateException("CLI TAE returned a run result that is RUNNING, " +runResult);
                    }
                }

                Map<AlgorithmRunConfiguration, AlgorithmRunResult> results = parseRunResults(unmodifiableTransformedToOriginalCliConfigurationsMapping, unmodifiableTransformedCliConfigurationsToCNFDecoderMapping, resultsMapping, runs);
                resultsMapping.putAll(results);

                log.trace("{} runs solved by CLI.", runs.size());

                cliDone.set(true);
            }

            @Override
            public void onFailure(RuntimeException e) {

                cliDone.set(true);

                log.error("Failed in CLI TAE.", e);

                throw e;
            }
        };

        // Run SATFC configs
        fSatfcTae.evaluateRunsAsync(satfcConfigurations, satfcCallback, decoratedRunStatusObserver);

        // Run CLI configs
        fCliTae.evaluateRunsAsync(Lists.newArrayList(transformedToOriginalCliConfigurationsMapping.keySet()), cliCallback, decoratedRunStatusObserver);

        // Wait until both done. 
        while (!satfcDone.get() || !cliDone.get()) {}

        /*
         * Return results.
         */     

        List<AlgorithmRunResult> results = new ArrayList<AlgorithmRunResult>(originalRunConfigurations.size());

        for (AlgorithmRunConfiguration runConfig : originalRunConfigurations) {

            results.add(resultsMapping.get(runConfig));
        }

        // Ensure that the results we return do not have run status RUNNING.   
        for (AlgorithmRunResult result : results) {
            if (result.getRunStatus().equals(RunStatus.RUNNING)) {
                throw new IllegalStateException("Attempted to return a result that is still RUNNING: " + result);
            }
        }
        
        // Delete all temporary files.
        for (String tmp : tmpFiles) {
            new File(tmp).delete();
        }

        return results;
    }

    private Map<AlgorithmRunConfiguration, AlgorithmRunResult> parseRunResults(
            final Map<AlgorithmRunConfiguration, AlgorithmRunConfiguration> unmodifiableTransformedToOriginalCliConfigurationsMapping,
            final Map<AlgorithmRunConfiguration, ISATDecoder> unmodifiableTransformedCliConfigurationsToCNFDecoderMapping, 
            final ConcurrentMap<AlgorithmRunConfiguration, AlgorithmRunResult> resultsMapping,
            List<? extends AlgorithmRunResult> runs) {

        Map<AlgorithmRunConfiguration, AlgorithmRunResult> parsedResults =  new HashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();

        for (final AlgorithmRunResult runResult : runs) {

            // Check if run is one of the transformed configurations, then we must revert it.

            AlgorithmRunResult actualResult = runResult;

            if (unmodifiableTransformedToOriginalCliConfigurationsMapping.containsKey(runResult.getAlgorithmRunConfiguration())) {

                if (runResult.getRunStatus().equals(RunStatus.RUNNING)) {
                    KillHandler kh = new KillHandler() {

                        AtomicBoolean b = new AtomicBoolean(false);
                        @Override
                        public void kill() {
                            b.set(true);
                            runResult.kill();
                        }

                        @Override
                        public boolean isKilled() {
                            return b.get();
                        }

                    };

                    if(unmodifiableTransformedToOriginalCliConfigurationsMapping.get(runResult.getAlgorithmRunConfiguration()) == null)
                    {
                        log.error("Couldn't find original run config for {} in {} ", runResult.getAlgorithmRunConfiguration(), unmodifiableTransformedToOriginalCliConfigurationsMapping);
                    }

                    actualResult = new RunningAlgorithmRunResult(unmodifiableTransformedToOriginalCliConfigurationsMapping.get(runResult.getAlgorithmRunConfiguration()),runResult.getRuntime(),runResult.getRunLength(), runResult.getQuality(), runResult.getResultSeed(), runResult.getWallclockExecutionTime(),kh);

                } else {

                    // In addition to switching the run config, we must fix the additional run data
                    // to provide a valid station-channel assignment if the result is SAT.

                    String additionalRunData = runResult.getAdditionalRunData();

                    if (runResult.getRunStatus().equals(RunStatus.SAT)) {
                        ISATDecoder decoder = unmodifiableTransformedCliConfigurationsToCNFDecoderMapping.get(runResult.getAlgorithmRunConfiguration());
                        
                        // Format is <StationID>=<ChannelAssignment>
                        Collection<String> assignments = new ArrayList<String>();
                        
                        for (String variable : additionalRunData.split(" ")) {
                            long var = Long.parseLong(variable);
                            
                            if (var > 0) {
                                Pair<Station, Integer> stationChannelVariable = decoder.decode(var);
                                
                                assignments.add(stationChannelVariable.getKey().getID() + "=" + stationChannelVariable.getValue());
                            }
                        }
                        
                        additionalRunData = StringUtils.join(assignments, ";");
                        
                    }

                    actualResult = new ExistingAlgorithmRunResult(unmodifiableTransformedToOriginalCliConfigurationsMapping.get(runResult.getAlgorithmRunConfiguration()), runResult.getRunStatus(), runResult.getRuntime(), runResult.getRunLength(), runResult.getQuality(), runResult.getResultSeed(), additionalRunData, runResult.getWallclockExecutionTime());



                }
            }

            // Check that the new run result is one that we're expecting.

            if (!resultsMapping.containsKey(actualResult.getAlgorithmRunConfiguration())) {
                throw new IllegalStateException("Observed a run result that does not correspond to any of the run configurations.");
            }

            parsedResults.put(actualResult.getAlgorithmRunConfiguration(), actualResult);
        }

        return parsedResults;
    }
    
    private ISATEncoder getSATEncoder(String configFoldername) {
        ManagerBundle data_bundle;
        try {
            data_bundle = fDataManager.getData(configFoldername);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not load interference data from \""+configFoldername+"\".");
        }
        
        IConstraintManager constraint_manager = data_bundle.getConstraintManager();
        
        ISATEncoder SATencoder;
        if(fSATencoders.containsKey(data_bundle))
        {
            SATencoder = fSATencoders.get(data_bundle);
        }
        else
        {
            SATencoder = new SATCompressor(constraint_manager);
            fSATencoders.put(data_bundle, SATencoder);
        }
        
        return SATencoder;
    }
    
    private ISATDecoder encodeInstanceToCNFFile(StationPackingInstance aInstance, String aOutputFilename, ISATEncoder aSATencoder) {
        Pair<CNF, ISATDecoder> encoding = aSATencoder.encode(aInstance);
        CNF cnf = encoding.getKey();
        
        try {
            FileUtils.writeStringToFile(new File(aOutputFilename), cnf.toDIMACS(new String[0]));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not write CNF to file.");
        }
        
        return encoding.getSecond();
    }

}
