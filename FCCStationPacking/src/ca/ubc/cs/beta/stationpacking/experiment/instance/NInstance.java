package ca.ubc.cs.beta.stationpacking.experiment.instance;


import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;
//import ca.ubc.cs.beta.stationpacking.data.StationComparator;


public class NInstance implements IInstance{
	
	private final Set<Integer> fChannels = new HashSet<Integer>();
	private final Set<Station> fStations = new HashSet<Station>(); 
	
	public NInstance(Set<Station> aStations, Set<Integer> aChannels){
		for(Integer aChannel : aChannels) fChannels.add(aChannel);
		for(Station aStation : aStations) fStations.add(aStation);
	}
	
	//AF - Added a different way to print set of channels so that an Instance.toString() is easier to read in CSV.
	private String hashChannelSet(Set<Integer> aChannels)
	{
		LinkedList<Integer> aChannelList = new LinkedList<Integer>(aChannels);
		Collections.sort(aChannelList);
		
		String aResult = "";
		Iterator<Integer> aChannelIterator = aChannelList.iterator();
		while(aChannelIterator.hasNext()){
			Integer aChannel = aChannelIterator.next();
			aResult += aChannel.toString();
			if(aChannelIterator.hasNext())
			{
				aResult+="-";
			}
		}
		return aResult;	
	}
	
	@Override
	public String toString() {
		return hashChannelSet(fChannels)+"_"+Station.hashStationSet(fStations);
	}

	@Override
	public int getNumberofStations() {
		return fStations.size();
	}
	
	@Override
	public boolean addStation(Station aStation){
		return fStations.add(aStation);
	}
	
	@Override
	public boolean removeStation(Station aStation){
		return fStations.remove(aStation);
	}
	
	@Override
	public Set<Station> getStations(){
		return fStations;
	}
	
	@Override
	public Set<Integer> getChannelRange(){
		return fChannels;
	}


}
