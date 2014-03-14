package ca.ubc.cs.beta.stationpacking.base;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Immutable container class for the station object.
 * Uniquely identified by its integer ID.
 * Also contains the station's domain (channels it can be on).
 * @author afrechet
 */
public class Station implements Comparable<Station>, Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final int fID;
	private Set<Integer> fDomain;
	
	/**
	 * Construct a station.
	 * @param aID - the station ID.
	 * @param aDomain - the station's domain (channels it can be assigned to).
	 */
	public Station(Integer aID, Set<Integer> aDomain){
		fID = aID;
		fDomain = new HashSet<Integer>(aDomain);
	}

	/**
	 * @return - the station ID.
	 */
	public int getID(){
		return fID;
	}
	
	
	@Override
	public String toString(){
		return "Station "+fID;
	}
	
	/**
	 * A station's domain is an unmodifiable set backed up by a hash set.
	 * @return - get the station's domain.
	 */
	public Set<Integer> getDomain(){
		return Collections.unmodifiableSet(fDomain);
	}
	
	/**
	 * Reduce the domain of a station.
	 * @param aReducedDomain - a set of integer channel 
	 */
	public void reduceDomain(Set<Integer> aReducedDomain)
	{
		if(!fDomain.containsAll(aReducedDomain))
		{
			throw new IllegalArgumentException("Cannot reduce domain of station "+this.toString()+" to "+aReducedDomain+" since original domain is "+fDomain);
		}
		fDomain = aReducedDomain;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o == this)
		{
			return true;
		}
		else if (!(o instanceof Station))
		{
			return false;
		}
		else
		{
			Station aStation = (Station) o;
			return fID == aStation.fID;
		}
	}
	
	@Override
	public int hashCode(){
		return fID;
	}
	
	/**
	 * Returns a unique, non-optimized string representing the given station set.
	 * Specifically, returns the "-"-separated list of sorted station IDs. 
	 * @param aStations - a station set to hash.
	 * @return - a string hash for the station set.
	 */
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
		return Integer.compare(fID,o.fID);
	}

}
