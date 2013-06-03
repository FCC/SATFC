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
import ca.ubc.cs.beta.aclib.options.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.stationpacking.execution.parameters.ImplementedSolverParameterValidator;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parser for main parameters related to executing an (instance generation) experiment.
 * @author afrechet
 *
 */
@UsageTextField(title="FCC StationPacking Instance Generation Options",description="Parameters required for an instance generation experiment.")
public class InstanceGenerationParameters extends AbstractOptions {	
	
	//Data parameters
	@ParametersDelegate
	private RepackingDataParameters fRepackingDataParameters = new RepackingDataParameters();
	public RepackingDataParameters getRepackingDataParameters()
	{
		return fRepackingDataParameters;
	}
	
	//TAE parameters
	@ParametersDelegate
	private AlgorithmExecutionOptions fAlgorithmExecutionOptions = new AlgorithmExecutionOptions();
	public AlgorithmExecutionOptions getAlgorithmExecutionOptions() {
		return fAlgorithmExecutionOptions;
	}
	/**
	 * Deprecated TAE parameters.
	 * 
	@Parameter(names = "-TAE_EXEC_DIR", description = "Directory location of where to execute TAE.")
	private String fExecutionDirectory = "SATsolvers/";
	public String getTAEExecDirectory()
	{
		return fExecutionDirectory;
	}
	@Parameter(names = "-TAE_EXECUTABLE", description = "TAE callstring.")
	private String fAlgorithmExecutable = "python solverwrapper.py";
	public String getTAEExecutable()
	{
		return fAlgorithmExecutable;
	}
	@Parameter(names = "-TAE_TYPE", description = "TAE execution environment.")
	private String fTAEType = "CLI";
	public String getTAEType()
	{
		return fTAEType;
	}
	
	@Parameter(names = "-TAE_CONC_EXEC_NUM", description = "Maximum number of concurrent TAE executions.")
	private int fMaximumConcurrentExecutions = 1;
	public int getTAEMaxConcurrentExec()
	{
		return fMaximumConcurrentExecutions;
	}
	
	@Parameter(names = "-TAE_PARAM_CONF_SPACE", description = "TAE parameter configuration space, usually used to specify which solver should be executed out of the default multi-solver wrapper. Overrides the SOLVER option. By default not set, use the SOLVER option.")
	private String fTAEParamConfSpace = "";
	public String getTAEParamConfigSpace()
	{
		if(fTAEParamConfSpace.trim().equals(""))
		{
			return "SATsolvers/sw_parameterspaces/sw_"+fSolver+".txt";
		}
		else
		{
			return fTAEParamConfSpace;
		}
	}
	
	@Parameter(names = "-SOLVER_CUTOFF", description = "Solver CPU cutoff time in seconds.")
	private double fSolverCutoff = 1800;
	public double getSolverCutoff()
	{
		return fSolverCutoff;
	}

	**/

	@Parameter(names = "-SOLVER", description = "SAT solver to use.", required=true, validateWith = ImplementedSolverParameterValidator.class)
	private String fSolver;
	public String getSolver()
	{
		return fSolver;
	}
	
	//Experiment parameters
	@Parameter(names = "-CNF_DIR", description = "Directory location of where to write CNFs.", required=true)
	private String fCNFDirectory;
	public String getCNFDirectory(){
		return fCNFDirectory;
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

	@Parameter(names = "-CNFLOOKUP_OUTPUT_FILE", description = "File to store CNF results.")
	private String fCNFOutputName = "CNFOutput";
	public String getCNFOutputName()
	{
		if(fExperimentNumRun!=-1)
		{
			return fCNFOutputName+Integer.toString(fExperimentNumRun);
		}
		else
		{
			return fCNFOutputName;
		}
		
	}
}
