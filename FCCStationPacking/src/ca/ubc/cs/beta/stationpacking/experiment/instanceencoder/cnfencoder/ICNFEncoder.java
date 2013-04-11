package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;


/**
 * Encodes set of station in DAC CNF format. 
 * All constraints are given in constructor.
 * @author afrechet
 *
 */
public interface ICNFEncoder {

	/**
	 * 
	 * @param aStations - a set of stations to encode.
	 * @return A DAC CNF encoding for that set of stations and encoder's relevant constraints. 
	 */
	public String encode(Set<Station> aStations);
	
	public boolean write(Set<Station> aStations,String aFileName);
	
}
