package ca.ubc.cs.beta.stationpacking.datamanagers;

import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;

import ca.ubc.cs.beta.stationpacking.datastructures.Station;

public class DomainStationManager implements IStationManager{

	private HashMap<Integer,Station> fStations = new HashMap<Integer,Station>();
	
	public DomainStationManager(String aStationDomainsFilename) throws Exception{
		CSVReader aReader = new CSVReader(new FileReader(aStationDomainsFilename),',');
		String[] aLine;
		Integer aID,aChannel;
		String aString;
		Set<Integer> aChannelDomain;
		
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
					throw new Exception("Station "+aID+" has empty domain.");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			fStations.put(aID, new Station(aID,aChannelDomain,-1));
		}
		aReader.close();	
		
	}
	
	@Override
	public Set<Station> getStations() {
		return new HashSet<Station>(fStations.values());
	}
	
	public Station get(Integer aID){
		return fStations.get(aID);
	}

}
