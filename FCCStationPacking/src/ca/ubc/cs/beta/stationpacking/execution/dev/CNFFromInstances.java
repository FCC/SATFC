package ca.ubc.cs.beta.stationpacking.execution.dev;

import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.PopulatedDomainStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.InstanceGenerationExecutor;
import ca.ubc.cs.beta.stationpacking.execution.parameters.experiment.InstanceGenerationParameters;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.HybridCNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.IComponentGrouper;

/**
 * Hack class to get CNFs corresponding to instances listed in a specific format in a file.
 * @author afrechet
 *
 */
public class CNFFromInstances {

	private static Logger log = LoggerFactory.getLogger(InstanceGenerationExecutor.class);
	
	public static void main(String[] args) throws Exception {
	
		/**
		 * Test arguments to use, instead of compiling and using command line.
		 * 
		 * 
		**/

		String[] aPaxosTargetArgs = {"-STATIONS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/stations2.csv",
				"-DOMAINS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Domain-041813.csv",
				"-CONSTRAINTS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Interferences-041813.csv",
				"-CNF_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/CNFs",
				"-SOLVER",
				"tunedclasp",
				"-EXPERIMENT_NAME",
				"TestExperiment",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/TestExperiment",
				"--execDir",
				"SATsolvers",
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800"
				};
		
		args = aPaxosTargetArgs;
		
		//TAE Options
		Map<String,AbstractOptions> aAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		
		//Parse the command line arguments in a parameter object.
		InstanceGenerationParameters aExecParameters = new InstanceGenerationParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aExecParameters, aAvailableTAEOptions);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aExecParameters, aAvailableTAEOptions);
			
			boolean showHiddenParameters = false;
			
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			
			log.error(aParameterException.getMessage());
			return;
		}
		
		log.info("Getting data...");
		PopulatedDomainStationManager aStationManager = new PopulatedDomainStationManager(aExecParameters.getRepackingDataParameters().StationFilename,aExecParameters.getRepackingDataParameters().DomainFilename);
	    Set<Station> aStations = aStationManager.getStations();
		DACConstraintManager dCM = new DACConstraintManager(aStations,aExecParameters.getRepackingDataParameters().ConstraintFilename);
	
		log.info("Creating constraint grouper...");
		IComponentGrouper aGrouper = new ConstraintGrouper();
		
		log.info("Creating CNF lookup...");
		HybridCNFResultLookup aCNFLookup = new HybridCNFResultLookup(aExecParameters.getCNFDirectory(), aExecParameters.getCNFOutputName());
		
		HashSet<String> aCNFNames = new HashSet<String>();
		
		Set<Integer> aChannelRange = aExecParameters.getPackingChannels();
		
		CSVReader aReader = new CSVReader(new FileReader("/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Features/TunedClaspInstanceGeneration/sorted_InstanceGeneration_TunedClasp_hard_testing_filtered.csv"),',');
		String[] aLine;
		while((aLine = aReader.readNext())!=null){
			HashSet<Integer> aStationIDs = new HashSet<Integer>();
			int i=3;
			while(i<aLine.length && !aLine[i].replace("\\s", "").isEmpty() )
			{
				aStationIDs.add(Integer.valueOf(aLine[i]));
				i++;
				
			}
			HashSet<Station> aInstanceStations = new HashSet<Station>();
			
			for(Station aStation : aStations)
			{
				if(aStationIDs.contains(aStation.getID()))
				{
					aInstanceStations.add(aStation);
				}
			}
			
			Instance aInstance = new Instance(aInstanceStations,aChannelRange);


				
			Set<Set<Station>> aInstanceGroups = aGrouper.group(aInstance,dCM);
			for(Set<Station> aStationComponent : aInstanceGroups){
			
				if (aStationComponent.size()>1)
				{
					//Create the component group instance.
					Instance aComponentInstance = new Instance(aStationComponent,aChannelRange);
					String aCNFFileName = aCNFLookup.getCNFNameFor(aComponentInstance).split(File.separator)[aCNFLookup.getCNFNameFor(aComponentInstance).split(File.separator).length-1];
					
					//Save CNF name
					aCNFNames.add(aCNFFileName);
				}
			}
			
		}
		//Write the CNF names to file.
		File aCNFFile = new File("/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Features/TunedClaspInstanceGeneration/sorted_InstanceGeneration_TunedClasp_hard_testing_CNFs.csv");
		for(String aCNFName : aCNFNames)
		{
			FileUtils.write(aCNFFile, aCNFName+"\n",true);
		}
		
	}

}
