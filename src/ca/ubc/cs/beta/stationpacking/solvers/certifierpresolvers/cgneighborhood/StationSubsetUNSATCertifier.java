package ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Checks if a given neighborhood of instances cannot be packed together.
 * @author afrechet
 *
 */
public class StationSubsetUNSATCertifier implements IStationSubsetCertifier {

	private static Logger log = LoggerFactory.getLogger(StationSubsetUNSATCertifier.class);
	
	private final ISolver fSolver;
	private final ITerminationCriterion fTerminationCriterion;
	
	public StationSubsetUNSATCertifier(ISolver aSolver, ITerminationCriterion aTerminationCriterion)
	{
		fSolver = aSolver;
		fTerminationCriterion = aTerminationCriterion;
	}
	
	@Override
	public SolverResult certify(StationPackingInstance aInstance,
			Set<Station> aMissingStations,
			ITerminationCriterion aTerminationCriterion, long aSeed) {
		
		ITerminationCriterion terminationCriterion = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(fTerminationCriterion,aTerminationCriterion));
		
		log.debug("Evaluating if stations not in previous assignment ({}) with their neighborhood are unpackable.",aMissingStations.size());
		StationPackingInstance UNSATboundInstance = new StationPackingInstance(aMissingStations, aInstance.getChannels(), aInstance.getPreviousAssignment());
		
		SolverResult UNSATboundResult = fSolver.solve(UNSATboundInstance, terminationCriterion, aSeed);
		
		if(UNSATboundResult.getResult().equals(SATResult.UNSAT))
		{
			log.debug("Stations not in previous assignment cannot be packed with their neighborhood.");
			return new SolverResult(SATResult.UNSAT,UNSATboundResult.getRuntime());
		}
		else
		{
			return new SolverResult(SATResult.TIMEOUT, UNSATboundResult.getRuntime());
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
