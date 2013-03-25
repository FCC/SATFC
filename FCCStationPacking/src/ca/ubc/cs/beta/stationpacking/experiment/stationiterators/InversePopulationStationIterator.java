package ca.ubc.cs.beta.stationpacking.experiment.stationiterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import ca.ubc.cs.beta.stationpacking.data.Station;


/**
 * Station iterators that returns given set of stations as if they were sampled according to the inverse of their population (i.e.
 * the most populous stations are sampled first).
 * @author afrechet
 *
 */
public class InversePopulationStationIterator implements Iterator<Station>{

	private Iterator<Station> fOrderedStationsIterator;
	
	/**
	 * @param aStations - a set of stations to iterate over.
	 * @param aStationPopulation - a map sending a station to its population.
	 * @param aRandomizer - random to use for sampling.
	 * @throws Exception 
	 */
	public InversePopulationStationIterator(Collection<Station> aStations, Map<Station,Integer> aStationPopulation, Random aRandomizer) throws Exception
	{
	
		if(!aStationPopulation.keySet().containsAll(aStations))
		{
			throw new Exception("Given populations for station iterator do not cover all given stations.");
		}
		
		aStations = new LinkedList<Station>(aStations);
		
		int aPopulationSum = 0;
		for(Station aStation : aStations)
		{
			aPopulationSum += aStationPopulation.get(aStation);
		}
		
		ArrayList<Station> aOrderedStations = new ArrayList<Station>();
		while(!aStations.isEmpty())
		{
			double aCandidateAggregatePopulation = aRandomizer.nextDouble()*aPopulationSum;
			
			Iterator<Station> aStationIterator = aStations.iterator();
			while(aStationIterator.hasNext())
			{
				Station aCandidateStation = aStationIterator.next();
				if(aStationPopulation.get(aCandidateStation) >= aCandidateAggregatePopulation)
				{
					aStationIterator.remove();
					aPopulationSum -= aStationPopulation.get(aCandidateStation);
					aOrderedStations.add(aCandidateStation);
					break;
				}
				aCandidateAggregatePopulation -= aStationPopulation.get(aCandidateStation);
			}		
		}
		
		fOrderedStationsIterator = aOrderedStations.iterator();
		
	}
	
	@Override
	public boolean hasNext() {
		return fOrderedStationsIterator.hasNext();
	}

	@Override
	public Station next() {
		return fOrderedStationsIterator.next();
	}

	@Override
	public void remove() {
		fOrderedStationsIterator.remove();
	}
	

}
