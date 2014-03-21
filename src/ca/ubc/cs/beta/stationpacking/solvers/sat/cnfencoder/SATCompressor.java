package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base.CompressionBijection;

/**
 * Encodes a problem instance as a propositional satisfiability problem.
 * Insures that the SAT variables are contiguous from 1 to n. 
 * A variable of the SAT encoding is a station channel pair, each constraint is trivially
 * encoded as a clause (this station cannot be on this channel when this other station is on this other channel is a two clause with the previous
 * SAT variables), and base clauses are added (each station much be on exactly one channel).
 * 
 * @author afrechet
 */
public class SATCompressor implements ISATEncoder {
	
	private final IConstraintManager fConstraintManager;
	
	public SATCompressor(IConstraintManager aConstraintManager)
	{
		fConstraintManager = aConstraintManager;
	}

	@Override
	public Pair<CNF, ISATDecoder> encode(StationPackingInstance aInstance) {
		
		SATEncoder aSATEncoder = new SATEncoder(fConstraintManager,new CompressionBijection<Long>());
		
		Pair<CNF,ISATDecoder> aEncoding = aSATEncoder.encode(aInstance);
		
		return aEncoding;
		
	}
	
	
	

	
}
