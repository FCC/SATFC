package ca.ubc.cs.beta.stationpacking.tae.switchfc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
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
        
        if (!aTmpDir.exists() && !aTmpDir.mkdirs()) {
            throw new IllegalStateException("Failed to create temp folder " + aTmpDir.getAbsolutePath());
        } else {
            fTmpDirname = aTmpDirname;
        }
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
            List<AlgorithmRunConfiguration> runConfigs, 
            final TargetAlgorithmEvaluatorRunObserver runStatusObserver) {

        List<AlgorithmRunConfiguration> satfcConfigurations = new ArrayList<AlgorithmRunConfiguration>();
        final Map<AlgorithmRunConfiguration, AlgorithmRunConfiguration> transformedToOriginalCliConfigurationsMapping = new ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunConfiguration>();

        for (AlgorithmRunConfiguration runConfig : runConfigs) {

            if (runConfig.getAlgorithmExecutionConfiguration()
                    .getTargetAlgorithmExecutionContext()
                    .containsKey(SATFCTargetAlgorithmEvaluator.SATFC_CONTEXT_KEY)) 
            {                
                satfcConfigurations.add(runConfig);

            } else {

                String encodedInstanceString = runConfig.getProblemInstanceSeedPair().getProblemInstance().getInstanceSpecificInformation();

                // transform problem instance to CLI-friendly

                String cnfFilename = writeCNFtoFile(encodedInstanceString, fTmpDirname);

                ProblemInstanceSeedPair transformedProblemInstance = new ProblemInstanceSeedPair(
                        new ProblemInstance(cnfFilename), runConfig.getProblemInstanceSeedPair().getSeed());

                AlgorithmRunConfiguration transformedCliRunConfig = new AlgorithmRunConfiguration(
                        transformedProblemInstance, runConfig.getParameterConfiguration(), runConfig.getAlgorithmExecutionConfiguration());                

                transformedToOriginalCliConfigurationsMapping.put(transformedCliRunConfig, runConfig);
            }
        }

        List<AlgorithmRunConfiguration> transformedCliConfigurations = Lists.newArrayList(transformedToOriginalCliConfigurationsMapping.keySet());

        /*
         * Create observer that is to be passed to the CLI TAE.
         */
        TargetAlgorithmEvaluatorRunObserver cliRunStatusObserver = new TargetAlgorithmEvaluatorRunObserver() {

            @Override
            public void currentStatus(List<? extends AlgorithmRunResult> runs) {
                
                List<AlgorithmRunResult> fixedRuns = new ArrayList<AlgorithmRunResult>(runs.size());
                
                for( final AlgorithmRunResult run : runs)
                {
                    if(run.getRunStatus().equals(RunStatus.RUNNING))
                    {
                        
                        KillHandler kh = new KillHandler()
                        {

                            AtomicBoolean b = new AtomicBoolean(false);
                            @Override
                            public void kill() {
                                b.set(true);
                                run.kill();
                            }

                            @Override
                            public boolean isKilled() {
                                return b.get();
                            }
                            
                        };
                        
                        if(transformedToOriginalCliConfigurationsMapping.get(run.getAlgorithmRunConfiguration()) == null)
                        {
                            log.error("Couldn't find original run config for {} in {} ", run.getAlgorithmRunConfiguration(), transformedToOriginalCliConfigurationsMapping);
                        }
                        fixedRuns.add(new RunningAlgorithmRunResult(transformedToOriginalCliConfigurationsMapping.get(run.getAlgorithmRunConfiguration()),run.getRuntime(),run.getRunLength(), run.getQuality(), run.getResultSeed(), run.getWallclockExecutionTime(),kh));
                        
                    } else
                    {
                        fixedRuns.add(new ExistingAlgorithmRunResult(transformedToOriginalCliConfigurationsMapping.get(run.getAlgorithmRunConfiguration()), run.getRunStatus(), run.getRuntime(), run.getRunLength(), run.getQuality(),run.getResultSeed(),run.getAdditionalRunData(), run.getWallclockExecutionTime()));
                    }
                }
                
                if(runStatusObserver != null)
                {
                    runStatusObserver.currentStatus(fixedRuns);
                }
            }
        };

        /*
         * Run the configurations in the corresponding target algorithm evaluators. 
         */
        
        // Run SATFC configs
        List<AlgorithmRunResult> satfcResults = fSatfcTae.evaluateRun(satfcConfigurations, runStatusObserver);

        // Run CLI configs
        List<AlgorithmRunResult> cliResults = fSatfcTae.evaluateRun(transformedCliConfigurations, cliRunStatusObserver);

        List<AlgorithmRunResult> results = new ArrayList<AlgorithmRunResult>(runConfigs.size());

        for (AlgorithmRunConfiguration aConfig : runConfigs) {
            if (satfcConfigurations.contains(aConfig)) {

                int index = satfcConfigurations.indexOf(aConfig);
                results.add(satfcResults.get(index));

            } else if (transformedCliConfigurations.contains(aConfig)) {

                int index = transformedCliConfigurations.indexOf(aConfig);
                results.add(cliResults.get(index));

            } else {
                throw new IllegalStateException("Run config " +aConfig+ " was not partitioned into either a SATFC or CLI run.");
            }
        }

        return results;
    }


    /**
     * Converts an encoded instance string to a CNF, then writes to a file in the given output folder.
     * @return Location of stored .cnf file
     */
    private String writeCNFtoFile(String encodedInstanceString, String outputFoldername) {
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

            Set<Integer> truedomain = station_manager.getDomain(station);
            if(!truedomain.containsAll(domain))
            {
                //log.warn("Domain {} of station {} does not contain all stations specified in problem domain {}.",truedomain,stationID,domain);
            }

            domains.put(station, domain);
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

        log.debug("Encoding into SAT...");
        Pair<CNF,ISATDecoder> encoding = SATencoder.encode(instance);
        CNF cnf = encoding.getKey();


        String aCNFFilename = outputFoldername+ File.separator +instance.getHashString()+".cnf";
        log.debug("Saving CNF to {}...",aCNFFilename);


        List<Integer> sortedStationIDs = new ArrayList<Integer>(stationID_domains.keySet());
        Collections.sort(sortedStationIDs);
        List<Integer> sortedAllChannels = new ArrayList<Integer>(instance.getAllChannels());
        Collections.sort(sortedAllChannels);

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
