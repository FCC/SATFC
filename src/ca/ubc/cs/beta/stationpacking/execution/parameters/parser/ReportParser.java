package ca.ubc.cs.beta.stationpacking.execution.parameters.parser;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.math3.util.Pair;

import au.com.bytecode.opencsv.CSVReader;

public class ReportParser {
	
	private HashSet<Integer> fCurrentStationIDs;
	private HashSet<Integer> fConsideredStationIDs;
	private HashSet<Integer> fPackingChannels;
	
	private ArrayList<Pair<HashSet<Integer>,HashSet<Integer>>> fInstances;
	
	public ReportParser(String aReportFile)
	{
		CSVReader aReader;
		fConsideredStationIDs = new HashSet<Integer>();
		fInstances = new ArrayList<Pair<HashSet<Integer>,HashSet<Integer>>>();
		try 
		{
			aReader = new CSVReader(new FileReader(aReportFile),',');
		
			String[] aLine;
			while((aLine = aReader.readNext())!=null)
			{
				String[] aChannelList = aLine[0].split("_")[0].split("-");
				fPackingChannels = new HashSet<Integer>();
				for(String aChannel : aChannelList)
				{
					fPackingChannels.add(Integer.valueOf(aChannel));
				}
				
				String[] aStationList = aLine[0].split("_")[1].split("-");
				fCurrentStationIDs = new HashSet<Integer>();
				for(String aStation : aStationList)
				{
					fCurrentStationIDs.add(Integer.valueOf(aStation));
				}
				fInstances.add(new Pair<HashSet<Integer>,HashSet<Integer>>(fPackingChannels,fCurrentStationIDs));
				
				fConsideredStationIDs.addAll(fCurrentStationIDs);
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public ArrayList<Pair<HashSet<Integer>,HashSet<Integer>>> getInstanceIDs()
	{
		return fInstances;
	}
	
	public HashSet<Integer> getCurrentStationIDs()
	{
		return new HashSet<Integer>(fCurrentStationIDs);
	}
	
	public HashSet<Integer> getConsideredStationIDs()
	{
		return new HashSet<Integer>(fConsideredStationIDs);
	}
	
	public HashSet<Integer> getPackingChannels(){
		return new HashSet<Integer>(fPackingChannels);
	}
	
	
	
}
