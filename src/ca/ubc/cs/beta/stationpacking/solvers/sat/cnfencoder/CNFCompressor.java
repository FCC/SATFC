package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;

/**
 * Decorates an SAT encoder. It compresses the SAT formula given by the encoder so that the variables are between 1 and n, where
 * n is the number of variables. Some SAT solvers have this requirement.
 * @author afrechet
 *
 */
public class CNFCompressor{
	private static Logger log = LoggerFactory.getLogger(CNFCompressor.class);
	
	private final HashBiMap<Long,Long> fCompressionMap;
	private long fCompressionMapMax = 1;
	
	public CNFCompressor()
	{
		fCompressionMap = HashBiMap.create();
	}
	
	public CNF compress(CNF aCNF) {
		
		log.info("Compressing CNF.");
		
		if(!aCNF.getVariables().containsAll(fCompressionMap.keySet()))
		{
			throw new IllegalArgumentException("Multiple encodings with the CNF compressor must be done on supersets of variables.");
		}
		
		CNF aCompressedCNF = new CNF();
		
		for(Clause aClause : aCNF)
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
		
		log.info("Done.");
		
		return aCompressedCNF;
	}

	public long decompress(long aVariable) {
		
		BiMap<Long,Long> aInverseCompressionMap = fCompressionMap.inverse();
		if(aInverseCompressionMap.containsKey(aVariable))
		{
			return aInverseCompressionMap.get(aVariable);
		}
		else
		{
			throw new IllegalArgumentException("Cannot decompress variable "+aVariable+", not in the compression map.");
		}
		

	}

	

}
