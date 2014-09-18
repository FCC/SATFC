package ca.ubc.cs.beta.stationpacking.solvers.certifierpresolvers.cgneighborhood;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Certifies if a station subset is packable or unpackable. Usually can only answer SAT or UNSAT cases exclusively.
 * @author afrechet
 */
public interface IStationSubsetCertifier {
    
    /**
     * Certifies if a station subset is packable or unpackable.
     * @param aInstance
     * @param aToPackStations
     * @param aTerminationCriterion
     * @param aSeed
     * @return
     */
	public SolverResult certify(
			StationPackingInstance aInstance,
			Set<Station> aToPackStations,
			ITerminationCriterion aTerminationCriterion,
			long aSeed);
	
	/**
	 * Tries to stop the solve call if implemented, if not throws an UnsupportedOperationException.
	 * @throws UnsupportedOperationException thrown if interruption is not supported.
	 */
	public void interrupt() throws UnsupportedOperationException;
	
	/**
	 * Ask the solver to shutdown.
	 */
	public void notifyShutdown();
	
}
