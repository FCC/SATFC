package ca.ubc.cs.beta.stationpacking.solver.cnfencoder;

import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solver.sat.Clause_old;

/**
 * Encodes set of station in DIMACS CNF format. 
 * Decodes DIMACS CNF format to a set of station assignments.
 * @author afrechet, narnosti
 *
 */
public interface ICNFEncoder_old {
	
	/* NA - takes an Instance and a set of Constraints, and returns
	 * the DIMACS CNF corresponding to this instance of SAT.
	 */	
	public Set<Clause_old> encode(StationPackingInstance aInstance, IConstraintManager aConstraintManager);
	
	/* NA - takes an Instance and a string corresponding to a satisfying variable assignment.
	 * Checks that each station is assigned exactly one channel, and that this channel is in its domain.
	 * If these conditions are not met, it throws and catches an exception describing the problem, and 
	 * returns an empty map.
	 */
	public Map<Integer,Set<Station>> decode(StationPackingInstance aInstance, Clause_old aAssignment);

}