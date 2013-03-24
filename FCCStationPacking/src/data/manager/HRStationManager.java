package data.manager;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;

import data.Station;

public class HRStationManager implements IStationManager{
	
	private HashSet<Station> fUnfixedStations;
	private HashSet<Station> fFixedStations;
	private HashSet<Station> fStations;
	
	private HashMap<Station,Integer> fPopulation;

	public HRStationManager(String aStationFilename) throws IOException
	{
		fFixedStations = new HashSet<Station>();
		fUnfixedStations = new HashSet<Station>();
		fPopulation = new HashMap<Station,Integer>();
		
		try(CSVReader aReader = new CSVReader(new FileReader(aStationFilename)))
		{
		
			//Skip header
			aReader.readNext();
			
			String[] aLine;
			while((aLine = aReader.readNext())!=null)
			{
				if(aLine[2].compareTo("USA")!=0 || aLine[4].trim().isEmpty())
				{
					fFixedStations.add(new Station(Integer.valueOf(aLine[0])));
				}
				else
				{
					Station aUnfixedStation = new Station(Integer.valueOf(aLine[0]));
					fUnfixedStations.add(aUnfixedStation);
					
					fPopulation.put(aUnfixedStation, Integer.valueOf(aLine[4]));
				}
				
			}
		}
		
		fStations = new HashSet<Station>(fFixedStations);
		fStations.addAll(fUnfixedStations);
	}
	
	
	@Override
	public Set<Station> getStations() {
		return fStations;
	}

	@Override
	public Set<Station> getFixedStations() {
		return fFixedStations;
	}

	@Override
	public Set<Station> getUnfixedStations() {
		return fUnfixedStations;
	}


	@Override
	public HashMap<Station, Integer> getStationPopulation() {
		return fPopulation;
	}

}
