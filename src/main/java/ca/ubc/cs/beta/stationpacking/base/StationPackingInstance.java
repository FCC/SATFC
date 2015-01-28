/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.base;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Immutable container class representing a station packing instance.
 * @author afrechet
 */
public class StationPackingInstance {
	
	private final ImmutableMap<Station, Set<Integer>> fDomains;
	private final ImmutableMap<Station, Integer> fPreviousAssignment;
	@Getter
	private final String name;
	
	// names are only for tracking metrics, so use this string when you don't care
	public final static String UNTITLED = "UNTITLED";

	/**
	 * Create a station packing instance.
	 * @param aDomains - a map taking each station to its domain of packable channels.
	 */
	public StationPackingInstance(Map<Station,Set<Integer>> aDomains, String name){
		this(aDomains, ImmutableMap.of(), name);
	}
	
	/**
	 * Create a station packing instance.
	 * @param aDomains - a map taking each station to its domain of packable channels.
	 * @param aPreviousAssignment - a map taking stations to the channels they were assigned to previously.
	 */
	public StationPackingInstance(Map<Station,Set<Integer>> aDomains, Map<Station,Integer> aPreviousAssignment, String name){
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
		
		// TODO: this isn't ideal. You could just sort this once at the beginning and then never again...
		Map<Station, Set<Integer>> domains = Maps.newLinkedHashMap();
		aDomains.keySet().stream().sorted().forEach(station -> {
			List<Integer> channels = Lists.newArrayList(aDomains.get(station));
			Collections.sort(channels);
			domains.put(station, Sets.newLinkedHashSet(channels));
		});
		this.name = name;
		fDomains = ImmutableMap.copyOf(domains);
		fPreviousAssignment = ImmutableMap.copyOf(aPreviousAssignment);
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
		return new StationPackingInstance(domains,aPreviousAssignment, StationPackingInstance.UNTITLED);
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

	// warning: changing this method will completely mess up hashing!
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int s=1;
		for(Station station : getStations())
		{
			sb.append(station+":"+StringUtils.join(fDomains.get(station),","));
			
			if(s+1<=getStations().size())
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
		return fDomains.keySet();
	}
	
	/**
	 * An instance's channels is an unmodifiable set backed up by a hash set.
	 * @return - get the problem instance's channels.
	 */
	public ImmutableMap<Station,Set<Integer>> getDomains(){
		return fDomains;
	}
	
	/**
	 * @return a map taking stations to the (valid) channels they were assigned to previously (if any).
	 */
	public ImmutableMap<Station,Integer> getPreviousAssignment()
	{
		return fPreviousAssignment;
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
	
	public BitSet toBitSet() {
		final BitSet bitSet = new BitSet();
		getStations().forEach(station -> bitSet.set(station.getID()));
		return bitSet;
	}


}
