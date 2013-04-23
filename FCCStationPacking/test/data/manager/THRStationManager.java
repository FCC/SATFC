package data.manager;

import java.io.IOException;

import ca.ubc.cs.beta.stationpacking.data.manager.HRStationManager;

import junit.framework.TestCase;

public class THRStationManager extends TestCase {

	public void testConstruction()
	{
		try 
		{
			HRStationManager aStationManager = new HRStationManager("/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPacking/experiment_dir/stations.csv");
			aStationManager.getClass();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void testSizes()
	{
		try 
		{
			HRStationManager aStationManager = new HRStationManager("/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPacking/experiment_dir/stations.csv");
			
			System.out.println(aStationManager.getStations().size());
			System.out.println(aStationManager.getFixedStations().size());
			System.out.println(aStationManager.getUnfixedStations().size());
			
			assertTrue(aStationManager.getFixedStations().size()+aStationManager.getUnfixedStations().size()==aStationManager.getStations().size());
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
}
