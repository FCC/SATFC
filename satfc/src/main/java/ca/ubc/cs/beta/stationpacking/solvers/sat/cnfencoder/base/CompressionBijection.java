/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
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
