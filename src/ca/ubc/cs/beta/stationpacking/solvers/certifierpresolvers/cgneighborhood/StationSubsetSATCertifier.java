package ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

public class StationSubsetSATCertifier implements IStationSubsetCertifier {

	private static Logger log = LoggerFactory.getLogger(StationSubsetSATCertifier.class);
	
	private final ISolver fSolver;
	private final double fMaxCutoff;
	
	public StationSubsetSATCertifier(ISolver aSolver, double aMaxCutoff)
	{
		fSolver = aSolver;
		fMaxCutoff = aMaxCutoff;
	}
	
	@Override
	public SolverResult certify(StationPackingInstance aInstance,
			Set<Station> aMissingStations,
			double aCutoff, long aSeed) {
		
		AutoStartStopWatch interWatch = new AutoStartStopWatch();
		
		HashMap<Station,Integer> previousAssignment = aInstance.getPreviousAssignment();
		
		/*
		 * Try the SAT bound, namely to see if the missing stations plus their neighborhood are packable when all other stations are fixed
		 * to their previous assignment values.
		 */
		//Change station packing instance so that the 'not to pack' stations have reduced domain.
		Set<Station> reducedDomainStations = new HashSet<Station>();
		for(Station station : aInstance.getStations())
		{
			if(!aMissingStations.contains(station))
			{
				reducedDomainStations.add(new Station(station.getID(),Sets.newHashSet(previousAssignment.get(station))));
			}
			else
			{
				reducedDomainStations.add(station);
			}
		}
		log.debug("Evaluating if stations not in previous assignment ({}) with their neighborhood are packable when all other stations are fixed to previous assignment.",reducedDomainStations.size());
		StationPackingInstance SATboundInstance = new StationPackingInstance(reducedDomainStations, aInstance.getChannels(), previousAssignment);
		
		double aTimeSpent = interWatch.stop()/1000.0;
		if(aCutoff-aTimeSpent>0)
		{
			SolverResult SATboundResult = fSolver.solve(SATboundInstance, Math.min(fMaxCutoff, aCutoff-aTimeSpent), aSeed);
			
			aTimeSpent += SATboundResult.getRuntime();
			
			if(SATboundResult.getResult().equals(SATResult.SAT))
			{
				log.debug("Stations not in previous assignment can be packed with their neighborhood when all other stations are fixed to their previous assignment..");
				return new SolverResult(SATResult.SAT,aTimeSpent,SATboundResult.getAssignment());
			}
			else
			{
				return new SolverResult(SATResult.TIMEOUT, aTimeSpent);
			}
		}
		else
		{
			return new SolverResult(SATResult.TIMEOUT, aTimeSpent);
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
