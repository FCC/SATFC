package ca.ubc.cs.beta.stationpacking.solver.taesolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.TargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.IComponentGrouper;


/**
 * SAT solver wrapper that uses Steve Ramage's AClib Target Algorithm Evaluators for execution.
 * @author afrechet, narnosti
 *
 */
public class TAESolver implements ISolver{
	
	private final double fScenarioCutoff = 999999.0;
	
	private ParamConfigurationSpace fParamConfigurationSpace;
	private TargetAlgorithmEvaluator fTargetAlgorithmEvaluator;
	private long fSeed;
	private IComponentGrouper fGrouper;
	private IConstraintManager fManager;
	private ICNFResultLookup fLookup;
	private ICNFEncoder fEncoder;
	
	
	/**
	 * Construct a solver wrapper around a target algorithm evaluator.
	 * @param aParamConfigurationSpaceFile - the location of the ParamILS formatted parameter configuration space file.
	 * @param aAlgorithmExecutable - the execution string of the algorithm executable (<i> e.g. </i> "python SATwrapper.py").
	 * @param aExecDir - the directory in which to execute the algorithm (<i> e.g. </i> "[...]/SolverWrapper/").
	 * @param aTargetAlgorithmEvaluatorExecutionEnvironment - <i> e.g. </i> "CLI" command-line on system, "MYSQLDBTAE" plug-in for mySQL workers, ... 
	 * @param aManager - constraint manager for the repacking instance.
	 * @param aLookup - CNF & Result lookup object.
	 * @param aEncoder - CNF encoder.
	 * @param aMaximumConcurrentExecutions - number of concurrent executions that can be done with the TAE.
	 * @deprecated
	 */
	public TAESolver(IConstraintManager aManager, ICNFResultLookup aLookup, ICNFEncoder aEncoder, String aParamConfigurationSpaceFile, String aAlgorithmExecutable, String aExecDir, String aTargetAlgorithmEvaluatorExecutionEnvironment, int aMaximumConcurrentExecutions, long aSeed)
	{
		fSeed = aSeed;
		fEncoder = aEncoder;
		fManager = aManager;
		fGrouper = new ConstraintGrouper();
		fLookup = aLookup;
		//Parameter configuration space
		fParamConfigurationSpace  = new ParamConfigurationSpace(new File(aParamConfigurationSpaceFile));
		
		//Algorithm Execution Config
		AlgorithmExecutionConfig aAlgorithmExecutionConfig = new AlgorithmExecutionConfig(aAlgorithmExecutable, aExecDir, fParamConfigurationSpace, false, false, fScenarioCutoff);
		
		//Target Algorithm Evaluator
		Map<String,AbstractOptions> aTargetAlgorithmEvaluatorOptionsMap = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
	
		TargetAlgorithmEvaluatorOptions aTargetAlgorithmEvaluatorOptions = new TargetAlgorithmEvaluatorOptions();
		aTargetAlgorithmEvaluatorOptions.retryCount = 0 ;
		aTargetAlgorithmEvaluatorOptions.maxConcurrentAlgoExecs = aMaximumConcurrentExecutions;
		aTargetAlgorithmEvaluatorOptions.targetAlgorithmEvaluator = aTargetAlgorithmEvaluatorExecutionEnvironment;
		
		//Setting CLI options.
		CommandLineTargetAlgorithmEvaluatorOptions CLIopts = (CommandLineTargetAlgorithmEvaluatorOptions) aTargetAlgorithmEvaluatorOptionsMap.get("CLI");
		CLIopts.logAllCallStrings = false;
		CLIopts.logAllProcessOutput = false;
		CLIopts.concurrentExecution = true;
		
		fTargetAlgorithmEvaluator = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(aTargetAlgorithmEvaluatorOptions, aAlgorithmExecutionConfig, false, aTargetAlgorithmEvaluatorOptionsMap);
	}
	
	
	public TAESolver(DACConstraintManager2 aConstraintManager, ICNFEncoder aCNFEncoder,
			ICNFResultLookup aLookup, IComponentGrouper aGrouper, 
			TargetAlgorithmEvaluator aTAE, AlgorithmExecutionConfig aTAEExecConfig, long aSeed) {
		fSeed = aSeed;
		fEncoder = aCNFEncoder;
		fManager = aConstraintManager;
		fGrouper = aGrouper;
		fLookup = aLookup;
		
		fParamConfigurationSpace  = aTAEExecConfig.getParamFile();
		fTargetAlgorithmEvaluator = aTAE;
	}


	/**
	 * Target algorithm run observer that kills runs when one UNSAT or TIMEOUT is found.
	 * @return a preempting run status observer.
	 */
	private TargetAlgorithmEvaluatorRunObserver getPreemptingObserver()
	{
		return new TargetAlgorithmEvaluatorRunObserver(){		
			@Override
			public void currentStatus(List<? extends KillableAlgorithmRun> runs) {
				boolean aKill = false;
				for(KillableAlgorithmRun aRun : runs)
				{
					if(!aRun.getRunResult().equals(RunResult.SAT) && !aRun.getRunResult().equals(RunResult.RUNNING))
					{
						aKill = true;
						break;
					}
				}
				if(aKill)
				{
					for(KillableAlgorithmRun aRun : runs)
					{
						aRun.kill();
					}
				}
			}	
		};
	}
	
	/* NA - Returns a SolverResult corresponding to packing stations aInstance.getStations()
	 * into channels aInstance.getChannelRange() given constraints imposed by fManager.
	 * 
	 * Optimized in the following ways:
	 * 1. Clusters the Instance into disjoint connected components
	 * 2. Checks with fLookup to see whether each component has been solved.
	 *    If any component has been determined to be UNSAT, the method returns.
	 * 3. Sequentially solves each new component, returning if any of them are not in SAT.
	 * 
	 */
	@Override
	public SolverResult solve(Instance aInstance, double aCutoff) throws Exception{
			
		Set<Integer> aChannelRange = aInstance.getChannels();
		
		//Group stations
		Set<Set<Station>> aInstanceGroups = fGrouper.group(aInstance.getStations(),fManager);
		
		ArrayList<SolverResult> aComponentResults = new ArrayList<SolverResult>();
		HashMap<RunConfig,Instance> aToSolveInstances = new HashMap<RunConfig,Instance>();
	
		for(Set<Station> aStationComponent : aInstanceGroups){
			//Create the component group instance.
			Instance aComponentInstance = new Instance(aStationComponent,aChannelRange);
			
			//Check if present
			if(fLookup.hasSolverResult(aComponentInstance))
			{
				SolverResult aSolverResult = fLookup.getSolverResult(aComponentInstance);
				//Early preemption if component is UNSAT,
				if (!aSolverResult.getResult().equals(SATResult.SAT) )
				{
					System.out.println(aComponentInstance);
					return new SolverResult(SATResult.UNSAT,0.0,new HashMap<Integer,Set<Station>>());
				}
				
				aComponentResults.add(aSolverResult);
			}
			else
			{
				//Not present, CNF must be solved.
				//Name the instance
				String aCNFFileName = fLookup.getCNFNameFor(aComponentInstance);
				
				File aCNFFile = new File(aCNFFileName);
				
				if(!aCNFFile.exists())
				{
					//Encode the instance
					String aCNF = fEncoder.encode(aComponentInstance,fManager);
					//Write it to disk
					try 
					{
						FileUtils.writeStringToFile(aCNFFile, aCNF);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
				//Create the run config and add it to the to-do list.
				ProblemInstance aProblemInstance = new ProblemInstance(aCNFFileName);
				ProblemInstanceSeedPair aProblemInstanceSeedPair = new ProblemInstanceSeedPair(aProblemInstance,fSeed);
				RunConfig aRunConfig = new RunConfig(aProblemInstanceSeedPair, aCutoff, fParamConfigurationSpace.getDefaultConfiguration());
				
				aToSolveInstances.put(aRunConfig,aComponentInstance);
				//System.out.println("\n\n\n"+aToSolveInstances+" "+aToSolveInstances.size()+"\n\n\n");

			}
		}

		//System.out.println("\n\n%%%%%%%%%%%%%%%%%%%%%%%% "+aToSolveInstances.size()+"\n\n");


		List<RunConfig> aRunConfigs = new ArrayList<RunConfig>(aToSolveInstances.keySet());
		List<AlgorithmRun> aRuns = fTargetAlgorithmEvaluator.evaluateRun(aRunConfigs,getPreemptingObserver());
		
		
		//if(!aRuns.isEmpty()) System.out.println("\n\n######################### \n\n");

		
		for(AlgorithmRun aRun : aRuns)
		{

			if(!aRun.getRunResult().equals(RunResult.KILLED))
			{

				double aRuntime = aRun.getRuntime();				
				SATResult aResult;
				Map<Integer,Set<Station>> aAssignment = new HashMap<Integer,Set<Station>>();
				switch (aRun.getRunResult()){
					case SAT:
						aResult = SATResult.SAT;
						
						//Grab assignment
						String aAdditionalRunData = aRun.getAdditionalRunData();
						aAssignment = fEncoder.decode(aToSolveInstances.get(aRun.getRunConfig()), aAdditionalRunData);
						
						break;
					case UNSAT:
						aResult = SATResult.UNSAT;
						break;
					case TIMEOUT:
						aResult = SATResult.TIMEOUT;
						break;
					default:
						aResult = SATResult.CRASHED;
						throw new IllegalStateException("Run "+aRun+" crashed!");
				}
				
				SolverResult aSolverResult = new SolverResult(aResult,aRuntime,aAssignment);
				
				//Save result if successfully computed

				fLookup.putSolverResult(aToSolveInstances.get(aRun.getRunConfig()),aSolverResult);

				
				//Add result to component results
				aComponentResults.add(aSolverResult);
			}	
		}
		return mergeComponentResults(aComponentResults);

	}
	
	private SolverResult mergeComponentResults(Collection<SolverResult> aComponentResults){
		double aRuntime = 0.0;
		
		//Merge runtimes as sum of times.
		HashSet<SATResult> aSATResults = new HashSet<SATResult>();
		for(SolverResult aSolverResult : aComponentResults)
		{
			aRuntime += aSolverResult.getRuntime();
			aSATResults.add(aSolverResult.getResult());
		}
		
		//Merge SAT results 
		SATResult aSATResult = SATResult.SAT;
		
		if(aSATResults.contains(SATResult.UNSAT))
		{
			aSATResult = SATResult.UNSAT;
		}
		else if(aSATResults.contains(SATResult.CRASHED))
		{
			aSATResult = SATResult.CRASHED;
		}
		else if(aSATResults.contains(SATResult.TIMEOUT))
		{
			aSATResult = SATResult.TIMEOUT;
		}
		
		//Merge assignments
		Map<Integer,Set<Station>> aAssignment = new HashMap<Integer,Set<Station>>();
		if(aSATResult.equals(SATResult.SAT))
		{
			for(SolverResult aComponentResult : aComponentResults)
			{
				Map<Integer,Set<Station>> aComponentAssignment = aComponentResult.getAssignment();
				for(Integer aAssignedChannel : aComponentAssignment.keySet())
				{
					if(!aAssignment.containsKey(aAssignedChannel))
					{
						aAssignment.put(aAssignedChannel, new HashSet<Station>());
					}
					aAssignment.get(aAssignedChannel).addAll(aComponentAssignment.get(aAssignedChannel));
				}
			}
		}
		
		
		return new SolverResult(aSATResult,aRuntime,aAssignment);
	}
	
	
	
}
