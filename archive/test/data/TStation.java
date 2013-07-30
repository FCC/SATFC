package data;

import java.util.HashSet;

import ca.ubc.cs.beta.stationpacking.data.Station;
import junit.framework.TestCase;

public class TStation extends TestCase {
	
	public void testEqual()
	{
		int aID = 23613136;
		
		Station aStation1 = new Station(aID, new HashSet<Integer>(),-1);
		Station aStation2 = new Station(aID, new HashSet<Integer>(),-1);
		
		assertTrue(aStation1.equals(aStation2));
	}
	
}
