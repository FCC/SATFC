package ca.ubc.cs.beta.stationpacking.solvers.sat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.SolverHelper;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.IComponentGrouper;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.ISATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

public class GenericSATBasedSolver implements ISolver {
	
	private static Logger log = LoggerFactory.getLogger(GenericSATBasedSolver.class);
	
	private final static boolean SAVE_CNF = false;
	private final static String CNF_DIR = "/ubc/cs/home/a/afrechet/arrow-space/OrcinusMountPoint/FCCFeasibilityCheckingInstances/UHF_21-08-13";
	
	
	private final IConstraintManager fConstraintManager;
	private final IComponentGrouper fComponentGrouper;
	private final ISATEncoder fSATEncoder;
	private final ISATSolver fSATSolver;
	
	protected GenericSATBasedSolver(ISATSolver aSATSolver, ISATEncoder aSATEncoder, IConstraintManager aConstraintManager, IComponentGrouper aComponentGrouper)
	{
		fConstraintManager = aConstraintManager;
		fComponentGrouper = aComponentGrouper;
		fSATEncoder = aSATEncoder;
		fSATSolver = aSATSolver;
	}
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, ITerminationCriterion aTerminationCriterion,
			long aSeed) {
		AutoStartStopWatch aSolveWatch = new AutoStartStopWatch();
		
		log.debug("Solving instance of {}...",aInstance.getInfo());

		Set<Integer> aChannelRange = aInstance.getChannels();
		
		HashSet<SolverResult> aComponentResults = new HashSet<SolverResult>();
		
		Set<Set<Station>> aStationComponents = fComponentGrouper.group(aInstance,fConstraintManager);
		log.debug("Problem separated in {} groups.",aStationComponents.size());
		
		for(Set<Station> aStationComponent : aStationComponents)
		{
			StationPackingInstance aComponentInstance = new StationPackingInstance(aStationComponent,aChannelRange);
			
			log.debug("Encoding subproblem in CNF.");
			Pair<CNF,ISATDecoder> aEncoding = fSATEncoder.encode(aInstance);
			CNF aCNF = aEncoding.getKey();
			ISATDecoder aDecoder = aEncoding.getValue();
			log.debug("CNF has {} clauses.",aCNF.size());
			
			//Check if CNF should be saved.
			if(SAVE_CNF)
			{
				log.debug("Trying to save CNF.");
				File aCNFFile = new File(CNF_DIR+File.separator+RandomStringUtils.randomAlphanumeric(25)+".cnf");
				while(aCNFFile.exists())
				{
					new File(CNF_DIR+File.separator+RandomStringUtils.randomAlphanumeric(25));
				}
				try {
					FileUtils.writeStringToFile(aCNFFile, aCNF.toDIMACS(null));
				} catch (IOException e) {
					log.error("Could not write CNF to file ({}).",e.getMessage());
				}
			}
			
			if(aTerminationCriterion.hasToStop())
			{
				break;
			}
			
			log.debug("Solving the subproblem CNF with "+aTerminationCriterion.getRemainingTime()+" s remaining.");
			SATSolverResult aComponentResult = fSATSolver.solve(aCNF, aTerminationCriterion, aSeed);
			
			log.debug("Parsing result.");
			Map<Integer,Set<Station>> aStationAssignment = new HashMap<Integer,Set<Station>>();
			if(aComponentResult.getResult().equals(SATResult.SAT))
			{
				HashMap<Long,Boolean> aLitteralChecker = new HashMap<Long,Boolean>();
				for(Literal aLiteral : aComponentResult.getAssignment())
				{
					boolean aSign = aLiteral.getSign(); 
					long aVariable = aLiteral.getVariable();
					
					//Do some quick verifications of the assignment.
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
						Pair<Station,Integer> aStationChannelPair = aDecoder.decode(aVariable);
						Station aStation = aStationChannelPair.getKey();
						Integer aChannel = aStationChannelPair.getValue();
						
						if(!aComponentInstance.getStations().contains(aStation) || !aComponentInstance.getChannels().contains(aChannel))
						{
							throw new IllegalStateException("A decoded station and channel from a component SAT assignment is not in that component's problem instance. ("+aStation+", channel:"+aChannel+")");
						}
						
						if(!aStationAssignment.containsKey(aChannel))
						{
							aStationAssignment.put(aChannel, new HashSet<Station>());
						}
						aStationAssignment.get(aChannel).add(aStation);
					}
				}
			}
			
			
			aComponentResults.add(new SolverResult(aComponentResult.getResult(),aComponentResult.getRuntime(),aStationAssignment));
			
			if(aComponentResult.equals(SATResult.UNSAT) || aTerminationCriterion.hasToStop())
			{
				break;
			}
		}
		
		SolverResult aResult = SolverHelper.mergeComponentResults(aComponentResults);
		
		log.debug("...done.");
		log.debug("Cleaning up...");
		
		//Post-process the result for correctness.
		if(aResult.getResult().equals(SATResult.SAT))
		{
			
			log.debug("Independently verifying the veracity of returned assignment");
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
			if(!fConstraintManager.isSatisfyingAssignment(aResult.getAssignment())){
				
				log.error("Bad assignment:");
				for(Integer aChannel : aResult.getAssignment().keySet())
				{
					log.error(aChannel+','+aResult.getAssignment().get(aChannel).toString());
				}
				
				throw new IllegalStateException("Merged station assignment violates some pairwise interference constraint.");
			}
			else
			{
				log.debug("Assignment was independently verified to be satisfiable.");
			}
		}
		log.debug("...done.");
		
		log.debug("Result : {}",aResult);
		log.debug("Total walltime taken "+aSolveWatch.stop()/1000.0+" seconds");
		
		return aResult;
	}

	@Override
	public void notifyShutdown() {
		fSATSolver.notifyShutdown();
	}


	@Override
	public void interrupt() throws UnsupportedOperationException {
		fSATSolver.interrupt();
	}

	

}
