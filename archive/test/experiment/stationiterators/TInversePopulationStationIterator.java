package experiment.stationiterators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.experiment.stationiterators.InversePopulationStationIterator;




import junit.framework.TestCase;

public class TInversePopulationStationIterator extends TestCase {
	
	public void testSize() throws Exception
	{
		int aSize = 100000;
		int aMaxPopulation = 100000000;
		int aSeed = 1;
		
		Random aRandomizer = new Random(aSeed);
		
		LinkedList<Station> aStations = new LinkedList<Station>();
		HashMap<Station,Integer> aStationPopulation = new HashMap<Station,Integer>();
		for(int j=0;j<aSize;j++)
		{
			int aPopulation = aRandomizer.nextInt(aMaxPopulation);
			Station aStation = new Station(j, new HashSet<Integer>(),-1);
			aStations.add(aStation);
			aStationPopulation.put(aStation, aPopulation);
		}
		
		InversePopulationStationIterator aIterator = new InversePopulationStationIterator(aStations,aStationPopulation, aRandomizer);
		
		int n=0;
		while(aIterator.hasNext())
		{
			n++;
			aIterator.next();
		}
		
		assertTrue(n==aSize);
		assertTrue(n==aStations.size());
		
		
	}
	
	public void testDistribution() throws Exception
	{
		int aSize = 5;
		int aPrecision = 10000;
		int aMaxPopulation = 100;
		int aSeed = 1;
		
		Random aRandomizer = new Random(aSeed);

		double[] aAveragePopulation = new double[aSize];
		
		for(int i=0;i<aPrecision;i++)
		{
			LinkedList<Station> aStations = new LinkedList<Station>();
			HashMap<Station,Integer> aStationPopulation = new HashMap<Station,Integer>();
			for(int j=0;j<aSize;j++)
			{
				int aPopulation = aRandomizer.nextInt(aMaxPopulation);
				Station aStation = new Station(j, new HashSet<Integer>(),-1);
				aStations.add(aStation);
				aStationPopulation.put(aStation, aPopulation);
			}
	
			InversePopulationStationIterator aIterator = new InversePopulationStationIterator(aStations,aStationPopulation, aRandomizer);
			
			int j=0;
			while(aIterator.hasNext())
			{
				Station aStation = aIterator.next();
				
				if(i==0)
				{
					aAveragePopulation[j] = (double) aStationPopulation.get(aStation);
				}
				else
				{
					Double aPrevAverage = aAveragePopulation[j];
					aAveragePopulation[j] = (aPrevAverage*(i-1)+(double) aStationPopulation.get(aStation))/i;
				}
				j++;
			}
		}
		
		System.out.println(Arrays.toString(aAveragePopulation));
		
		boolean aSorted = true;
		for(int j=0;j<aAveragePopulation.length-1;j++)
		{
			if(aAveragePopulation[j]<aAveragePopulation[j+1])
			{
				aSorted = false;
				break;
			}
		}
		
		assertTrue(aSorted);
	}
	
}
