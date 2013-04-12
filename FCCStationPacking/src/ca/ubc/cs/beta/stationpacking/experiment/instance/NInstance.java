package ca.ubc.cs.beta.stationpacking.experiment.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.execution.Main;


public class NInstance implements IInstance{
	
	private Integer fMinChannel,fMaxChannel;
	private Set<Station> fStations; 
	
	//NA - think about structure of this; in particular, Instance, InstanceEncoder, CNFLookup; can it be simplifies?
	public NInstance(Set<Station> aStations, Integer ... aChannelRange){
		setChannelRange(aChannelRange);
		fStations = aStations;
	}
	
	@Override
	public String toString() {
		return fMinChannel.toString()+"_"+fMaxChannel+"_"+Station.hashStationSet(fStations);
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
	
	public void setChannelRange(Integer ...aChannelRange){
		if(aChannelRange.length>1){ 
			fMinChannel = aChannelRange[1]; 
			fMaxChannel = aChannelRange[0];
		} else if(aChannelRange.length==1){ 
			fMaxChannel = aChannelRange[0]; 
			fMinChannel = Main.defaultMinChannel;
		} else {
			fMinChannel = Main.defaultMinChannel;
			fMaxChannel = Main.defaultMaxChannel;
		}
	}
	
	public Integer[] getChannelRange(){
		Integer[] aRange = new Integer[2];
		aRange[0] = fMaxChannel;
		aRange[1] = fMinChannel;
		return aRange;
	}

	public Set<String> getCNFs(){
		return new HashSet<String>();
	}
	
}
