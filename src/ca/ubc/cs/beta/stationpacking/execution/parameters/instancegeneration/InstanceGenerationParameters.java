package ca.ubc.cs.beta.stationpacking.execution.parameters.instancegeneration;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.parameters.parser.ReportParser;
import ca.ubc.cs.beta.stationpacking.execution.parameters.repackingdata.RepackingDataParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.IncrementalSolverParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.TAESolverParameters;
import ca.ubc.cs.beta.stationpacking.experiment.InstanceGeneration;
import ca.ubc.cs.beta.stationpacking.experiment.InversePopulationStationIterator;
import ca.ubc.cs.beta.stationpacking.solver.reporters.IExperimentReporter;
import ca.ubc.cs.beta.stationpacking.solver.reporters.LocalExperimentReporter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.SolverParameters;

@UsageTextField(title="FCC Station Packing Instance Generation Options",description="Parameters required for an instance generation experiment.")
public class InstanceGenerationParameters extends AbstractOptions {
	
	//Experiment parameters
	@Parameter(names = "-EXPERIMENT_NAME", description = "Experiment name.", required=true)
	private String fExperimentName;	
	@Parameter(names = "-EXPERIMENT_NUMRUN", description = "Experiment execution number. By default no execution number.")
	private int fExperimentNumRun = -1;
	public String getExperimentName()
	{
		if(fExperimentNumRun!=-1)
		{
			return fExperimentName+Integer.toString(fExperimentNumRun);
		}
		else
		{
			return fExperimentName;
		}
	}
	
	@Parameter(names = "-EXPERIMENT_DIR", description = "Experiment directory to write reports to.", required=true)
	public String ExperimentDirectory;
	
	@Parameter(names = "-REPORT_FILE", description = "Report file of a previously executed experiment to be continued. STARTING_STATIONS, PACKING_CHANNELS and remaining stations to consider are extracted from it. Overrides other parmeters.")
	private String fReportFile;
	public HashSet<Integer> getConsideredStationsIDs()
	{
		if(fReportFile == null)
		{
			return getStartingStationsIDs();
		}
		else
		{
			return new ReportParser(fReportFile).getConsideredStationIDs();
		}
	}
		
	@Parameter(names = "-STARTING_STATIONS", description = "List of stations to start from.")
	private List<String> fStartingStations = new ArrayList<String>();
	private HashSet<Integer> getStartingStationsIDs()
	{
		if(fReportFile == null)
		{
			HashSet<Integer> aStartingStations = new HashSet<Integer>();
			for(String aStation : fStartingStations)
			{
				aStartingStations.add(Integer.valueOf(aStation));
			}
			return aStartingStations;
		}
		else
		{
			return new ReportParser(fReportFile).getCurrentStationIDs();
		}	
	}
	
	@Parameter(names = "-STATION_POPULATIONS_FILE", description = "File containing the list of populations.",required =true)
	private String fStationPopFile;
	private HashMap<Station,Integer> getStationPopulationMap(IStationManager aStationManager)
	{
		HashMap<Station,Integer> aStationPopulationMap = new HashMap<Station,Integer>();
		
		Logger log = LoggerFactory.getLogger(InstanceGenerationParameters.class);
		log.info("Assigning populations to stations...");
		
		try 
		{
			CSVReader aReader = new CSVReader(new FileReader(fStationPopFile));
			try
			{
				//Skip header
				aReader.readNext();
				String[] aLine;
				
				while((aLine = aReader.readNext())!=null)
				{
					Integer aStationID = Integer.valueOf(aLine[0]);
					Integer aPopulation = Integer.valueOf(aLine[1]);
					try
					{
						Station aStation = aStationManager.getStationfromID(aStationID);
						aStationPopulationMap.put(aStation, aPopulation);
						
					}
					catch(IllegalArgumentException e)
					{
						log.debug("Couldn't assign population to station with ID "+aStationID+" ("+e.getMessage()+").");
						continue;
					}
					
				}
				
				return aStationPopulationMap;
			}
			finally
			{
				aReader.close();
			}
		} 
		catch (IOException e) 
		{
			throw new IllegalArgumentException("Could not read populations from station population file "+fStationPopFile+" "+e.getMessage());
		}
	}
	
	public Iterator<Station> getStationIterator()
	{
		Logger log = LoggerFactory.getLogger(InstanceGenerationParameters.class);
		log.info("Getting the station iterator...");
	
		HashSet<Integer> aStartingStationIDs = getStartingStationsIDs();
		
		IStationManager aStationManager = RepackingDataParameters.getDACStationManager();
		
		HashSet<Station> aToConsiderStations = new HashSet<Station>();
		
		for(Station aStation : aStationManager.getStations())
		{
			if(!aStartingStationIDs.contains(aStation.getID()))
			{
				aToConsiderStations.add(aStation);
			}
		}
		
		
		HashMap<Station,Integer> aStationPopulations = getStationPopulationMap(aStationManager);
		
		return new InversePopulationStationIterator(aToConsiderStations,aStationPopulations, Seed);
	}
	
	public HashSet<Station> getStartingStations()
	{
		Logger log = LoggerFactory.getLogger(InstanceGenerationParameters.class);
		log.info("Getting the starting stations...");
		
		HashSet<Integer> aStartingStationIDs = getStartingStationsIDs();
		
		Set<Station> aStations = RepackingDataParameters.getDACStationManager().getStations();
		
		HashSet<Station> aStartingStations = new HashSet<Station>();
		
		for(Station aStation : aStations)
		{
			if(aStartingStationIDs.contains(aStation.getID()))
			{
				aStartingStations.add(aStation);
			}
		}
		return aStartingStations;
	}
	
	@Parameter(names = "-PACKING_CHANNELS", description = "List of channels to pack into.", required = true)
	private List<String> fPackingChannels = Arrays.asList("14" ,"15" ,"16" ,"17" ,"18" ,"19" ,"20" ,"21" ,"22" ,"23" ,"24" ,"25" ,"26" ,"27" ,"28" ,"29" ,"30");
	public HashSet<Integer> getPackingChannels()
	{
		Logger log = LoggerFactory.getLogger(InstanceGenerationParameters.class);
		log.info("Getting the packing channels...");
		if(fReportFile == null)
		{
			HashSet<Integer> aPackingChannels = new HashSet<Integer>();
			for(String aChannel : fPackingChannels)
			{
				aPackingChannels.add(Integer.valueOf(aChannel));
			}
			return aPackingChannels;
		}
		else
		{
			return new ReportParser(fReportFile).getPackingChannels();
		}
	}
	
	@Parameter(names = "-CUTOFF", description = "Time allowed to the feasibility checker (in seconds).")
	public double Cutoff = 1800.0;
	
	@Parameter(names = "-SEED", description = "(Random) seed given to the feasibility checker.")
	public long Seed = 1;
	
	//Solver parameters
	// change for solver parameters if not incremental // broken branching
	@ParametersDelegate
	public IncrementalSolverParameters SolverParameters = new IncrementalSolverParameters();
	
	//(Global) Data parameters
	@ParametersDelegate
	public RepackingDataParameters RepackingDataParameters = new RepackingDataParameters();
	
	public IExperimentReporter getExperimentReporter()
	{
		Logger log = LoggerFactory.getLogger(InstanceGenerationParameters.class);
		log.info("Getting the experiment reporter...");
		return new LocalExperimentReporter(ExperimentDirectory, getExperimentName());
	}
	
	public InstanceGeneration getInstanceGeneration()
	{
		Logger log = LoggerFactory.getLogger(InstanceGenerationParameters.class);
		log.info("Getting the instance generation experiment...");
		
		IStationManager aStationManager = RepackingDataParameters.getDACStationManager();
		IConstraintManager aConstraintManager = RepackingDataParameters.getDACConstraintManager(aStationManager); 
		
		return new InstanceGeneration(SolverParameters.getSolver(aStationManager,aConstraintManager), getExperimentReporter());
	}
}
