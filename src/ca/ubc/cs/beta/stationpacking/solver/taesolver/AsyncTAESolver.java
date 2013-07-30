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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.cnfwriter.CNFStringWriter;
import ca.ubc.cs.beta.stationpacking.solver.reporters.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.AsyncCachedCNFLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.IComponentGrouper;

/**
 * SAT solver wrapper that uses Steve Ramage's AClib Target Algorithm Evaluators for execution in an asynchronous way, particularly useful for parallel solving of many instances..
 * 
 * @author afrechet
 * 
 */
public class AsyncTAESolver {
	
	private static Logger log = LoggerFactory.getLogger(TAESolver.class);
	
	private ParamConfigurationSpace fParamConfigurationSpace;
	private TargetAlgorithmEvaluator fTargetAlgorithmEvaluator;
	private IComponentGrouper fGrouper;
	private IConstraintManager fManager;
	private ICNFEncoder fEncoder;
	private CNFStringWriter fStringWriter;
	
	private AsyncCachedCNFLookup fLookup;
	
	/**
	 * 
	 * @param aConstraintManager - the manager in charge of constraints.
	 * @param aCNFEncoder - the encoder in charge of taking constraints and an instance and producing a CNF clause set.
	 * @param aLookup - the CNF lookup in charge of monitoring CNFs.
	 * @param aGrouper - the component grouper in charge of partitioning instance in subinstances.
	 * @param aStringWriter - the writer in charge of transforming a CNF clause set in a string representation.
	 * @param aTAE - an AClib Target Algorithm Evaluator in charge of running SAT solver.
	 * @param aTAEExecConfig - the TAE's configuration.
	 */
	public AsyncTAESolver(IConstraintManager aConstraintManager, ICNFEncoder aCNFEncoder,
			ICNFResultLookup aLookup, IComponentGrouper aGrouper, CNFStringWriter aStringWriter,
			TargetAlgorithmEvaluator aTAE, AlgorithmExecutionConfig aTAEExecConfig) {
		
		fEncoder = aCNFEncoder;
		fManager = aConstraintManager;
		fGrouper = aGrouper;
		try
		{
			fLookup = (AsyncCachedCNFLookup) aLookup;
		}
		catch(ClassCastException e)
		{
			throw new IllegalArgumentException("The CNF lookup must be asynchronous! "+e.getMessage());
		}
		fStringWriter = aStringWriter;
		
		fParamConfigurationSpace  = aTAEExecConfig.getParamFile();
		fTargetAlgorithmEvaluator = aTAE;
	}


	private TargetAlgorithmEvaluatorCallback getCompilingCallback(final StationPackingInstance aInstance,final IExperimentReporter aAsynchronousReporter,final HashMap<RunConfig,StationPackingInstance> aToSolveInstances)
	{
		return new TargetAlgorithmEvaluatorCallback()
		{
			@Override
			public void onSuccess(List<AlgorithmRun> runs){
				HashSet<SolverResult> aComponentResults = new HashSet<SolverResult>();
				for(AlgorithmRun aRun : runs)
				{
					double aRuntime = aRun.getRuntime();				
					SATResult aResult;
					Map<Integer,Set<Station>> aAssignment = new HashMap<Integer,Set<Station>>();
					switch (aRun.getRunResult()){
						case KILLED:
							aResult = SATResult.KILLED;
							break;
						case SAT:
							aResult = SATResult.SAT;
							
							//Grab assignment
							String aAdditionalRunData = aRun.getAdditionalRunData();
							StationPackingInstance aGroupInstance = aToSolveInstances.get(aRun.getRunConfig());
							Clause aAssignmentClause = fStringWriter.stringToAssignmentClause(aGroupInstance, aAdditionalRunData);
							aAssignment = fEncoder.decode(aGroupInstance, aAssignmentClause);
			
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
					
					//Add result to component results
					aComponentResults.add(aSolverResult);
				}
				
				if(aComponentResults.size()!=aToSolveInstances.size())
				{
					throw new IllegalStateException("Not as many results as there are instance groups!");
				}
				
				//Merge all the results
				SolverResult aResult = mergeComponentResults(aComponentResults);
				
				log.info("Terminated.");
				
				//Post-process the result for correctness.
				if(aResult.getResult().equals(SATResult.SAT))
				{
			
					log.info("Independently verifying the veracity of returned assignment");
					//Check assignment has the right number of stations
					int aAssignmentSize = 0;
					for(Integer aChannel : aResult.getAssignment().keySet())
					{
						aAssignmentSize += aResult.getAssignment().get(aChannel).size();
					}
					if(aAssignmentSize!=aInstance.getNumberofStations())
					{
						throw new IllegalStateException("Merged station assignment doesn't assign exactly the stations in the instance.");
					}
				
					
					//Check that assignment is indeed satisfiable.
					if(!fManager.isSatisfyingAssignment(aResult.getAssignment())){
						
						log.error("Bad assignment:");
						for(Integer aChannel : aResult.getAssignment().keySet())
						{
							log.error(aChannel+','+aResult.getAssignment().get(aChannel).toString());
						}
						
						throw new IllegalStateException("Merged station assignment violates some pairwise interference constraint.");
					}
					else
					{
						log.info("Assignment was independently verified to be satisfiable.");
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
	 * Solve the instance asynchronously, and give the result to the AsynchronousReporter once finished.
	 * @param aInstance - the instance to solve.
	 * @param aCutoff - the cutoff time.
	 * @param aSeed - the seed to run the solver with.
	 * @param aAsynchronousReporter - the (asynchronous) reporter in charge of the results.
	 * @throws Exception
	 */
	public void solve(StationPackingInstance aInstance, double aCutoff, long aSeed, IExperimentReporter aAsynchronousReporter) throws Exception{

		log.info("Solving instance of {}",aInstance.getInfo());
		
		Set<Integer> aChannelRange = aInstance.getChannels();
		
		//Group stations
		Set<Set<Station>> aInstanceGroups = fGrouper.group(aInstance,fManager);
		
		HashMap<RunConfig,StationPackingInstance> aToSolveInstances = new HashMap<RunConfig,StationPackingInstance>();

		//Create the runs to execute.
		for(Set<Station> aStationComponent : aInstanceGroups){
			
			//Create the component group instance.
			StationPackingInstance aComponentInstance = new StationPackingInstance(aStationComponent,aChannelRange);
			
			
			//Not present, CNF must be solved.
			//Name the instance
			String aCNFFileName = fLookup.getCNFNameFor(aComponentInstance);
			
			File aCNFFile = new File(aCNFFileName);
			
			if(!aCNFFile.exists())
			{
				//Encode the instance
				Set<Clause> aClauseSet = fEncoder.encode(aComponentInstance,fManager);
				String aCNF = fStringWriter.clausesToString(aComponentInstance,aClauseSet);
				
				
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
			ProblemInstanceSeedPair aProblemInstanceSeedPair = new ProblemInstanceSeedPair(aProblemInstance,aSeed);
			RunConfig aRunConfig = new RunConfig(aProblemInstanceSeedPair, aCutoff, fParamConfigurationSpace.getDefaultConfiguration());
			
			aToSolveInstances.put(aRunConfig,aComponentInstance);
		
		}
		
		//Execute the runs
		List<RunConfig> aRunConfigs = new ArrayList<RunConfig>(aToSolveInstances.keySet());
		
		//We are not providing any preempting observer when doing async runs as we do not want to kill (possibly) shared instances.
		fTargetAlgorithmEvaluator.evaluateRunsAsync(aRunConfigs,getCompilingCallback(aInstance,aAsynchronousReporter,aToSolveInstances));
		
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
		SATResult aSATResult = SATResult.CRASHED;
		
		if(aSATResults.size()==1)
		{
			aSATResult = aSATResults.iterator().next();
		}
		else if(aSATResults.contains(SATResult.UNSAT))
		{
			aSATResult = SATResult.UNSAT;
		}
		else if(aSATResults.contains(SATResult.CRASHED))
		{
			aSATResult = SATResult.CRASHED;
		}
		else if(aSATResults.contains(SATResult.TIMEOUT) || aSATResults.contains(SATResult.KILLED))
		{
			aSATResult = SATResult.TIMEOUT;
		}
		
		//If all runs were killed, it is because we went over time. 
		if(aSATResult.equals(SATResult.KILLED))
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

	
	
	public void waitForFinish()
	{
		fTargetAlgorithmEvaluator.waitForOutstandingEvaluations();
		notifyShutdown();
	}
	
	
	public void notifyShutdown() {
		if(fTargetAlgorithmEvaluator != null)
		{
			fTargetAlgorithmEvaluator.notifyShutdown();
		}
		
	}
		
}
