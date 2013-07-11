package ca.ubc.cs.beta.stationpacking.execution;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.execution.parameters.asyncresolving.AsyncResolvingParameters;
import ca.ubc.cs.beta.stationpacking.solver.reporters.AsynchronousLocalExperimentReporter;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.AsyncTAESolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class AsyncInstanceResolvingExecutor {

	private static Logger log = LoggerFactory.getLogger(AsyncInstanceResolvingExecutor.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		String[] aPaxosTargetArgs = {
				"-EXPERIMENT_NAME",
				"ResolvingInstances",
				"-INSTANCE_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/downloads/FC_compare/ResolveInstances.csv",
				"-EXPERIMENT_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Results/Resolving",
				"-DOMAINS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Domain-041813.csv",
				"-CONSTRAINTS_FILE",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Interferences-041813.csv",
				"-CNF_DIR",
				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/CNFs",
				"-SOLVER",
				"tunedclasp",
				"--execDir",
				"/ubc/cs/home/a/afrechet/arrow-space/git/fcc-station-packing/FCCStationPacking/SATsolvers",
				"--algoExec",
				"python solverwrapper.py",
				"--cutoffTime",
				"1800",
				"-CUTOFF",
				"1800",
				"--tae",
				"MYSQLDB",
				"--mysqldbtae-pool",
				"resolving_july"
				};
		
		args = aPaxosTargetArgs;
		
		//Parse the command line arguments in a parameter object.
		AsyncResolvingParameters aInstanceResolvingParameters = new AsyncResolvingParameters();
		JCommander aParameterParser = JCommanderHelper.getJCommander(aInstanceResolvingParameters, aInstanceResolvingParameters.SolverParameters.AvailableTAEOptions);
		try
		{
			aParameterParser.parse(args);
		}
		catch (ParameterException aParameterException)
		{
			List<UsageSection> sections = ConfigToLaTeX.getParameters(aInstanceResolvingParameters,aInstanceResolvingParameters.SolverParameters.AvailableTAEOptions);
			
			boolean showHiddenParameters = false;
			
			//A much nicer usage screen than JCommander's 
			ConfigToLaTeX.usage(sections, showHiddenParameters);
			
			log.error(aParameterException.getMessage());
			return;
		}
		
		AsynchronousLocalExperimentReporter aReporter = aInstanceResolvingParameters.getExperimentReporter();
		log.info("Starting to write instances...");
		aReporter.startWritingReport();
		
		
		int aNumberOfBatches = 100;
		
		ArrayList<StationPackingInstance> aInstances = aInstanceResolvingParameters.getInstances();
//		//Batching evaluate run asyncs to not run out of memory when submitting instances (thread creation for every evaluate run async starves memory).
//		for(int aBatchIndex =0;aBatchIndex<aNumberOfBatches;aBatchIndex++)
//		{
//			log.info("Writing batch {} out of {}...",aBatchIndex,aNumberOfBatches);
			AsyncTAESolver aSolver = null;
			try
			{
				try 
				{
					aSolver = aInstanceResolvingParameters.SolverParameters.getSolver();
					
					
					log.info("Submitting the instances...");
					
					for(int aInstanceIndex = 0; aInstanceIndex < aInstances.size(); aInstanceIndex++)
					{
						StationPackingInstance aInstance = aInstances.get(aInstanceIndex);
//						if(aInstanceIndex%aNumberOfBatches == aBatchIndex)
//						{
							aSolver.solve(aInstance, aInstanceResolvingParameters.Cutoff, aInstanceResolvingParameters.Seed, aReporter);
//						}
						
						
					}
					
					
//					log.info("All instances submitted, waiting for completion...");
//					aSolver.waitForFinish();
					
					
	
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
				
			}
			finally{
				
				aSolver.notifyShutdown();
			}
//		}
		
		//Kill report writing process
		log.info("Done! Shutting down EVERYTHING!");
		aReporter.stopWritingReport();
		
	}
	
		
}
