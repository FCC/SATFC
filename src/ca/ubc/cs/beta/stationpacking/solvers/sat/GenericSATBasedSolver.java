package ca.ubc.cs.beta.stationpacking.solvers.sat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import ca.ubc.cs.beta.stationpacking.utils.Watch;

public class GenericSATBasedSolver implements ISolver {
	
	private static Logger log = LoggerFactory.getLogger(GenericSATBasedSolver.class);
	
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
		
		Watch watch = Watch.constructAutoStartWatch();
		
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
			
			if(aTerminationCriterion.hasToStop())
			{
				log.debug("All time spent.");
				break;
			}
			
			log.debug("Solving the subproblem CNF with "+aTerminationCriterion.getRemainingTime()+" s remaining.");
			watch.stop();
			SATSolverResult aComponentResult = fSATSolver.solve(aCNF, aTerminationCriterion, aSeed);
			watch.start();
			
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
		
		watch.stop();
		double extraTime = watch.getEllapsedTime();
		aResult = SolverResult.addTime(aResult, extraTime);
		
		log.debug("Result:");
		log.debug(aResult.toParsableString());
		
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
