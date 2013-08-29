package ca.ubc.cs.beta.stationpacking.base;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Immutable container class representing a station packing instance.
 * @author afrechet
 */
public class StationPackingInstance {
	
	private final Set<Station> fStations;
	private final Set<Integer> fChannels;
	
	/**
	 * Create a station packing instance.
	 * @param aStations - set of stations to pack.
	 * @param aChannels - set of channels to pack in.
	 */
	public StationPackingInstance(Set<Station> aStations, Set<Integer> aChannels){
		
		fChannels = Collections.unmodifiableSet(new HashSet<Integer>(aChannels));
		fStations = Collections.unmodifiableSet(new HashSet<Station>(aStations));
	}
	
	//AF - Added a different way to print set of channels so that an Instance.toString() is easier to read in CSV.
	/**
	 * Returns a unique, non-optimized string representing the given channel set.
	 * Specifically, returns the "-"-separated list of sorted channels.
	 * @param aChannels - a channel set to hash.
	 * @return - a string hash for the station set.
	 */
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
		return hashChannelSet(fChannels)+"_"+Station.hashStationSet(fStations);
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
	
	/**
	 * An instance's stations is an unmodifiable set backed up by a hash set.
	 * @return - get the problem instance's stations.
	 */
	public Set<Station> getStations(){
		return fStations;
	}
	
	/**
	 * An instance's channels is an unmodifiable set backed up by a hash set.
	 * @return - get the problem instance's channels.
	 */
	public Set<Integer> getChannels(){
		return fChannels;
	}
	
	/**
	 * @return an information string about the instance.
	 */
	public String getInfo()
	{
		return fStations.size()+" stations to pack into "+fChannels.size()+" channels";
	}
	
	/**
	 * @return a hashed version of the instance's string representation.
	 */
	public String getHashString()
	{
		String aString = this.toString();
		MessageDigest aDigest = DigestUtils.getSha1Digest();
		try {
			byte[] aResult = aDigest.digest(aString.getBytes("UTF-8"));
		    String aResultString = new String(Hex.encodeHex(aResult));	
		    return aResultString;
		}
		catch (UnsupportedEncodingException e) {
		    throw new IllegalStateException("Could not encode filename", e);
		}
	}


}
