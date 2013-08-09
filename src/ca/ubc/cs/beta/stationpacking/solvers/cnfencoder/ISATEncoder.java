package ca.ubc.cs.beta.stationpacking.solvers.cnfencoder;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;

/**
 * Encodes a problem instance as a propositional satisfiability problem. 
 * @author afrechet
 */
public interface ISATEncoder {
	

	/**
	 * Encodes a station packing problem instances to a SAT CNF formula.
	 * 
	 * @param aInstance - an instance to encode as a SAT problem.
	 * @return a SAT CNF representation of the problem instance. 
	 */
	public CNF encode(StationPackingInstance aInstance);
	
	
	/**
	 * @param aVariable - a SAT variable.
	 * @return - the station and channel encoded by the given SAT variable.
	 */
	public Pair<Station,Integer> decode(long aVariable);
	

	
	
}
