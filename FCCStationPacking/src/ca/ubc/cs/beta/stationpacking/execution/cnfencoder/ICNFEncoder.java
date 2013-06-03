package ca.ubc.cs.beta.stationpacking.execution.cnfencoder;

import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;

/**
 * Encodes set of station in DIMACS CNF format. 
 * Decodes DIMACS CNF format to a set of station assignments.
 * @author afrechet, narnosti
 *
 */
public interface ICNFEncoder {
	
	/* NA - takes an Instance and a set of Constraints, and returns
	 * the DIMACS CNF corresponding to this instance of SAT.
	 */
	public String encode(Instance aInstance, IConstraintManager aConstraintManager);
	
	//public Set<Clause> encode(IInstance aInstance, IConstraintManager aConstraintManager);
	
	/* NA - takes an Instance and a string corresponding to a satisfying variable assignment.
	 * Checks that each station is assigned exactly one channel, and that this channel is in its domain.
	 * If these conditions are not met, it throws and catches an exception describing the problem, and 
	 * returns an empty map.
	 */
	public Map<Integer,Set<Station>> decode(Instance aInstance, String aCNFAssignment);
	
	//public Map<Integer,Set<Station>> decode(IInstance aInstance, Set<Clause> aAssignment);

	
}
