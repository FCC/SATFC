package ca.ubc.cs.beta.stationpacking.tae.switchfc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
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

    private final String fInterferencesConfigFolderDir;
    private final String fTmpDirname;

    SwitchFCTargetAlgorithmEvaluator(SATFCTargetAlgorithmEvaluator aSatfcTae, CommandLineTargetAlgorithmEvaluator aCliTae, String aInterferencesConfigFolderDir, String aTmpDirname) {
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

        fInterferencesConfigFolderDir = aInterferencesConfigFolderDir;

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

                Pair<CNF, ISATDecoder> cnf = createCNFfromInstanceString(encodedInstanceString);
                String cnfFilename = printCNFtoFile(fTmpDirname, cnf);

                ProblemInstanceSeedPair transformedProblemInstance = new ProblemInstanceSeedPair(
                        new ProblemInstance(cnfFilename), runConfig.getProblemInstanceSeedPair().getSeed());

                AlgorithmRunConfiguration transformedCliRunConfig = new AlgorithmRunConfiguration(
                        transformedProblemInstance, runConfig.getParameterConfiguration(), runConfig.getAlgorithmExecutionConfiguration());                

                transformedToOriginalCliConfigurationsMapping.put(transformedCliRunConfig, runConfig);
                transformedCliConfigurationsToCNFDecoderMapping.put(transformedCliRunConfig, cnf.getValue());
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


    /**
     * Converts an encoded instance string to a CNF, then writes to a file in the given output folder.
     * @return Location of stored .cnf file
     */
    private Pair<CNF, ISATDecoder> createCNFfromInstanceString(String encodedInstanceString) {
        /*
         * Get data from instance.
         */
        Map<Integer,Set<Integer>> stationID_domains = new HashMap<Integer,Set<Integer>>();
        Map<Integer,Integer> previous_assignmentID = new HashMap<Integer,Integer>();
        String configFoldername;

        String[] encoded_instance_parts = encodedInstanceString.split("_");

        if(encoded_instance_parts.length == 0)
        {
            throw new IllegalArgumentException("Unparseable encoded instance string \""+encodedInstanceString+"\".");
        }

        //Get the config folder name.
        configFoldername = fInterferencesConfigFolderDir + File.separator + encoded_instance_parts[0];

        //Get problem info.
        for(int i=1;i<encoded_instance_parts.length;i++)
        {
            Integer station;
            Integer previousChannel;
            Set<Integer> domain;

            String station_info_string = encoded_instance_parts[i];
            String[] station_info_parts = station_info_string.split(";");

            String station_string = station_info_parts[0];
            if(isInteger(station_string))
            {
                station = Integer.parseInt(station_string);
            }
            else
            {
                throw new IllegalArgumentException("Unparseable station info \""+station_info_string+"\" (station ID "+station_string+" is not an integer).");
            }

            String previous_channel_string = station_info_parts[1];
            if(isInteger(previous_channel_string))
            {
                previousChannel = Integer.parseInt(previous_channel_string);
                if(previousChannel <= 0)
                {
                    previousChannel = null;
                }
            }
            else
            {
                throw new IllegalArgumentException("Unparseable station info \""+station_info_string+"\" (previous channel "+previous_channel_string+" is not an integer).");
            }


            domain = new HashSet<Integer>();
            String channels_string = station_info_parts[2];
            String[] channels_parts = channels_string.split(",");
            for(String channel_string : channels_parts)
            {
                if(isInteger(channel_string))
                {
                    domain.add(Integer.parseInt(channel_string));
                }
                else
                {
                    throw new IllegalArgumentException("Unparseable station info \""+station_info_string+"\" (domain channel "+channel_string+" is not an integer).");
                }
            }


            stationID_domains.put(station, domain);
            if(previousChannel != null)
            {
                previous_assignmentID.put(station, previousChannel);
            }
        }

        /*
         * Validate instance data.
         */
        File config_folder = new File(configFoldername);
        if(!config_folder.exists())
        {
            throw new IllegalArgumentException("Encoded instance's interference config folder \""+configFoldername+"\" does not exist.");
        }
        else if(!config_folder.isDirectory())
        {
            throw new IllegalArgumentException("Encoded instance's interference config folder \""+configFoldername+"\" is not a directory.");
        }

        if(!stationID_domains.keySet().containsAll(previous_assignmentID.keySet()))
        {
            log.warn("Encoded instance's previous assignment contains stations not in the indicated domains.");
        }

        /*
         * Construct station packing instances. 
         */

        ManagerBundle data_bundle;
        try {
            data_bundle = fDataManager.getData(configFoldername);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not load interference data from \""+configFoldername+"\".");
        }
        IStationManager station_manager = data_bundle.getStationManager();
        IConstraintManager constraint_manager = data_bundle.getConstraintManager();

        Map<Station,Set<Integer>> domains = new HashMap<Station,Set<Integer>>();
        for(Entry<Integer,Set<Integer>> stationID_domains_entry : stationID_domains.entrySet())
        {
            Integer stationID = stationID_domains_entry.getKey();
            Set<Integer> domain = stationID_domains_entry.getValue();

            Station station = station_manager.getStationfromID(stationID);

            Set<Integer> validDomain = station_manager.getDomain(station);
            if(!validDomain.containsAll(domain))
            {
                //log.warn("Domain {} of station {} does not contain all stations specified in problem domain {}.",truedomain,stationID,domain);
            }

            domains.put(station, Sets.intersection(domain, validDomain));
        }

        Map<Station,Integer> previous_assignment = new HashMap<Station,Integer>();
        for(Entry<Integer,Integer> previous_assignmentID_entry : previous_assignmentID.entrySet())
        {
            Integer stationID = previous_assignmentID_entry.getKey();
            Integer previous_channel = previous_assignmentID_entry.getValue();

            Station station = station_manager.getStationfromID(stationID);

            Set<Integer> truedomain = station_manager.getDomain(station);
            if(!truedomain.contains(previous_channel))
            {
                log.warn("Domain {} of station {} does not contain previous assigned channel {}.",truedomain,stationID,previous_channel);
            }

            previous_assignment.put(station, previous_channel);

        }

        StationPackingInstance instance = new StationPackingInstance(domains,previous_assignment);


        /*
         * Encode instance into CNF.
         */
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

        return SATencoder.encode(instance);
    }

    private String printCNFtoFile(String outputFoldername, Pair<CNF, ISATDecoder> encoding) {
        CNF cnf = encoding.getKey();

        String aCNFFilename = outputFoldername+ File.separator +cnf.getHashString()+".cnf";

        try {
            FileUtils.writeStringToFile(new File(aCNFFilename), cnf.toDIMACS(new String[0]));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not write CNF to file.");
        }

        return aCNFFilename;
    }

    private static boolean isInteger(String s) {
        try { 
            Integer.parseInt(s); 
        } catch(NumberFormatException e) { 
            return false; 
        }
        return true;
    }

}
