package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder;

import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;


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
	
	//NA - the CNFEncoder should store station constraints internally
	
	//NA - Returns a String corresponding to the DIMACS cnf format of the problem
	public String encode(IInstance aInstance, IConstraintManager aConstraintManager);
	
	//public boolean writeCNF(Set<Station> aStations, Set<Integer> aChannels, String aFileName);
	
	
	public Map<Station,Integer> decode(IInstance aInstance, String aCNFAssignment);
	
	
	
}
