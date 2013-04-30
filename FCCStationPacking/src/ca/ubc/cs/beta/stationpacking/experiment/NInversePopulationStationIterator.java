package ca.ubc.cs.beta.stationpacking.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import ca.ubc.cs.beta.stationpacking.data.Station;

public class NInversePopulationStationIterator implements Iterator<Station>{

	private Iterator<Station> fOrderedStationsIterator;
	
	/**
	 * @param aStations - a set of stations to iterate over.
	 * @param aStationPopulation - a map sending a station to its population.
	 * @param aRandomizer - random to use for sampling.
	 * @throws Exception 
	 */
	public NInversePopulationStationIterator(Collection<Station> aStationsCollection, long aSeed) throws Exception
	{
		ArrayList<Station> aStations = new ArrayList<Station>(aStationsCollection);
		Random aRandomizer = new Random(aSeed);		
		long aPopulationSum = 0;
		int aStationPop;
		for(Station aStation : aStations) {
			aStationPop = aStation.getPop();
			if(aStationPop < 0) throw new Exception("Station with negative population found.");
			else aPopulationSum += aStationPop;
		}
		
		ArrayList<Station> aOrderedStations = new ArrayList<Station>();
		while(!aStations.isEmpty()){
			Collections.shuffle(aStations,aRandomizer);
			double aCandidateAggregatePopulation = aRandomizer.nextDouble()*aPopulationSum;
			Iterator<Station> aStationIterator = aStations.iterator();
			while(aStationIterator.hasNext()){
				Station aCandidateStation = aStationIterator.next();
				aStationPop = aCandidateStation.getPop();
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
