package ca.ubc.cs.beta.stationpacking.experiment.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.execution.Main;


public class NInstance implements IInstance{
	
	private SortedSet<Integer> fChannels = new TreeSet<Integer>();
	private SortedSet<Station> fStations = new TreeSet<Station>(); 
	
	//NA - think about structure of this; in particular, Instance, InstanceEncoder, CNFLookup; can it be simplifies?
	public NInstance(Set<Station> aStations, Set<Integer> aChannels){
		for(Integer aChannel : aChannels) fChannels.add(aChannel);
		for(Station aStation : aStations) fStations.add(aStation);
	}
	
	@Override
	public String toString() {
		return fChannels.toString()+"_"+Station.hashStationSet(fStations);
	}

	@Override
	public int getNumberofStations() {
		return fStations.size();
	}
	
	public boolean addStation(Station aStation){
		return fStations.add(aStation);
	}
	
	public boolean removeStation(Station aStation){
		return fStations.remove(aStation);
	}
	
	public Set<Station> getStations(){
		return fStations;
	}
	
	public void setChannelRange(Set<Integer> aChannels){
		fChannels.clear();
		for(Integer aChannel : aChannels) fChannels.add(aChannel);
	}
	
	public Set<Integer> getChannelRange(){
		return fChannels;
	}


}
