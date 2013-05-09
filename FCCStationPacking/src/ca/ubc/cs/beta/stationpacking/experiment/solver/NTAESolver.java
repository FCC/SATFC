package ca.ubc.cs.beta.stationpacking.experiment.solver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.cli.CommandLineTargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.currentstatus.CurrentRunStatusObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.loader.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.instance.NInstance;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnflookup.ICNFLookup;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.componentgrouper.ConstraintGrouper;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;

/**
 * SAT solver wrapper that uses Steve Ramage's AClib Target Algorithm Evaluators for execution.
 * @author afrechet, narnosti
 *
 */
public class NTAESolver implements ISolver{
	
	private final double fScenarioCutoff = 999999.0;
	
	private ParamConfigurationSpace fParamConfigurationSpace;
	private TargetAlgorithmEvaluator fTargetAlgorithmEvaluator;
	private int fSeed;
	private IComponentGrouper fGrouper;
	private IConstraintManager fManager;
	private ICNFLookup fLookup;
	private ICNFEncoder fEncoder;
	
	
	/**
	 * Construct a solver wrapper around a target algorithm evaluator.
	 * @param aParamConfigurationSpaceFile - the location of the ParamILS formatted parameter configuration space file.
	 * @param aAlgorithmExecutable - the execution string of the algorithm executable (<i> e.g. </i> "python SATwrapper.py").
	 * @param aExecDir - the directory in which to execute the algorithm (<i> e.g. </i> "[...]/SolverWrapper/").
	 * @param aTargetAlgorithmEvaluatorExecutionEnvironment (<i> e.g. </i> "CLI" command-line on system, "MYSQLDBTAE" plug-in for mySQL workers, ...). 
	 */
	public NTAESolver(IConstraintManager aManager, ICNFLookup aLookup, ICNFEncoder aEncoder, String aParamConfigurationSpaceFile, String aAlgorithmExecutable, String aExecDir, String aTargetAlgorithmEvaluatorExecutionEnvironment, int aMaximumConcurrentExecutions)
	{
		fEncoder = aEncoder;
		fManager = aManager;
		fGrouper = new ConstraintGrouper(fManager);
		fLookup = aLookup;
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
		
		//Setting CLI options.
		CommandLineTargetAlgorithmEvaluatorOptions CLIopts = (CommandLineTargetAlgorithmEvaluatorOptions) aTargetAlgorithmEvaluatorOptionsMap.get("CLI");
		CLIopts.logAllCallStrings = false;
		CLIopts.logAllProcessOutput = false;
		CLIopts.concurrentExecution = true;
		
		fTargetAlgorithmEvaluator = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(aTargetAlgorithmEvaluatorOptions, aAlgorithmExecutionConfig, false, aTargetAlgorithmEvaluatorOptionsMap);
	}
	
	
	/**
	 * Target algorithm run observer that kills runs when one UNSAT or TIMEOUT is found.
	 * @return a preempting run status observer.
	 */
	private CurrentRunStatusObserver getPreemptingObserver()
	{
		return new CurrentRunStatusObserver(){		
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
	public SolverResult solve(IInstance aInstance, double aCutoff) {
		Map<RunConfig,IInstance> aRunConfigMap = new HashMap<RunConfig,IInstance>();
		Set<Set<Station>> aInstanceGroups = fGrouper.group(aInstance.getStations());
		//System.out.println("\n"+aInstanceGroups.size()+" distinct groups: "+aInstanceGroups+"\n");
		Set<Integer> aChannelRange = aInstance.getChannelRange();
		SATResult aSATResult;		
		for(Set<Station> aStationComponent : aInstanceGroups){
			NInstance aComponentInstance = new NInstance(aStationComponent,aChannelRange);
			String aCNFFileName = fLookup.getNameFor(aComponentInstance);//NA if we already have computed the result, just reuse it
			if((aSATResult = fLookup.getSATResult(aComponentInstance))!=null) {
				if(aSATResult.equals(SATResult.UNSAT)) return new SolverResult(SATResult.UNSAT,0.0);
			} else {
				//System.out.println("\n WRITING NEW SAT INSTANCE\n");
				String aCNF = fEncoder.encode(aComponentInstance,fManager);
				try {
					FileUtils.writeStringToFile(new File(aCNFFileName), aCNF);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//Create run for TAE.
			ProblemInstance aProblemInstance = new ProblemInstance(aCNFFileName);
			ProblemInstanceSeedPair aProblemInstanceSeedPair = new ProblemInstanceSeedPair(aProblemInstance,fSeed);
			RunConfig aRunConfig = new RunConfig(aProblemInstanceSeedPair, aCutoff, fParamConfigurationSpace.getDefaultConfiguration());
			aRunConfigMap.put(aRunConfig,aComponentInstance);
		}
		
		double aRuntime = 0.0;
		AlgorithmRun aRun;
		List<AlgorithmRun> aRuns;
		List<RunConfig> aRunConfigList = new ArrayList<RunConfig>();
		for(RunConfig aConfig : aRunConfigMap.keySet()){
			aRunConfigList.clear();
			aRunConfigList.add(aConfig);
			aRuns = fTargetAlgorithmEvaluator.evaluateRun(aRunConfigList,getPreemptingObserver());
			if(aRuns.size()>1) {
				try { throw new Exception("I'm confused in NTAESolver - NA."); 
				} catch(Exception e){ e.printStackTrace(); }
			}
			aRun = aRuns.get(0);
			aRuntime += aRun.getRuntime();
			//NA - we should save the station assignments
			switch(aRun.getRunResult()){
				case SAT:
					fLookup.putSATResult(aRunConfigMap.get(aConfig),new SolverResult(SATResult.SAT,aRun.getRuntime()));
					break;
				case UNSAT:
					fLookup.putSATResult(aRunConfigMap.get(aConfig), new SolverResult(SATResult.UNSAT,aRun.getRuntime()));
					return new SolverResult(SATResult.UNSAT,aRuntime);
				case TIMEOUT:
					fLookup.putSATResult(aRunConfigMap.get(aConfig),new SolverResult(SATResult.TIMEOUT,aRun.getRuntime()));
					return new SolverResult(SATResult.TIMEOUT,aRuntime);
				default:
					fLookup.putSATResult(aRunConfigMap.get(aConfig),new SolverResult(SATResult.CRASHED,aRun.getRuntime()));
					return new SolverResult(SATResult.CRASHED,aRuntime);
			}
		}
		//NA - If we've made it through the loop without returning, the Instance is SAT
		return new SolverResult(SATResult.SAT,aRuntime);		
	}

}
