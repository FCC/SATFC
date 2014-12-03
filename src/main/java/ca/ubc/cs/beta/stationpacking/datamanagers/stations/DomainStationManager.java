package ca.ubc.cs.beta.stationpacking.datamanagers.stations;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import ca.ubc.cs.beta.stationpacking.base.Station;
import au.com.bytecode.opencsv.CSVReader;

/**
 * In charge of managing collections of stations read from a domain file.
 * @author afrechet
 */
public class DomainStationManager implements IStationManager{

	private final Map<Integer,Station> fStations = new HashMap<Integer,Station>();
	private final Map<Station,Set<Integer>> fDomains = new HashMap<Station,Set<Integer>>();
	
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
	public HashSet<Station> getStationsfromID(Collection<Integer> aIDs) {
		HashSet<Station> stations = new HashSet<Station>();
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

}
