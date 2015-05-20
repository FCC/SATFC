/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.datamanagers.stations;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.base.Station;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;

/**
 * In charge of managing collections of stations read from a domain file.
 * @author afrechet
 */
public class DomainStationManager implements IStationManager{

	private final Map<Integer,Station> fStations = new HashMap<Integer,Station>();
	private final Map<Station,Set<Integer>> fDomains = new HashMap<Station,Set<Integer>>();
    private final String fHash;
	
	/**
	 * @param aStationDomainsFilename - domain file from which stations should be read.
	 * @throws FileNotFoundException - if domain file is not found.
	 */
	public DomainStationManager(String aStationDomainsFilename) throws FileNotFoundException{
	
		CSVReader aReader;
		aReader = new CSVReader(new FileReader(aStationDomainsFilename),',');
		String[] aLine;
		Integer aID,aChannel;
		String aString;
		Set<Integer> aChannelDomain;
		try
		{
			while((aLine = aReader.readNext())!=null){	
				aChannelDomain = new HashSet<Integer>();
				for(int i=2;i<aLine.length;i++){ 	//NA - Ideally would have a more robust check here
					aString = aLine[i].replaceAll("\\s", "");
					if(aString.length()>0){
						aChannel = Integer.valueOf(aString); 
						aChannelDomain.add(aChannel);
					}
				}
				aID = Integer.valueOf(aLine[1].replaceAll("\\s", ""));
				if(aChannelDomain.isEmpty()){
					try{
						throw new IllegalStateException("Station "+aID+" has empty domain.");
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				Station station = new Station(aID);
				fStations.put(aID, station);
				fDomains.put(station,aChannelDomain);
			}
			aReader.close();	
		}
		catch(IOException e)
		{
			throw new IllegalStateException("There was an exception while reading the station domains file ("+e.getMessage()+").");
		}

        final HashFunction hf = Hashing.murmur3_32();
        final HashCode hc = hf.newHasher()
                .putObject(fDomains, new Funnel<Map<Station, Set<Integer>>>() {
                    @Override
                    public void funnel(Map<Station, Set<Integer>> from, PrimitiveSink into) {
                        from.keySet().stream().sorted().forEach(s -> {
                            into.putInt(s.getID());
                            from.get(s).stream().sorted().forEach(into::putInt);
                        });
                    }
                })
                .hash();
        fHash = hc.toString();
	}
	
	@Override
	public Set<Station> getStations() {
		return new HashSet<Station>(fStations.values());
	}
	
	@Override
    public Station getStationfromID(Integer aID){
		
		if(!fStations.containsKey(aID))
		{
			throw new IllegalArgumentException("Station manager does not contain station for ID "+aID);
		}
		
		return fStations.get(aID);
	}

	@Override
	public Set<Station> getStationsfromID(Collection<Integer> aIDs) {
		Set<Station> stations = new HashSet<Station>();
		for(Integer aID : aIDs)
		{
			stations.add(getStationfromID(aID));
		}
		return stations;
	}

	@Override
	public Set<Integer> getDomain(Station aStation) {
		Set<Integer> domain = fDomains.get(aStation);
		if(domain == null)
		{
			throw new IllegalArgumentException("No domain contained for station "+aStation);
		}
		return domain;
	}
	
	/**
	 * @param stationIDs - all stationIDs
	 * @return a mapping between stationIDs and domains 
	 */
	public Map<Integer, Set<Integer>> getDomainsFromIDs(Set<Integer> stationIDs) {
		Map<Integer, Set<Integer>> domainsFromID = new HashMap<Integer, Set<Integer>>();
		Iterator<Integer> stationIterator = stationIDs.iterator();
		while(stationIterator.hasNext())
		{
			int stationID = stationIterator.next();
			Set<Integer> domain = this.getDomain(getStationfromID(stationID));
			domainsFromID.put(stationID, domain);
		}
		return domainsFromID;
	}

    public String getHashCode() {
        return fHash;
    }

}
