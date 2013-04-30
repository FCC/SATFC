package ca.ubc.cs.beta.stationpacking.legacy;

import java.util.Comparator;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.StationComparator;

public class StationChannelPairComparator implements Comparator<StationChannelPair>{

	//NA compares pairs first by stations, then by channels
	public int compare(StationChannelPair aPair1, StationChannelPair aPair2) {
		System.out.println("Reached StationChannelPairComparator.compare()");;
		Station aStation1 = aPair1.getStation();
		Station aStation2 = aPair2.getStation();
		int aDifference = new StationComparator().compare(aStation1,aStation2);
		if(aDifference==0) aDifference = Integer.compare(aPair1.getChannel(), aPair2.getChannel());
		return(aDifference);
	}

}
