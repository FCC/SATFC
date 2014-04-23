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

/**
 * Checks if a given neighborhood of instances cannot be packed together.
 * @author afrechet
 *
 */
public class StationSubsetUNSATCertifier implements IStationSubsetCertifier {

	private static Logger log = LoggerFactory.getLogger(StationSubsetUNSATCertifier.class);
	
	private final ISolver fSolver;
	private final ITerminationCriterionFactory fTerminationCriterionFactory;
	
	public StationSubsetUNSATCertifier(ISolver aSolver, ITerminationCriterionFactory aTerminationCriterionFactory)
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
		
		log.debug("Evaluating if stations not in previous assignment ({}) with their neighborhood are unpackable.",aToPackStations.size());
		
		Map<Station,Set<Integer>> domains = aInstance.getDomains();
		Map<Station,Set<Integer>> toPackDomains = new HashMap<Station,Set<Integer>>();
		for(Station station : aToPackStations)
		{
			toPackDomains.put(station, domains.get(station));
		}
		
		StationPackingInstance UNSATboundInstance = new StationPackingInstance(toPackDomains, aInstance.getPreviousAssignment());
		
		watch.stop();
		SolverResult UNSATboundResult = fSolver.solve(UNSATboundInstance, terminationCriterion, aSeed);
		watch.start();
		
		if(UNSATboundResult.getResult().equals(SATResult.UNSAT))
		{	
			log.debug("Stations not in previous assignment cannot be packed with their neighborhood.");
			
			watch.stop();
			double extraTime = watch.getEllapsedTime();
			return new SolverResult(SATResult.UNSAT,UNSATboundResult.getRuntime()+extraTime);
		}
		else
		{
			watch.stop();
			double extraTime = watch.getEllapsedTime();
			return new SolverResult(SATResult.TIMEOUT, UNSATboundResult.getRuntime()+extraTime);
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
