package data.manager;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.data.Station;

import junit.framework.TestCase;

public class TPair extends TestCase {

	public void testStationPair()
	{
		Station S1 = new Station(1);
		Station S2 = new Station(2);
		Station S3 = new Station(1);
		
		Pair<Station,Station> P1 = new Pair<Station,Station>(S1,S2);
		//Pair<Station,Station> P2 = new Pair<Station,Station>(S2,S1); //NA - Commented out due to never being used
		
		Pair<Station,Station> P3 = new Pair<Station,Station>(S3,S2);
		
		assertTrue(P1.equals(P3));
		
		
		
		
	}
	
}
