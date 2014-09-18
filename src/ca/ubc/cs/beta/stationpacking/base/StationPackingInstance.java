package ca.ubc.cs.beta.stationpacking.base;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Immutable container class representing a station packing instance.
 * @author afrechet
 */
public class StationPackingInstance {
	
	private final Map<Station,Set<Integer>> fDomains;
	private final HashMap<Station,Integer> fPreviousAssignment;
	
	/**
	 * Create a station packing instance.
	 * @param aDomains - a map taking each station to its domain of packable channels.
	 */
	public StationPackingInstance(Map<Station,Set<Integer>> aDomains){
		this(aDomains,new HashMap<Station,Integer>());
	}
	
	/**
	 * Create a station packing instance.
	 * @param aDomains - a map taking each station to its domain of packable channels.
	 * @param aPreviousAssignment - a map taking stations to the channels they were assigned to previously.
	 */
	public StationPackingInstance(Map<Station,Set<Integer>> aDomains, Map<Station,Integer> aPreviousAssignment){
		
		//Validate assignment domain.
		for(Station station : aDomains.keySet())
		{
			Integer previousChannel = aPreviousAssignment.get(station);
			if(previousChannel != null && !aDomains.get(station).contains(previousChannel))
			{
				throw new IllegalArgumentException("Provided previous assignment assigned channel "+previousChannel+" to station "+station+" which is not in its problem domain "+aDomains.get(station)+".");
			}
			
			if(aDomains.get(station).isEmpty())
			{
				throw new IllegalArgumentException("Domain for station "+station+" is empty.");
			}
		}
		
		fDomains = Collections.unmodifiableMap(new HashMap<Station,Set<Integer>>(aDomains));
		fPreviousAssignment = new HashMap<Station,Integer>(aPreviousAssignment);
	}
	
	/**
	 * @param aStations - set of stations to pack.
	 * @param aChannels - set of channels to pack into.
	 * @param aPreviousAssignment - valid previous assignment for the stations on the channels.
	 * @return a station packing instance consisting of packing all the given stations in the given channels.
	 */
	public static StationPackingInstance constructUniformDomainInstance(Set<Station> aStations, Set<Integer> aChannels, Map<Station,Integer> aPreviousAssignment)
	{
		Map<Station,Set<Integer>> domains = new HashMap<Station,Set<Integer>>();
		for(Station station : aStations)
		{
			domains.put(station, aChannels);
		}
		return new StationPackingInstance(domains,aPreviousAssignment);
	}
	
	/**
	 * @return - all the channels present in the domains.
	 */
	public Set<Integer> getAllChannels()
	{
		Set<Integer> allChannels = new HashSet<Integer>();
		for(Set<Integer> channels : fDomains.values())
		{
			allChannels.addAll(channels);
		}
		return allChannels;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		LinkedList<Station> stations = new LinkedList<Station>(fDomains.keySet());
		Collections.sort(stations);
		int s=1;
		for(Station station : stations)
		{
			LinkedList<Integer> channels = new LinkedList<Integer>(fDomains.get(station));
			Collections.sort(channels);
			
			sb.append(station+":"+StringUtils.join(channels,","));
			
			if(s+1<=stations.size())
			{
				sb.append(";");
			}
			
			s++;
			
		}
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fDomains == null) ? 0 : fDomains.hashCode());
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
		if (fDomains == null) {
			if (other.fDomains != null)
				return false;
		} else if (!fDomains.equals(other.fDomains))
			return false;
		return true;
	}

	/**
	 * An instance's stations is an unmodifiable set backed up by a hash set.
	 * @return - get the problem instance's stations.
	 */
	public Set<Station> getStations(){
		return Collections.unmodifiableSet(fDomains.keySet());
	}
	
	/**
	 * An instance's channels is an unmodifiable set backed up by a hash set.
	 * @return - get the problem instance's channels.
	 */
	public Map<Station,Set<Integer>> getDomains(){
		return Collections.unmodifiableMap(fDomains);
	}
	
	/**
	 * @return a map taking stations to the (valid) channels they were assigned to previously (if any).
	 */
	public Map<Station,Integer> getPreviousAssignment()
	{
		return Collections.unmodifiableMap(fPreviousAssignment);
	}
	
	/**
	 * @return an information string about the instance.
	 */
	public String getInfo()
	{
		return fDomains.keySet().size()+" stations, "+getAllChannels().size()+" all channels.";
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
