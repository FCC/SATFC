package ca.ubc.cs.beta.stationpacking.datamanagers;

import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datastructures.Station;

import au.com.bytecode.opencsv.CSVReader;

/* NA - fStations is populated from aStationDomainsFilename, with population info from aStationFilename.
 * 
 */


public class PopulatedDomainStationManager implements IStationManager{
	
	private HashMap<Integer,Station> fStations = new HashMap<Integer,Station>();
	public PopulatedDomainStationManager(String aStationFilename, String aStationDomainsFilename) throws Exception{
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
					throw new IllegalStateException("Station "+aID+" has empty domain.");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			aStationLookup.put(aID, aChannelDomain);
		}
		aReader.close();	

		Set<Integer> aChannels;
		Integer aStationPop;
		aReader = new CSVReader(new FileReader(aStationFilename));
		aReader.readNext();	//Skip header
		while((aLine = aReader.readNext())!=null){
			aID = Integer.valueOf(aLine[0]);
			if((aChannels = aStationLookup.get(aID))!=null){
				aStationPop = Integer.valueOf(aLine[4]);
				
				fStations.put(aID,new Station(aID,aChannels,aStationPop));
				aStationLookup.remove(aID);
			}
		}
		aReader.close();
		if(!aStationLookup.isEmpty()){
//			try{
//				//AF - print out stations with no populations;
//				/*
//				ArrayList<Integer> aNoPopStations = new ArrayList<Integer>(aStationLookup.keySet());
//				Collections.sort(aNoPopStations);
//				for(Integer aNoPopStation : aNoPopStations)
//				{
//					System.out.println(aNoPopStation);
//				}
//				*/
//				//throw new Exception("Missing station population for "+aStationLookup.size()+" stations.");
//			} catch(Exception e){
//				e.printStackTrace();
//				
//			}
			for(Integer aID1 : aStationLookup.keySet()){
				aChannels = aStationLookup.get(aID1);
				fStations.put(aID1,new Station(aID1,aChannels,0));
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


}
