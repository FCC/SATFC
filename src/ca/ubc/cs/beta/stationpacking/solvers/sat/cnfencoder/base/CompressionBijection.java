package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.base;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Compression bijection that maps any number to [n] where n is the number of values seen so far.
 * @author afrechet
 * @param <X> - domain of bijection.
 */
public class CompressionBijection<X extends Number> implements IBijection<X, Long> {

	private final HashBiMap<X,Long> fCompressionMap;
	private long fCompressionMapMax = 1;
	
	public CompressionBijection()
	{
		fCompressionMap = HashBiMap.create();
	}
	
	@Override
	public Long map(X aDomainElement) {
		
		if(!fCompressionMap.containsKey(aDomainElement))
		{
			fCompressionMap.put(aDomainElement, fCompressionMapMax);
			return fCompressionMapMax++;
		}
		else
		{
			return fCompressionMap.get(aDomainElement);
		}
	}

	@Override
	public X inversemap(Long aImageElement) {
		
		BiMap<Long,X> aInverseCompressionMap = fCompressionMap.inverse();
		
		if(aInverseCompressionMap.containsKey(aImageElement))
		{
			return aInverseCompressionMap.get(aImageElement);
		}
		else
		{
			throw new IllegalArgumentException("Compression map does not contain an domain element for "+aImageElement);
		}
	}



}
