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
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.cnfwriter.CNFStringWriter;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.componentgrouper.IComponentGrouper;


/**
 * SAT solver wrapper that uses Steve Ramage's AClib Target Algorithm Evaluators for execution.
 * @author afrechet, narnosti
 *
 */
public class TAESolver implements ISolver{
	
	private static Logger log = LoggerFactory.getLogger(TAESolver.class);
	
	private ParamConfigurationSpace fParamConfigurationSpace;
	private TargetAlgorithmEvaluator fTargetAlgorithmEvaluator;
	
	private IConstraintManager fManager;
	private ICNFResultLookup fLookup;
	private ICNFEncoder fEncoder;
	private CNFStringWriter fStringWriter;
	
	private IComponentGrouper fGrouper;
	
	private final boolean fKeepCNFs;
	
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
	public TAESolver(IConstraintManager aConstraintManager, ICNFEncoder aCNFEncoder,
			ICNFResultLookup aLookup, IComponentGrouper aGrouper, CNFStringWriter aStringWriter,
			TargetAlgorithmEvaluator aTAE, AlgorithmExecutionConfig aTAEExecConfig, boolean aKeepCNFs) {
		
		fEncoder = aCNFEncoder;
		fManager = aConstraintManager;
		fGrouper = aGrouper;
		fLookup = aLookup;
		fStringWriter = aStringWriter;
		
		fParamConfigurationSpace  = aTAEExecConfig.getParamFile();
		fTargetAlgorithmEvaluator = aTAE;
		
		fKeepCNFs = aKeepCNFs;
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
				
				//Check if one run has terminated without a SAT result.
				for(KillableAlgorithmRun aRun : runs)
				{
					if(!aRun.getRunResult().equals(RunResult.SAT) && !aRun.getRunResult().equals(RunResult.RUNNING))
					{
						aKill = true;
						break;
					}
				}
				
				//Check if the sum of the runtimes is less than each job's cutoff (which is assumed to be the same for every job).
				if(!aKill)
				{
					double aCutoff = 0.0;
					double aSumRuntimes = 0.0;
					for(KillableAlgorithmRun aRun : runs)
					{
						aCutoff = aRun.getRunConfig().getCutoffTime();
						aSumRuntimes += aRun.getRuntime();
					}
					//TODO Some cutoff management magic right here.
					if(aSumRuntimes>1.01+aCutoff+1)
					{
						aKill = true;
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
	public SolverResult solve(StationPackingInstance aInstance, double aCutoff, long aSeed) throws Exception{
		long aStartTime = System.nanoTime();
		
		log.info("Solving instance of {}",aInstance.getInfo());
		
		Set<Integer> aChannelRange = aInstance.getChannels();
		
		//Group stations
		Set<Set<Station>> aInstanceGroups = fGrouper.group(aInstance,fManager);
		
		ArrayList<SolverResult> aComponentResults = new ArrayList<SolverResult>();
		HashMap<RunConfig,StationPackingInstance> aToSolveInstances = new HashMap<RunConfig,StationPackingInstance>();
		
		HashSet<String> aCNFs = new HashSet<String>();
		//Create the runs to execute.
		for(Set<Station> aStationComponent : aInstanceGroups){
			//Create the component group instance.
			StationPackingInstance aComponentInstance = new StationPackingInstance(aStationComponent,aChannelRange);
			
			//Check if present
			if(fLookup.hasSolverResult(aComponentInstance))
			{
				SolverResult aSolverResult = fLookup.getSolverResult(aComponentInstance);
				//Early preemption if component is UNSAT,
				if (!aSolverResult.getResult().equals(SATResult.SAT) )
				{
					return new SolverResult(SATResult.UNSAT,0.0,new HashMap<Integer,Set<Station>>());
				}
				
				aComponentResults.add(aSolverResult);
			}
			else
			{
				//Not present, CNF must be solved.
				//Name the instance
				String aCNFFileName = fLookup.getCNFNameFor(aComponentInstance);
				aCNFs.add(aCNFFileName);
				
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
		}
		
		//Execute the runs
		List<RunConfig> aRunConfigs = new ArrayList<RunConfig>(aToSolveInstances.keySet());
		List<AlgorithmRun> aRuns = fTargetAlgorithmEvaluator.evaluateRun(aRunConfigs,getPreemptingObserver());
		
		
		for(AlgorithmRun aRun : aRuns)
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
			
			//Save result if successfully computed
			if(!(aResult.equals(SATResult.CRASHED) || aResult.equals(SATResult.KILLED)))
			{
				fLookup.putSolverResult(aToSolveInstances.get(aRun.getRunConfig()),aSolverResult);
			}
			
			//Add result to component results
			aComponentResults.add(aSolverResult);
		}
		
		if(aComponentResults.size()!=aInstanceGroups.size())
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
		
		log.info("Result : {}",aResult);
		log.info("Total time taken "+(System.nanoTime()-aStartTime)*Math.pow(10, -9)+" seconds");
		
		log.info("Cleaning up.");
		if(!fKeepCNFs)
		{	
			log.info("Deleting CNFs.");
			for(String aCNFFileName : aCNFs)
			{
				File aCNFFile = new File(aCNFFileName);
				
				if(aCNFFile.exists())
				{
					aCNFFile.delete();
				}
			}
		}
		log.info("Writing results to file.");
		fLookup.writeToFile();
		
		log.info("Done.");
		
		return aResult;

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


	@Override
	public void notifyShutdown() {
		if(fTargetAlgorithmEvaluator != null)
		{
			fTargetAlgorithmEvaluator.notifyShutdown();
		}
		
	}
	
	
	
}
