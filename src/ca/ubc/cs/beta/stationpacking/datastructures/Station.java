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
	
	private final Set<Integer> fDomain;
	

	public Station(Integer aID, Set<Integer> aDomain){
		fID = aID;
		fDomain = aDomain;
	}

	
	public int getID(){
		return fID;
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
