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
package ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder;

import org.apache.commons.math3.util.Pair;

public class SATEncoderUtils {
	
	/*
	 * Szudzik's elegant pairing function (http://szudzik.com/ElegantPairing.pdf)
	 * that acts as a bijection between our station channel pairs and the SAT variables.
	 */
	
	/**
	 * Szudzik's <a href="http://szudzik.com/ElegantPairing.pdf">elegant pairing function</a>.
	 * @param x - an integer.
	 * @param y - an integer.
	 * @return a bijective mapping of (x,y) to a (long) integer z.
	 */
	public static long SzudzikElegantPairing(Integer x, Integer y)
	{
		long X = (long) x;
		long Y = (long) y;
		
		long Z;
		if(X<Y)
		{
			Z= Y*Y+X;
		}
		else
		{
			Z = X*X+X+Y;
		}
		
		return Z;
	}
	/**
	 * Inverse of Szudzik's elegant pairing function.
	 * @param z - an integer.
	 * @return the bijective (inverse) mapping of z to a pair of integers (x,y).
	 */
	public static Pair<Integer,Integer> SzudzikElegantInversePairing(long z)
	{
		long a = (long) (z-Math.pow(Math.floor(Math.sqrt(z)),2));
		long b =(long) Math.floor(Math.sqrt(z));
		
		if(a<b)
		{
			if(a>Integer.MAX_VALUE || a<Integer.MIN_VALUE || b > Integer.MAX_VALUE || b<Integer.MIN_VALUE)
			{
				throw new IllegalArgumentException("Cannot unpair "+z+" to integer pairing components.");
			}
			
			return new Pair<Integer,Integer>((int)a,(int)b);
		}
		else
		{
			if(b>Integer.MAX_VALUE || b<Integer.MIN_VALUE || (a-b) > Integer.MAX_VALUE || (a-b)<Integer.MIN_VALUE)
			{
				throw new IllegalArgumentException("Cannot unpair "+z+" to integer pairing components.");
			}
			
			return new Pair<Integer,Integer>((int)b,(int)(a-b));
		}
		
	}
}
