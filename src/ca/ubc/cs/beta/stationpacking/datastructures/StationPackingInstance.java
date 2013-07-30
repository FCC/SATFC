package ca.ubc.cs.beta.stationpacking.datastructures;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;


public class StationPackingInstance {
	
	private HashMap<Integer,Station> fStations = new HashMap<Integer,Station>();

	private final Set<Integer> fChannels;
	
	public StationPackingInstance(){
		fChannels = new HashSet<Integer>();
	}
	
	public StationPackingInstance(Set<Station> aStations, Set<Integer> aChannels){
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
	
	/**
	 * @param aInstanceString - a string representation of the instance (should be the result of calling toString() on the instance).
	 * @param aStationManager - the station manager to pull stations from.
	 * @return the instance represented by the string. 
	 */
	public static StationPackingInstance valueOf(String aInstanceString, IStationManager aStationManager)
	{
		String aChannelString = aInstanceString.split("_")[0];
		String aStationString = aInstanceString.split("_")[1];
	
		HashSet<Integer> aInstanceChannels = new HashSet<Integer>();
		for(String aChannel : aChannelString.split("-"))
		{
			aInstanceChannels.add(Integer.valueOf(aChannel));
		}
		
		HashSet<Station> aInstanceStations = new HashSet<Station>();
		
		String[] aInstanceStationIDs = aStationString.split("-");
		
		for(String aStationID : aInstanceStationIDs)
		{
			aInstanceStations.add(aStationManager.getStationfromID(Integer.valueOf(aStationID)));
		}
		
		if(aInstanceStations.size()!= aInstanceStationIDs.length)
		{
			
			throw new IllegalStateException("Couldn't identify all stations from the instance's string representation");
		}
		
		return new StationPackingInstance(aInstanceStations, aInstanceChannels);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fChannels == null) ? 0 : fChannels.hashCode());
		result = prime * result
				+ ((fStations == null) ? 0 : fStations.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StationPackingInstance other = (StationPackingInstance) obj;
		if (fChannels == null) {
			if (other.fChannels != null)
				return false;
		} else if (!fChannels.equals(other.fChannels))
			return false;
		if (fStations == null) {
			if (other.fStations != null)
				return false;
		} else if (!fStations.equals(other.fStations))
			return false;
		return true;
	}
	
	public int getNumberofStations() {
		return fStations.size();
	}
	
	public boolean addStation(Station aStation){
		return (fStations.put(aStation.getID(),aStation)==null);
	}
	
	public boolean removeStation(Station aStation){
		return (fStations.remove(aStation.getID())!=null);
	}
	
	public HashSet<Station> getStations(){
		return new HashSet<Station>(fStations.values());
	}
	
	public HashSet<Integer> getChannels(){
		return new HashSet<Integer>(fChannels);
	}
	
	public Station getStation(Integer aID){
		return fStations.get(aID);
	}
	
	public String getInfo()
	{
		return fStations.size()+" stations to pack into "+fChannels.size()+" channels";
	}


}
