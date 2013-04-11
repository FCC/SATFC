package ca.ubc.cs.beta.stationpacking.data;

import org.apache.commons.math3.util.Pair;

/**
 * Container class for a pairwise station packing constraint. 
 * Both protected and interfering station-channel pairs cannot occur at the same time.
 * @author afrechet
 *
 */
public class Constraint {
	
	StationChannelPair fProtected;
	StationChannelPair fInterfering;
	
	public Constraint(StationChannelPair aProtected, StationChannelPair aInterfering)
	{
		fProtected = aProtected;
		fInterfering = aInterfering;
	}
	
	public StationChannelPair getProtectedPair()
	{
		return fProtected;
	}
	
	public StationChannelPair getInterferingPair()
	{
		return fInterfering;
	}
	
	@Override
	public String toString()
	{
		return "{"+fProtected.toString()+" "+fInterfering.toString()+"}";
	}
	
	@Override
	public int hashCode(){
    	int aConstraint1Hash = fProtected != null ? fProtected.hashCode() : 0;
    	int aConstraint2Hash = fInterfering != null ? fInterfering.hashCode() : 0;
    	return aConstraint1Hash+aConstraint2Hash;
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof Constraint))
		{
			return false;
		}
		else
		{
			Constraint a = (Constraint) o;
			return 	(fProtected.equals(a.getProtectedPair())&&fInterfering.equals(a.getInterferingPair()))||
					(fInterfering.equals(a.getProtectedPair())&&fProtected.equals(a.getInterferingPair()));
		}
	}

}
