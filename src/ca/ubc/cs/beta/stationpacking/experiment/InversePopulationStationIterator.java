package ca.ubc.cs.beta.stationpacking.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;

public class InversePopulationStationIterator implements Iterator<Station>{

	private Iterator<Station> fOrderedStationsIterator;
	
	/**
	 * @param aStations - a set of stations to iterate over.
	 * @param aStationPopulation - a map sending a station to its population.
	 * @param aRandomizer - random to use for sampling.
	 * @throws Exception 
	 */
	public InversePopulationStationIterator(Collection<Station> aStationsCollection, HashMap<Station,Integer> aStationPopulation, long aSeed)
	{
		Logger log = LoggerFactory.getLogger(InversePopulationStationIterator.class);
		ArrayList<Station> aStations = new ArrayList<Station>(aStationsCollection);
		Random aRandomizer = new Random(aSeed);
		
		HashMap<Station,Integer> aModifiedStationPop = new HashMap<Station,Integer>();
		
		long aPopulationSum = 0;
		int aStationPop;
		for(Station aStation : aStations) {
			if(!aStationPopulation.containsKey(aStation))
			{
				//throw new IllegalArgumentException("Provided station population contains no population for station "+aStation);
				log.warn("Provided station population contains no population for station "+aStation+", assigning population 0.");
				aStationPop = 0;
			}
			else
			{
				aStationPop = aStationPopulation.get(aStation);
			}
			if(aStationPop < 0) 
			{
				throw new IllegalArgumentException("Station with negative population found.");
			}
			else
			{
				aModifiedStationPop.put(aStation, aStationPop);
				aPopulationSum += aStationPop;
			}
			
		}
		
		ArrayList<Station> aOrderedStations = new ArrayList<Station>();
		while(!aStations.isEmpty()){
			Collections.shuffle(aStations,aRandomizer);
			double aCandidateAggregatePopulation = aRandomizer.nextDouble()*aPopulationSum;
			Iterator<Station> aStationIterator = aStations.iterator();
			while(aStationIterator.hasNext()){
				Station aCandidateStation = aStationIterator.next();
				aStationPop = aModifiedStationPop.get(aCandidateStation);
				if(aStationPop >= aCandidateAggregatePopulation){
					aStationIterator.remove();
					aPopulationSum -= aStationPop;
					aOrderedStations.add(aCandidateStation);
					break;
				}
				aCandidateAggregatePopulation -= aStationPop;
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
