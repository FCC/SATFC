package ca.ubc.cs.beta.stationpacking.data.manager;

import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;

import au.com.bytecode.opencsv.CSVReader;
/* NA - fStations is populated from aStationDomainsFilename, with population info from aStationFilename.
 * 
 */


public class DACStationManager implements IStationManager{
	
	//private Set<Station> fUnfixedStations = new HashSet<Station>();
	//private Set<Station> fFixedStations = new HashSet<Station>();
	private Set<Station> fStations = new HashSet<Station>();
	
	public DACStationManager(String aStationFilename, String aStationDomainsFilename) throws Exception{

		CSVReader aReader = new CSVReader(new FileReader(aStationDomainsFilename),',');
		String[] aLine;
		Integer aID,aChannel;
		String aString;
		Set<Integer> aChannelDomain;
		
		Map<Integer,Set<Integer>> aStationLookup = new HashMap<Integer,Set<Integer>>();

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
			aStationLookup.put(aID, aChannelDomain);
		}
		aReader.close();	
		System.out.println("aStationLookup is of size "+aStationLookup.size());

		Set<Integer> aChannels;
		Integer aStationPop;
		aReader = new CSVReader(new FileReader(aStationFilename));
		aReader.readNext();	//Skip header
		while((aLine = aReader.readNext())!=null){
			aID = Integer.valueOf(aLine[0]);
			if((aChannels = aStationLookup.get(aID))!=null){
				aStationPop = Integer.valueOf(aLine[4]);
				fStations.add(new Station(aID,aChannels,aStationPop));
				aStationLookup.remove(aID);
			}
		}
		aReader.close();
		if(!aStationLookup.isEmpty()){
			try{ 
				throw new Exception("Missing station population for "+aStationLookup.size()+" stations.");
			} catch(Exception e){
				e.printStackTrace();
				for(Integer aID1 : aStationLookup.keySet()){
					fStations.add(new Station(aID1,aStationLookup.get(aID1),0));
				}
			}
		}

		/*
		aReader = new CSVReader(new FileReader(aStationFilename));
		aReader.readNext();	//Skip header
		while((aLine = aReader.readNext())!=null){
			if(aLine[2].compareTo("USA")!=0 || aLine[4].trim().isEmpty()){
				fFixedStations.add(new Station(Integer.valueOf(aLine[0])));
			} else {
				Station aUnfixedStation = new Station(Integer.valueOf(aLine[0]));
				fUnfixedStations.add(aUnfixedStation);
				//fPopulation.put(aUnfixedStation, Integer.valueOf(aLine[4]));
			}
		}
		aReader.close();
		*/
	}
	
	/*
	public Map<Station,Integer> getStationPopulation(){
		Map<Station,Integer> aPopulationMap = new HashMap<Station,Integer>();
		for(Station aStation : fStations){
			aPopulationMap.put(aStation,aStation.getPop());
		}
		return aPopulationMap;
	}
	*/
	
	
	@Override
	public Set<Station> getStations() {
		return fStations;
	}


}
