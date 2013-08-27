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
