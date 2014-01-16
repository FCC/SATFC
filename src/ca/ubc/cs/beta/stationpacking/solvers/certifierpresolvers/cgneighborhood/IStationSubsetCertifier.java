package ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Certifies if a station subset is packable or unpackable.
 * @author afrechet
 *
 */
public interface IStationSubsetCertifier {

	public SolverResult certify(
			StationPackingInstance aInstance,
			Set<Station> aMissingStations,
			ITerminationCriterion aTerminationCriterion,
			long aSeed);
	
	/**
	 * Tries to stop the solve call if implemented, if not throws an UnsupportedOperationException.
	 */
	public void interrupt() throws UnsupportedOperationException;
	
	/**
	 * Ask the solver to shutdown.
	 */
	public void notifyShutdown();
	
}
