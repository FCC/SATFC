package ca.ubc.cs.beta.stationpacking.solvers.tae;

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
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.SolverHelper;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.tae.cnflookup.AsyncCachedCNFLookup;
import ca.ubc.cs.beta.stationpacking.solvers.tae.cnflookup.ICNFResultLookup;
import ca.ubc.cs.beta.stationpacking.solvers.tae.reporters.IExperimentReporter;

/**
 * SAT solver wrapper that uses Steve Ramage's AClib Target Algorithm Evaluators for execution in an asynchronous way, particularly useful for parallel solving of many instances..
 * 
 * @author afrechet
 * 
 */
public class AsyncTAEBasedSolver {
	
	private static Logger log = LoggerFactory.getLogger(TAEBasedSolver.class);
	
	private TargetAlgorithmEvaluator fTAE;
	private AlgorithmExecutionConfig fExecConfig;
	private ParamConfiguration fParamConfig;
	
	private IComponentGrouper fGrouper;
	private IConstraintManager fManager;
	private ISATEncoder fEncoder;
	
	private AsyncCachedCNFLookup fLookup;
	
	/**
	 * 
	 * @param aConstraintManager - the manager in charge of constraints.
	 * @param aCNFEncoder - the encoder in charge of taking constraints and an instance and producing a CNF clause set.
	 * @param aLookup - the CNF lookup in charge of monitoring CNFs.
	 * @param aGrouper - the component grouper in charge of partitioning instance in subinstances.
	 * @param aTargetAlgorithmEvaluator - target algorithm evaluator to use.
	 * @param aExecutionConfig - the execution config for the algorithm we want to execute.
	 * @param aParamConfig - the parameter configuration for the algorithm we want to execute.
	 */
	public AsyncTAEBasedSolver(IConstraintManager aConstraintManager,
			ISATEncoder aCNFEncoder,
			ICNFResultLookup aLookup,
			IComponentGrouper aGrouper,
			TargetAlgorithmEvaluator aTargetAlgorithmEvaluator,
			AlgorithmExecutionConfig aExecutionConfig,
			ParamConfiguration aParamConfig) {
		
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
		
		fTAE = aTargetAlgorithmEvaluator;
		fParamConfig  = aParamConfig;
		fExecConfig = aExecutionConfig;
		
	}


	private TargetAlgorithmEvaluatorCallback getCompilingCallback(final StationPackingInstance aInstance,final IExperimentReporter aAsynchronousReporter,final HashMap<RunConfig,StationPackingInstance> aToSolveInstances, final HashMap<RunConfig,ISATDecoder> aComponentDecoders)
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
							StationPackingInstance aComponentInstance = aToSolveInstances.get(aRun.getRunConfig());
							ISATDecoder aSATDecoder = aComponentDecoders.get(aRun.getRunConfig());
							
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
									Pair<Station,Integer> aStationChannelPair = aSATDecoder.decode(aVariable);
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
					
					//Add result to component results
					aComponentResults.add(aSolverResult);
				}
				
				if(aComponentResults.size()!=aToSolveInstances.size())
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
					if(aAssignmentSize!=aInstance.getStations().size())
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
				
				
				
				aAsynchronousReporter.report(aInstance, aResult);
				
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
		HashMap<RunConfig,ISATDecoder> aComponentDecoders = new HashMap<RunConfig,ISATDecoder>();	
		
		//Create the runs to execute.
		for(Set<Station> aStationComponent : aInstanceGroups){
			
			//Create the component group instance.
			StationPackingInstance aComponentInstance = new StationPackingInstance(aStationComponent,aChannelRange);
			
			//Not present, CNF must be solved.
			//Name the instance
			String aCNFFileName = fLookup.getCNFNameFor(aComponentInstance);
			
			File aCNFFile = new File(aCNFFileName);
			
			
			//Encode the instance
			Pair<CNF,ISATDecoder> aEncoding = fEncoder.encode(aInstance);
			CNF aCNF = aEncoding.getKey();
			ISATDecoder aDecoder = aEncoding.getValue();
			
			String aCNFString = aCNF.toDIMACS(new String[]{"FCC Station packing instance.","[Channels]_[Stations] ",aComponentInstance.toString()});
			
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
			RunConfig aRunConfig = new RunConfig(aProblemInstanceSeedPair, aCutoff, fParamConfig,fExecConfig);
			
			aToSolveInstances.put(aRunConfig,aComponentInstance);
			aComponentDecoders.put(aRunConfig, aDecoder);
		
		}
		
		//Execute the runs
		List<RunConfig> aRunConfigs = new ArrayList<RunConfig>(aToSolveInstances.keySet());
		
		//We are not providing any preempting observer when doing async runs as we do not want to kill (possibly) shared instances.
		fTAE.evaluateRunsAsync(aRunConfigs,getCompilingCallback(aInstance,aAsynchronousReporter,aToSolveInstances,aComponentDecoders));
		
	}
	
	
	public void waitForFinish()
	{
		fTAE.waitForOutstandingEvaluations();
		notifyShutdown();
	}
	
	
	public void notifyShutdown() {
		if(fTAE != null)
		{
			fTAE.notifyShutdown();
		}
		
	}
		
}
