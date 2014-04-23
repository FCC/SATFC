package ca.ubc.cs.beta.stationpacking.base;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

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
	
	/**
	 * Construct a station.
	 * @param aID - the station ID.
	 */
	public Station(Integer aID){
		fID = aID;
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
	 * ID hashing.
	 */
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		if (fID != other.fID)
			return false;
		return true;
	}

	/**
	 * ID & domain hashing
	 */
	
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
