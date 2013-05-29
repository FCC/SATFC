package ca.ubc.cs.beta.stationpacking.execution.executionparameters.parameterparser;

import java.io.FileReader;
import java.util.HashSet;


import au.com.bytecode.opencsv.CSVReader;

public class ReportParser {
	
	private HashSet<Integer> fCurrentStationIDs;
	private HashSet<Integer> fConsideredStationIDs;
	private HashSet<Integer> fPackingChannels;
	
	public ReportParser(String aReportFile)
	{
		CSVReader aReader;
		fConsideredStationIDs = new HashSet<Integer>();
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
				fConsideredStationIDs.addAll(fCurrentStationIDs);
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
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
