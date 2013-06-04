package ca.ubc.cs.beta.stationpacking.solver;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
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
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

import ca.ubc.cs.beta.stationpacking.datamanagers.DACConstraintManager2;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.experiment.experimentreport.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.solver.TAESolver.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.solver.TAESolver.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;

/**
 * SAT solver wrapper that uses Steve Ramage's AClib Target Algorithm Evaluators for execution in an asynchronous way, particularly useful for parallel solving of many instances..
 * @author afrechet
 *
 */
public class AsyncTAESolver {
	
	private final double fScenarioCutoff = 999999.0;
	
	private ParamConfigurationSpace fParamConfigurationSpace;
	private TargetAlgorithmEvaluator fTargetAlgorithmEvaluator;
	private long fSeed;
	private IComponentGrouper fGrouper;
	private IConstraintManager fManager;
	private ICNFEncoder fEncoder;
	
	private AsyncCachedCNFLookup fLookup;
	
	
	/**
	 * Construct a solver wrapper around a target algorithm evaluator.
	 * @param aParamConfigurationSpaceFile - the location of the ParamILS formatted parameter configuration space file.
	 * @param aAlgorithmExecutable - the execution string of the algorithm executable (<i> e.g. </i> "python SATwrapper.py").
	 * @param aExecDir - the directory in which to execute the algorithm (<i> e.g. </i> "[...]/SolverWrapper/").
	 * @param aTargetAlgorithmEvaluatorExecutionEnvironment - <i> e.g. </i> "CLI" command-line on system, "MYSQLDBTAE" plug-in for mySQL workers, ... 
	 * @param aManager - constraint manager for the repacking instance.
	 * @param aCNFDirectory - a location for the CNF to be written to.
	 * @param aLookup - CNF & Result lookup object.
	 * @param aEncoder - CNF encoder.
	 * @param aMaximumConcurrentExecutions - number of concurrent executions that can be done with the TAE.
	 * @param aSeed 
	 * @deprecated
	 */
	public AsyncTAESolver(IConstraintManager aManager, ICNFEncoder aEncoder, String aCNFDirectory, String aParamConfigurationSpaceFile, String aAlgorithmExecutable, String aExecDir, String aTargetAlgorithmEvaluatorExecutionEnvironment, int aMaximumConcurrentExecutions, long aSeed)
	{
		fEncoder = aEncoder;
		fManager = aManager;
		fGrouper = new ConstraintGrouper();
		fLookup = new AsyncCachedCNFLookup(aCNFDirectory);
		
		//Parameter configuration space
		fParamConfigurationSpace  = new ParamConfigurationSpace(new File(aParamConfigurationSpaceFile));
		
		//Algorithm Execution Config
		AlgorithmExecutionConfig aAlgorithmExecutionConfig = new AlgorithmExecutionConfig(aAlgorithmExecutable, aExecDir, fParamConfigurationSpace, false, false, fScenarioCutoff);
		
		//Target Algorithm Evaluator
		TargetAlgorithmEvaluatorOptions aTargetAlgorithmEvaluatorOptions = new TargetAlgorithmEvaluatorOptions();
		aTargetAlgorithmEvaluatorOptions.retryCount = 0 ;
		aTargetAlgorithmEvaluatorOptions.maxConcurrentAlgoExecs = aMaximumConcurrentExecutions;
		aTargetAlgorithmEvaluatorOptions.targetAlgorithmEvaluator = aTargetAlgorithmEvaluatorExecutionEnvironment;
		
		Map<String,AbstractOptions> aTargetAlgorithmEvaluatorOptionsMap = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
		
		fTargetAlgorithmEvaluator = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(aTargetAlgorithmEvaluatorOptions, aAlgorithmExecutionConfig, false, aTargetAlgorithmEvaluatorOptionsMap);
	}
	
	
	public AsyncTAESolver(DACConstraintManager2 aConstraintManager, ICNFEncoder aCNFEncoder,
			String aCNFDirectory, TargetAlgorithmEvaluator aTAE, AlgorithmExecutionConfig aTAEExecConfig, long aSeed) {
		fSeed = aSeed;
		fEncoder = aCNFEncoder;
		fManager = aConstraintManager;
		fGrouper = new ConstraintGrouper();
		fLookup = new AsyncCachedCNFLookup(aCNFDirectory);
		
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
	
	private TargetAlgorithmEvaluatorCallback getCompilingCallback(final Instance aInstance,final IExperimentReporter aAsynchronousReporter)
	{
		return new TargetAlgorithmEvaluatorCallback()
		{
			@Override
			public void onSuccess(List<AlgorithmRun> runs) {
				HashSet<SolverResult> aComponentResults = new HashSet<SolverResult>();
				for(AlgorithmRun aRun : runs)
				{
					if(!aRun.getRunResult().equals(RunResult.KILLED))
					{
						double aRuntime = aRun.getRuntime();				
						SATResult aResult;
						switch (aRun.getRunResult()){
							case SAT:
								aResult = SATResult.SAT;
								break;
							case UNSAT:
								aResult = SATResult.UNSAT;
								break;
							case TIMEOUT:
								aResult = SATResult.TIMEOUT;
								break;
							default:
								aResult = SATResult.CRASHED;
								break;
						}
						
						SolverResult aSolverResult = new SolverResult(aResult,aRuntime);

						
						//Add result to component results
						aComponentResults.add(aSolverResult);
					}	
				}
				try {
					aAsynchronousReporter.report(aInstance, mergeComponentResults(aComponentResults));
				} 
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				
			}

			@Override
			public void onFailure(RuntimeException t) {
				t.printStackTrace();
				System.exit(1);
				
			}
		};
	}
	
	/**
	 * A CNF (only) lookup for the asynchronous solver. 
	 * <i> We do not use a standard CNF/Result lookup because we inherently will use asynchronous TAE with workers which contain they're own lookup process. </i>
	 * @author afrechet
	 *
	 */
	private class AsyncCachedCNFLookup
	{
		
		private HashMap<Instance,String> fCNFMap;
		private String fCNFDirectory;
		/**
		 * Construct an asynchronous cached CNF llookup.
		 */
		public AsyncCachedCNFLookup(String aCNFDirectory)
		{
			fCNFMap = new HashMap<Instance,String>();
			fCNFDirectory = aCNFDirectory;
		}
		
		//Private function to create hashed filenames for CNF.
		private String hashforFilename(String aString)
		{
			MessageDigest aDigest = DigestUtils.getSha1Digest();
			try {
				byte[] aResult = aDigest.digest(aString.getBytes("UTF-8"));
			    String aResultString = new String(Hex.encodeHex(aResult));	
			    return aResultString;
			}
			catch (UnsupportedEncodingException e) {
			    throw new IllegalStateException("Could not encode filename", e);
			}
		}
		
		private String getNameCNF(Instance aInstance)
		{
			return hashforFilename(Station.hashStationSet(aInstance.getStations())+Instance.hashChannelSet(aInstance.getChannels()));
		}
		
		/**
		 * Return the CNF name for an instance.
		 * @param aInstance - problem instance to get CNF name for.
		 * @return the string CNF name for the problem instance.
		 */
		public String getCNFNameFor(Instance aInstance)
		{
			return fCNFDirectory+File.separatorChar+getNameCNF(aInstance)+".cnf";
		}
		
		/**
		 * Return true if the instance was saved by the lookup.
		 * @param aInstance - an instance to look for.
		 * @return true if the instance is present in the lookup, false otherwise.
		 */
		public boolean hasCNFFor(Instance aInstance)
		{
			return fCNFMap.containsKey(aInstance);
		}
		
		/**
		 * Put an instance in the lookup.
		 * @param aInstance - the instance to put in the lookup.
		 * @return ture if the instance was already there, false otherwise.
		 */
		public boolean putCNFfor(Instance aInstance)
		{
			return (fCNFMap.put(aInstance, getNameCNF(aInstance))!=null);
		}	
	}
	
	/**
	 * Solve the instance asynchronously, and give the result to the AsynchronousReporter once finished.
	 * @param aInstance - the instance to solve.
	 * @param aCutoff - the cutoff time.
	 * @param aAsynchronousReporter - the reporter in charge of the results.
	 * @throws Exception
	 */
	public void solve(Instance aInstance, double aCutoff, IExperimentReporter aAsynchronousReporter) throws Exception{

		Set<Integer> aChannelRange = aInstance.getChannels();
		
		//Group stations
		Set<Set<Station>> aInstanceGroups = fGrouper.group(aInstance.getStations(),fManager);
		
		HashMap<RunConfig,Instance> aToSolveInstances = new HashMap<RunConfig,Instance>();
	
		for(Set<Station> aStationComponent : aInstanceGroups){
			//Create the component group instance.
			Instance aComponentInstance = new Instance(aStationComponent,aChannelRange);
			
			//Name the instance
			String aCNFFileName = fLookup.getCNFNameFor(aComponentInstance);
			
			//Check if new CNF
			if(!fLookup.hasCNFFor(aComponentInstance))
			{
				//Encode the instance
				String aCNF = fEncoder.encode(aComponentInstance,fManager);
				try 
				{
					FileUtils.writeStringToFile(new File(aCNFFileName), aCNF);
				} 
				catch (IOException e) 
				{
					throw new IllegalStateException("Solver had a problem writing a CNF to file.",e);
				}
				//Save the CNF.
				fLookup.putCNFfor(aInstance);
			}	
			
			//Create the run config and add it to the to-do list.
			ProblemInstance aProblemInstance = new ProblemInstance(aCNFFileName);
			ProblemInstanceSeedPair aProblemInstanceSeedPair = new ProblemInstanceSeedPair(aProblemInstance,fSeed);
			RunConfig aRunConfig = new RunConfig(aProblemInstanceSeedPair, aCutoff, fParamConfigurationSpace.getDefaultConfiguration());
			
			aToSolveInstances.put(aRunConfig,aInstance);
		}
		
		List<RunConfig> aRunConfigs = new ArrayList<RunConfig>(aToSolveInstances.keySet());
		fTargetAlgorithmEvaluator.evaluateRunsAsync(aRunConfigs,getCompilingCallback(aInstance,aAsynchronousReporter),getPreemptingObserver());
		
	}
	
	private SolverResult mergeComponentResults(Collection<SolverResult> aComponentResults){
		double aRuntime = 0.0;
		
		HashSet<SATResult> aSATResults = new HashSet<SATResult>();
		for(SolverResult aSolverResult : aComponentResults)
		{
			aRuntime += aSolverResult.getRuntime();
			aSATResults.add(aSolverResult.getResult());
		}
		
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
		
		return new SolverResult(aSATResult,aRuntime);
	}
	
	
	public void waitForFinish()
	{
		fTargetAlgorithmEvaluator.waitForOutstandingEvaluations();
		fTargetAlgorithmEvaluator.notifyShutdown();
	}
		
}
