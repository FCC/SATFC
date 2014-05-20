package ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterionFactory;
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.collect.Sets;

public class StationSubsetSATCertifier implements IStationSubsetCertifier {

	private static Logger log = LoggerFactory.getLogger(StationSubsetSATCertifier.class);
	
	private final ISolver fSolver;
	private final ITerminationCriterionFactory fTerminationCriterionFactory;
	
	public StationSubsetSATCertifier(ISolver aSolver, ITerminationCriterionFactory aTerminationCriterionFactory)
	{
		fSolver = aSolver;
		fTerminationCriterionFactory = aTerminationCriterionFactory;
	}
	
	@Override
	public SolverResult certify(StationPackingInstance aInstance,
			Set<Station> aToPackStations,
			ITerminationCriterion aTerminationCriterion, long aSeed) {
		
		Watch watch = Watch.constructAutoStartWatch();
		
		ITerminationCriterion terminationCriterion = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(fTerminationCriterionFactory.getTerminationCriterion(),aTerminationCriterion));
		
		Map<Station,Integer> previousAssignment = aInstance.getPreviousAssignment();
		
		/*
		 * Try the SAT bound, namely to see if the missing stations plus their neighborhood are packable when all other stations are fixed
		 * to their previous assignment values.
		 */
		//Change station packing instance so that the 'not to pack' stations have reduced domain.
		Map<Station,Set<Integer>> domains = aInstance.getDomains();
		Map<Station,Set<Integer>> reducedDomains = new HashMap<Station,Set<Integer>>();
		
		
		for(Station station : aInstance.getStations())
		{
			Set<Integer> domain = domains.get(station);
			
			if(!aToPackStations.contains(station))
			{
				Integer previousChannel = previousAssignment.get(station);
				if(!domain.contains(previousChannel))
				{
					//One empty domain station, cannot affirm anything.
					log.warn("Station {} in previous assignment is assigned to channel {} not in current domain {} - SAT certifier is indecisive.",station,previousChannel,domain);
					watch.stop();
					double extraTime = watch.getElapsedTime();
					return new SolverResult(SATResult.TIMEOUT, extraTime);
				}
				reducedDomains.put(station,Sets.newHashSet(previousChannel));
			}
			else
			{
				reducedDomains.put(station,domain);
			}
		}
		log.debug("Evaluating if stations not in previous assignment with their neighborhood are packable when all other stations are fixed to previous assignment.");
		
		if(aToPackStations.size()<10)
		{
			log.debug("Missing station and neighborhood: {} .",aToPackStations);
		}
		
		StationPackingInstance SATboundInstance = new StationPackingInstance(reducedDomains, previousAssignment);
		
		if(!aTerminationCriterion.hasToStop())
		{
			
			watch.stop();
			SolverResult SATboundResult = fSolver.solve(SATboundInstance, terminationCriterion, aSeed);
			watch.start();
			
			
			if(SATboundResult.getResult().equals(SATResult.SAT))
			{
				log.debug("Stations not in previous assignment can be packed with their neighborhood when all other stations are fixed to their previous assignment..");
				
				watch.stop();
				double extraTime = watch.getElapsedTime();
				
				return SolverResult.addTime(SATboundResult, extraTime);
			}
			else
			{
				watch.stop();
				double extraTime = watch.getElapsedTime();
				return new SolverResult(SATResult.TIMEOUT, SATboundResult.getRuntime()+extraTime);
			}
		}
		else
		{
			watch.stop();
			double extraTime = watch.getElapsedTime();
			return new SolverResult(SATResult.TIMEOUT, extraTime);
		}
	}

	@Override
	public void notifyShutdown() {
		log.warn("Not shutting down associated solver as it may be used elsewhere.");
	}

	@Override
	public void interrupt() throws UnsupportedOperationException {
		fSolver.interrupt();
	}

}
