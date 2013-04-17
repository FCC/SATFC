package ca.ubc.cs.beta.stationpacking.experiment.instance;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;


public class Instance implements IInstance{
	

	private HashMap<HashSet<Station>,String> fStationComponentstoCNF;
	
	public Instance(Map<Set<Station>,String> aStationComponentstoCNF, Integer ... aRange)
	{
		fStationComponentstoCNF = new HashMap<HashSet<Station>,String>();
		for(Set<Station> aStationComponent : aStationComponentstoCNF.keySet()) {
			fStationComponentstoCNF.put(new HashSet<Station>(aStationComponent), aStationComponentstoCNF.get(aStationComponent));
		}
	}
	
	/*
	public ArrayList<String> getCNFs() {
		return new ArrayList<String>(fStationComponentstoCNF.values());
	}
	
	@Override
	public String toString()
	{
		Set<Station> aStations = new HashSet<Station>();
		for(HashSet<Station> aStationComponent : fStationComponentstoCNF.keySet())
		{
			aStations.addAll(aStationComponent);
		}
		return fLow.toString()+"_"+fHigh+"_"+Station.hashStationSet(aStations);
	}
	*/

	@Override
	public int getNumberofStations() {
		int aNumberofStations = 0;
		for(HashSet<Station> aStationComponent : fStationComponentstoCNF.keySet())
		{
			aNumberofStations += aStationComponent.size();
		}
		return aNumberofStations;
	}
	
	public Set<Station> getStations(){
		Set<Station> aStationSet = new HashSet<Station>();
		for(Set<Station> aStations : fStationComponentstoCNF.keySet()){
			aStationSet.addAll(aStations);
		}
		return aStationSet;
	}

	
	public boolean addStation(Station aStation){
		try{
			throw new Exception("Method addStation not implemented for class Instance.");
		} catch(Exception e){
			System.out.println(e.getMessage());
		}
		return false;
	}
	
	public boolean removeStation(Station aStation){
		try{
			throw new Exception("Method removeStation not implemented for class Instance.");
		} catch(Exception e){
			System.out.println(e.getMessage());
		}
		return false;
	}
	
	public void setChannelRange(Set<Integer> aChannels){
		try{
			throw new Exception("Method setChannelRange not implemented for class Instance.");
		} catch(Exception e){
			System.out.println(e.getMessage());
		}
	}
	
	public Set<Integer> getChannelRange(){
		try{
			throw new Exception("Method getChannelRange not implemented for class Instance.");
		} catch(Exception e){
			System.out.println(e.getMessage());
		}
		return null;
	}
}
