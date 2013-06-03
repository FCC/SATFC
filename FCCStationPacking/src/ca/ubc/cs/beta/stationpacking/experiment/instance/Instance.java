package ca.ubc.cs.beta.stationpacking.experiment.instance;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;

public class Instance implements IInstance{
	
	private HashMap<Integer,Station> fStations = new HashMap<Integer,Station>();

	
	private final Set<Integer> fChannels;
	//private final Set<Station> fStations = new HashSet<Station>(); 
	
	public Instance(Set<Station> aStations, Set<Integer> aChannels){
		fChannels = new HashSet<Integer>(aChannels);
		for(Station aStation : aStations) fStations.put(aStation.getID(),aStation);
	}
	
	//AF - Added a different way to print set of channels so that an Instance.toString() is easier to read in CSV.
	public static String hashChannelSet(Set<Integer> aChannels)
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
		return hashChannelSet(fChannels)+"_"+Station.hashStationSet(fStations.values());
	}

	@Override
	public int getNumberofStations() {
		return fStations.size();
	}
	
	@Override
	public boolean addStation(Station aStation){
		return (fStations.put(aStation.getID(),aStation)==null);
	}
	
	@Override
	public boolean removeStation(Station aStation){
		return (fStations.remove(aStation)!=null);
	}
	
	@Override
	public Set<Station> getStations(){
		return new HashSet<Station>(fStations.values());
	}
	
	@Override
	public Set<Integer> getChannelRange(){
		return new HashSet<Integer>(fChannels);
	}
	
	@Override
	public Station getStation(Integer aID){
		return fStations.get(aID);
	}


}
