package ca.ubc.cs.beta.stationpacking.execution.parameters.parsers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
/*
import java.lang.reflect.Array;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
*/

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.RepackingDataParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parser for main parameters related to executing an (instance generation) experiment.
 * @author afrechet
 *
 */
@UsageTextField(title="FCC StationPacking Instance Generation Options",description="Parameters required for an instance generation experiment.")
public class InstanceGenerationParameterParser extends AbstractOptions {	
	
	//Data parameters
	@ParametersDelegate
	private RepackingDataParameters fRepackingDataParameters = new RepackingDataParameters();
	public RepackingDataParameters getRepackingDataParameters()
	{
		return fRepackingDataParameters;
	}
	
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
	private String fExperimentDirectory;
	public String getExperimentDir()
	{
		return fExperimentDirectory;
	}
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
	public ReportParser getReportParser()
	{
		return new ReportParser(fReportFile);
	}
	
	@Parameter(names = "-STARTING_STATIONS", description = "List of stations to start from.")
	private List<String> fStartingStations = new ArrayList<String>();
	public HashSet<Integer> getStartingStationsIDs()
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
	
	@Parameter(names = "-PACKING_CHANNELS", description = "List of channels to pack in.")
	private List<String> fPackingChannels = Arrays.asList("14" ,"15" ,"16" ,"17" ,"18" ,"19" ,"20" ,"21" ,"22" ,"23" ,"24" ,"25" ,"26" ,"27" ,"28" ,"29" ,"30");
	public HashSet<Integer> getPackingChannels()
	{

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

	@Parameter(names = "-SEED", description = "Seed.")
	private long fSeed = 1;
	public long getSeed()
	{
		return fSeed;
	}

}
