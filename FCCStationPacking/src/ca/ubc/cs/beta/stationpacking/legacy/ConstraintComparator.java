package ca.ubc.cs.beta.stationpacking.legacy;

import java.util.Comparator;

public class ConstraintComparator implements Comparator<Constraint>{

	/* NA - a way to compare two constraints. First, sort the StationChannelPairs 
	 * (so that constraints with identical pairs in opposite orders are considered equal).
	 * Then, compare them pair by pair.
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Constraint o1, Constraint o2) {
		System.out.println("reaching this block of code");
		StationChannelPairComparator aC = new StationChannelPairComparator();
		StationChannelPair aPair11 = o1.getInterferingPair();
		StationChannelPair aPair12 = o1.getProtectedPair();
		StationChannelPair aTempPair;
		if(aC.compare(aPair11,aPair12)>0){
			aTempPair = aPair11;
			aPair11 = aPair12;
			aPair12 = aTempPair;
		}
		StationChannelPair aPair21 = o2.getInterferingPair();
		StationChannelPair aPair22 = o2.getProtectedPair();
		if(aC.compare(aPair21,aPair22)>0){
			aTempPair = aPair21;
			aPair21 = aPair22;
			aPair22 = aTempPair;
		}
		int aDifference = aC.compare(aPair11, aPair21);
		if(aDifference == 0) aDifference = aC.compare(aPair12, aPair22);
		return aDifference;
	}

}
