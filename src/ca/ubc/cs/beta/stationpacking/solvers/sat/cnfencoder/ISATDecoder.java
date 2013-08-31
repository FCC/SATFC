package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.base.Station;

public interface ISATDecoder {
	
	/**
	 * @param aVariable - a SAT variable.
	 * @return - the station and channel encoded by the given SAT variable.
	 */
	public Pair<Station,Integer> decode(long aVariable);
	
}
