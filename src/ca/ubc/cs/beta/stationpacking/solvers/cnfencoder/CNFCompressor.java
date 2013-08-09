package ca.ubc.cs.beta.stationpacking.solvers.cnfencoder;

import org.apache.commons.math3.util.Pair;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;

/**
 * Decorates an SAT encoder. It compresses the SAT formula given by the encoder so that the variables are between 1 and n, where
 * n is the number of variables. Some SAT solvers have this requirement.
 * @author afrechet
 *
 */
public class CNFCompressor implements ISATEncoder {

	private final ISATEncoder fSATEncoder;
	
	private final HashBiMap<Long,Long> fCompressionMap;
	private long fCompressionMapMax = 1;
	
	public CNFCompressor(ISATEncoder aSATEncoder)
	{
		fSATEncoder = aSATEncoder;
		fCompressionMap = HashBiMap.create();
	}
	
	@Override
	public CNF encode(StationPackingInstance aInstance) {
		
		CNF aEncodedCNF = fSATEncoder.encode(aInstance);
		
		if(!aEncodedCNF.getVariables().containsAll(fCompressionMap.keySet()))
		{
			throw new IllegalArgumentException("Multiple encodings with the CNF compressor must be done on supersets of variables.");
		}
		
		CNF aCompressedCNF = new CNF();
		
		for(Clause aClause : aEncodedCNF)
		{
			Clause aCompressedClause = new Clause();
			
			for(Litteral aLitteral : aClause)
			{
				long aVariable = aLitteral.getVariable();
				
				if(!fCompressionMap.containsKey(aVariable))
				{
					fCompressionMap.put(aVariable, fCompressionMapMax++);
				}
				
				long aCompressedVariable = fCompressionMap.get(aVariable);
				
				aCompressedClause.add(new Litteral(aCompressedVariable,aLitteral.getSign()));
				
			}
			
			aCompressedCNF.add(aCompressedClause);
		}
		
		return aCompressedCNF;
	}

	@Override
	public Pair<Station, Integer> decode(long aVariable) {
		
		BiMap<Long,Long> aInverseCompressionMap = fCompressionMap.inverse();
		if(aInverseCompressionMap.containsKey(aVariable))
		{
			return fSATEncoder.decode(aInverseCompressionMap.get(aVariable));
		}
		else
		{
			throw new IllegalArgumentException("Cannot uncompress variable "+aVariable+", not in the compression map.");
		}
		

	}

	

}
