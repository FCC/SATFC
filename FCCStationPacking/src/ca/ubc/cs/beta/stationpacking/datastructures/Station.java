package ca.ubc.cs.beta.stationpacking.datastructures;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;


/**
 * Container class for the station object.
 * Uniquely identified by its integer ID.
 * @author afrechet
 *
 */
public class Station implements Comparable<Station>, Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Integer fID;
	private Integer fPop;
	private boolean fSetPop;
	
	private final Set<Integer> fDomain;
	

	public Station(Integer aID, Set<Integer> aDomain, Integer aPop){
		fID = aID;
		fPop = aPop;
		fSetPop = true;
		fDomain = aDomain;
	}
	
	public Station(Integer aID, Set<Integer> aDomain)
	{
		fID = aID;
		fSetPop = false;
		fDomain = aDomain;
	}
	
	public int getID(){
		return fID;
	}
	
	public int getPop(){
		return fPop;
	}
	
	/**
	 * Set the station's population to the given population. May only be done once.
	 * @param aPop - the value for the station's population.
	 */
	public void setPop(Integer aPop)
	{
		if(!fSetPop)
		{
			fPop = aPop;
			fSetPop = true;
		}
		else
		{
			throw new UnsupportedOperationException("Cannot set the population of a station more than once");
		}
	}
	
	
	@Override
	public String toString(){
		return "Station "+fID;
	}
	
	public Set<Integer> getDomain(){
		return new HashSet<Integer>(fDomain);
	}
	
	public boolean removeFromDomain(Integer aChannel){
		return fDomain.remove(aChannel);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Station))
		{
			return false;
		}
		else
		{
			Station aStation = (Station) o;
			return fID.equals(aStation.fID);
		}
	}
	
	@Override
	public int hashCode(){
		return fID.hashCode();
	}
	
	public static String hashStationSet(Collection<Station> aStations)
	{
		LinkedList<Station> aStationsList = new LinkedList<Station>(aStations);
		Collections.sort(aStationsList, new StationComparator());
		
		String aResult = "";
		Iterator<Station> aStationIterator = aStationsList.iterator();
		while(aStationIterator.hasNext()){
			Station aStation = aStationIterator.next();
			aResult += aStation.getID();
			if(aStationIterator.hasNext())
			{
				aResult+="-";
			}
		}
		return aResult;	
	}

	@Override
	public int compareTo(Station o) {
		return fID - o.getID();
	}

}
