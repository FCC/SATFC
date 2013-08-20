package ca.ubc.cs.beta.stationpacking.solvers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;
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
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.CNFCompressor;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;


/**
 * SAT solver wrapper that uses Steve Ramage's AClib Target Algorithm Evaluators for execution.
 * @author afrechet, narnosti
 *
 */
public class TAEBasedSolver implements ISolver{
	
	private static Logger log = LoggerFactory.getLogger(TAEBasedSolver.class);
	
	private ParamConfigurationSpace fParamConfigurationSpace;
	private TargetAlgorithmEvaluator fTargetAlgorithmEvaluator;
	
	private IConstraintManager fManager;
	private ICNFResultLookup fLookup;
	private ISATEncoder fEncoder;
	
	private IComponentGrouper fGrouper;
	
	private final boolean fKeepCNFs;
	
	/**
	 * 
	 * @param aConstraintManager - the manager in charge of constraints.
	 * @param aSATEncoder - the encoder in charge of taking constraints and an instance and producing a CNF clause set.
	 * @param aLookup - the CNF lookup in charge of monitoring CNFs.
	 * @param aGrouper - the component grouper in charge of partitioning instance in subinstances.
	 * @param aTAE - an AClib Target Algorithm Evaluator in charge of running SAT solver.
	 * @param aTAEExecConfig - the TAE's configuration.
	 */
	public TAEBasedSolver(IConstraintManager aConstraintManager, ISATEncoder aSATEncoder,
			ICNFResultLookup aLookup, IComponentGrouper aGrouper, TargetAlgorithmEvaluator aTAE, AlgorithmExecutionConfig aTAEExecConfig, boolean aKeepCNFs) {
		
		fEncoder = aSATEncoder;
		fManager = aConstraintManager;
		fGrouper = aGrouper;
		fLookup = aLookup;
		
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
					if(aSumRuntimes>1.01*aCutoff+1)
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
	public SolverResult solve(StationPackingInstance aInstance, double aCutoff, long aSeed){
		long aStartTime = System.nanoTime();
		
		log.info("Solving instance of {}",aInstance.getInfo());
		
		Set<Integer> aChannelRange = aInstance.getChannels();
		
		//Group stations
		Set<Set<Station>> aInstanceGroups = fGrouper.group(aInstance,fManager);
		
		ArrayList<SolverResult> aComponentResults = new ArrayList<SolverResult>();
		HashMap<RunConfig,StationPackingInstance> aToSolveInstances = new HashMap<RunConfig,StationPackingInstance>();
		HashMap<RunConfig,CNFCompressor> aComponentEncoders = new HashMap<RunConfig,CNFCompressor>();
		
		HashSet<String> aCNFs = new HashSet<String>();
		//Create the runs to execute.
		for(Set<Station> aStationComponent : aInstanceGroups){
			
			CNFCompressor aComponentEncoder = new CNFCompressor();
			
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
				
				//Encode the instance
				CNF aCNF = fEncoder.encode(aComponentInstance);
				CNF aCompressedCNF = aComponentEncoder.compress(aCNF);
				
				String aCNFString = aCompressedCNF.toDIMACS(new String[]{"FCC Station packing instance.","[Channels]_[Stations] ",aComponentInstance.toString()});
				
				//Write it to disk
				try 
				{
					FileUtils.writeStringToFile(aCNFFile, aCNFString);
				} 
				catch (IOException e) 
				{
					throw new IllegalStateException("Could not write CNF to file ("+e.getMessage()+").");
				}
				
				//Create the run config and add it to the to-do list.
				ProblemInstance aProblemInstance = new ProblemInstance(aCNFFileName);
				ProblemInstanceSeedPair aProblemInstanceSeedPair = new ProblemInstanceSeedPair(aProblemInstance,aSeed);
				RunConfig aRunConfig = new RunConfig(aProblemInstanceSeedPair, aCutoff, fParamConfigurationSpace.getDefaultConfiguration());
				
				aToSolveInstances.put(aRunConfig,aComponentInstance);
				aComponentEncoders.put(aRunConfig, aComponentEncoder);
				
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
					StationPackingInstance aComponentInstance = aToSolveInstances.get(aRun.getRunConfig());
					CNFCompressor aComponentEncoder = aComponentEncoders.get(aRun.getRunConfig());
					
					//The TAE wrapper is assumed to return a ';'-separated string of litterals, one litteral for each variable of the SAT problem.
					HashMap<Long,Boolean> aLitteralChecker = new HashMap<Long,Boolean>();
					for(String aLiteral : aAdditionalRunData.split(";"))
					{
						boolean aSign = !aLiteral.contains("-"); 
						long aVariable = Long.valueOf(aLiteral.replace("-", ""));
						
						if(aLitteralChecker.containsKey(aVariable))
						{
							log.warn("A variable was present twice in a SAT assignment.");
							if(!aLitteralChecker.get(aVariable).equals(aSign))
							{
								throw new IllegalStateException("SAT assignment from TAE wrapper assigns a variable to true AND false.");
							}
						}
						else
						{
							aLitteralChecker.put(aVariable, aSign);
						}
						
						//If the litteral is positive, then we keep it as it is an assigned station to a channel.
						if(aSign)
						{
							Pair<Station,Integer> aStationChannelPair = fEncoder.decode(aComponentEncoder.decompress(aVariable));
							Station aStation = aStationChannelPair.getKey();
							Integer aChannel = aStationChannelPair.getValue();
							
							if(!aComponentInstance.getStations().contains(aStation) || !aComponentInstance.getChannels().contains(aChannel))
							{
								throw new IllegalStateException("A decoded station and channel from a component SAT assignment is not in that component's problem instance.");
							}
							
							if(!aAssignment.containsKey(aChannel))
							{
								aAssignment.put(aChannel, new HashSet<Station>());
							}
							aAssignment.get(aChannel).add(aStation);
						}
					}
	
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
		SolverResult aResult = SolverHelper.mergeComponentResults(aComponentResults);
		
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
		try {
			fLookup.writeToFile();
		} catch (IOException e) {
			log.error("Could not write result to file ({})",e.getMessage());
		}
		
		log.info("Done.");
		
		return aResult;

	}
	

	@Override
	public void notifyShutdown() {
		if(fTargetAlgorithmEvaluator != null)
		{
			fTargetAlgorithmEvaluator.notifyShutdown();
		}
		
	}
	
	@Override
	public void interrupt() throws UnsupportedOperationException 
	{
		throw new UnsupportedOperationException("TAEBasedSolver does not support pre-emption. (interrupts)");
	}
	
	
}
