/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;
import lombok.Getter;
import lombok.NonNull;

/**
 * Immutable container class representing a station packing instance.
 * @author afrechet
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonDeserialize(using = StationPackingInstanceDeserializer.class)
public class StationPackingInstance {
	
	public static final String NAME_KEY = "NAME";
    public static final String CACHE_DATE_KEY = "CACHE_DATE";
    public static final String UNTITLED = "UNTITLED";
    private final ImmutableMap<Station, Set<Integer>> domains;
	private final ImmutableMap<Station, Integer> previousAssignment;
	@Getter
	private final ConcurrentMap<String, Object> metadata;

	/**
	 * Create a station packing instance.
	 * @param aDomains - a map taking each station to its domain of packable channels.
	 */
	public StationPackingInstance(Map<Station,Set<Integer>> aDomains){
		this(aDomains, ImmutableMap.of());
	}

    public StationPackingInstance(Map<Station,Set<Integer>> aDomains, Map<Station,Integer> aPreviousAssignment) {
        this(aDomains, aPreviousAssignment, ImmutableMap.of());
    }
	
	/**
	 * Create a station packing instance.
	 * @param aDomains - a map taking each station to its domain of packable channels.
	 * @param aPreviousAssignment - a map taking stations to the channels they were assigned to previously.
	 */
	public StationPackingInstance(Map<Station,Set<Integer>> aDomains, Map<Station,Integer> aPreviousAssignment, @NonNull Map<String, Object> metadata){
		this.metadata = new ConcurrentHashMap<>(metadata);
		// Remove any previous assignment info for stations that aren't present
		previousAssignment = aPreviousAssignment.entrySet().stream().filter(entry -> aDomains.keySet().contains(entry.getKey())).collect(GuavaCollectors.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
		
		//Validate assignment domain.
		for(Station station : aDomains.keySet())
		{
			if(aDomains.get(station).isEmpty())
			{
				throw new IllegalArgumentException("Domain for station "+station+" is empty.");
			}
		}

        // sort everything
		final Map<Station, Set<Integer>> tempDomains = new LinkedHashMap<>();
		aDomains.keySet().stream().sorted().forEach(station -> tempDomains.put(station, ImmutableSet.copyOf(aDomains.get(station).stream().sorted().iterator())));
		this.domains = ImmutableMap.copyOf(tempDomains);
	}

    /**
	 * @return - all the channels present in the domains.
	 */
	public Set<Integer> getAllChannels()
    {
		Set<Integer> allChannels = new HashSet<>();
		for(Set<Integer> channels : domains.values())
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
			sb.append(station).append(":").append(StringUtils.join(domains.get(station), ","));
			
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
				+ ((domains == null) ? 0 : domains.hashCode());
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
		if (domains == null) {
			if (other.domains != null)
				return false;
		} else if (!domains.equals(other.domains))
			return false;
		return true;
	}

	/**
	 * An instance's stations is an unmodifiable set backed up by a hash set.
	 * @return - get the problem instance's stations.
	 */
	public ImmutableSet<Station> getStations(){
		return domains.keySet();
	}
	
	/**
	 * An instance's channels is an unmodifiable set backed up by a hash set.
	 * @return - get the problem instance's channels.
	 */
	public ImmutableMap<Station,Set<Integer>> getDomains(){
		return domains;
	}
	
	/**
	 * @return a map taking stations to the (valid) channels they were assigned to previously (if any).
	 */
	public ImmutableMap<Station,Integer> getPreviousAssignment()
	{
		return previousAssignment;
	}
	
	/**
	 * @return an information string about the instance.
	 */
	public String getInfo()
	{
		return domains.keySet().size()+" stations, "+getAllChannels().size()+" all channels";
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
            return new String(Hex.encodeHex(aResult));
		}
		catch (UnsupportedEncodingException e) {
		    throw new IllegalStateException("Could not encode filename", e);
		}
	}
	
	public String getName() {
		return (String) metadata.getOrDefault(NAME_KEY, UNTITLED);
	}

    public boolean hasName() {
        return !getName().equals(UNTITLED);
    }

    public String getAuction() {
        final String name = (String) metadata.get(NAME_KEY);
        return StationPackingUtils.parseAuctionFromName(name);
    }
}
