package ca.ubc.cs.beta.stationpacking.datamanagers.stations;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.base.Station;

/**
 * Station manager that is populated from a channel domain file.
 * @author afrechet
 */
public class DomainStationManager implements IStationManager{

	private HashMap<Integer,Station> fStations = new HashMap<Integer,Station>();
	
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
				fStations.put(aID, new Station(aID,aChannelDomain));
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
	
	public Station getStationfromID(Integer aID){
		
		if(!fStations.containsKey(aID))
		{
			throw new IllegalArgumentException("Station manager does not contain station for ID "+aID);
		}
		
		return fStations.get(aID);
	}

}
