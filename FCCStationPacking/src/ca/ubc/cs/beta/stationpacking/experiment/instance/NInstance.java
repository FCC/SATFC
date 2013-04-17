package ca.ubc.cs.beta.stationpacking.experiment.instance;


import java.util.HashSet;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;


public class NInstance implements IInstance{
	
	private final Set<Integer> fChannels = new HashSet<Integer>();
	private final Set<Station> fStations = new HashSet<Station>(); 
	
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
