package data.manager;

import java.io.IOException;
import java.util.HashSet;

import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.manager.HRConstraintManager;
import ca.ubc.cs.beta.stationpacking.data.manager.IConstraintManager;


import junit.framework.TestCase;

public class THRConstraintManager extends TestCase {
	
	public void testPairwiseConstraintStations() throws IOException
	{
		IConstraintManager aConstraintManager = new HRConstraintManager("/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPacking/experiment_dir/AllowedChannels.csv", "/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPacking/experiment_dir/PairwiseConstraints.csv");
		
		HashSet<Station> aStations = new HashSet<Station>();
		HashSet<Integer> aStationIDs = new HashSet<Integer>();
	
		for(Constraint aConstraint : aConstraintManager.getPairwiseConstraints(new HashSet<Integer>()))
		{
			
			aStations.add(aConstraint.getInterferingPair().getStation());
			aStations.add(aConstraint.getProtectedPair().getStation());
			
			aStationIDs.add(aConstraint.getInterferingPair().getStation().getID());
			aStationIDs.add(aConstraint.getProtectedPair().getStation().getID());
			
		}

		System.out.println(aStations.size());
		System.out.println(aStationIDs.size());
		assertTrue(aStations.size()==aStationIDs.size());
	}
	
}
