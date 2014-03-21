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
	private final Set<Integer> fDomain;
	
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
		return Integer.toString(fID);
	}
	
	/**
	 * A station's domain is an unmodifiable set backed up by a hash set.
	 * @return - get the station's domain.
	 */
	public Set<Integer> getDomain(){
		return Collections.unmodifiableSet(fDomain);
	}
	
	/**
	 * @param aReducedDomain - a set of integer channel
	 * @return a station with the reduced domain. 
	 */
	public Station getReducedDomainStation(Set<Integer> aReducedDomain)
	{
		if(!fDomain.containsAll(aReducedDomain))
		{
			throw new IllegalArgumentException("Cannot reduce domain of station "+this.toString()+" to "+aReducedDomain+" since original domain is "+fDomain);
		}
		return new Station(fID,aReducedDomain);
	}

//	/* (non-Javadoc)
//	 * @see java.lang.Object#hashCode()
//	 */
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + fID;
//		return result;
//	}
//
//	/* (non-Javadoc)
//	 * @see java.lang.Object#equals(java.lang.Object)
//	 */
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		Station other = (Station) obj;
//		if (fID != other.fID)
//			return false;
//		return true;
//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fDomain == null) ? 0 : fDomain.hashCode());
		result = prime * result + fID;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Station other = (Station) obj;
		if (fDomain == null) {
			if (other.fDomain != null)
				return false;
		} else if (!fDomain.equals(other.fDomain))
			return false;
		if (fID != other.fID)
			return false;
		return true;
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
		Collections.sort(aStationsList);
		
		StringBuilder builder = new StringBuilder();
		Iterator<Station> aStationIterator = aStationsList.iterator();
		while(aStationIterator.hasNext()){
			Station aStation = aStationIterator.next();
			builder.append(aStation.toString());
			if(aStationIterator.hasNext())
			{
				builder.append("-");
			}
		}
		return builder.toString();	
	}

	@Override
	public int compareTo(Station o) {
		return Integer.compare(fID,o.fID);
	}

}
