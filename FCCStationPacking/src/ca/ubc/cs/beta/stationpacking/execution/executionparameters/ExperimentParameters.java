package ca.ubc.cs.beta.stationpacking.execution.executionparameters;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.data.Station;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Parser for main parameters related to executing an (intance generation) experiment.
 * @author afrechet
 *
 */
public class ExperimentParameters {
	//Data parameters
	@Parameter(names = "-STATIONS_FILE", description = "Station list filename.", required=true)
	private String fStationFilename;
	public String getStationFilename()
	{
		return fStationFilename;
	}
	
	@Parameter(names = "-DOMAINS_FILE", description = "Stations' valid channel domains filename.", required=true)
	private String fDomainFilename;
	public String getDomainFilename()
	{
		return fDomainFilename;
	}
	
	@Parameter(names = "-CONSTRAINTS_FILE", description = "Constraints filename.", required=true)
	private String fConstraintFilename;
	public String getConstraintFilename()
	{
		return fConstraintFilename;
	}

	@Parameter(names = "-CNF_DIR", description = "Directory location of where to write CNFs.", required=true)
	private String fCNFDirectory;
	public String getCNFDirectory(){
		return fCNFDirectory;
	}
	
	//TAE parameters
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
	@Parameter(names = "-SOLVER", description = "SAT solver to use.", required=true, validateWith = ImplementedSolverParameterValidator.class)
	private String fSolver;
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
	private String fExperimentDirectory;
	public String getExperimentDir()
	{
		return fExperimentDirectory;
	}
	
	@Parameter(names = "-STARTING_STATIONS", description = "List of stations to start from.")
	private List<String> fStartingStations = new ArrayList<String>();
	public HashSet<Integer> getStartingStationsIDs()
	{
		HashSet<Integer> aStartingStations = new HashSet<Integer>();
		for(String aStation : fStartingStations)
		{
			aStartingStations.add(Integer.valueOf(aStation));
		}
		return aStartingStations;
	}
	
	@Parameter(names = "-PACKING_CHANNELS", description = "List of channels to pack in.")
	private List<String> fPackingChannels = Arrays.asList("14" ,"15" ,"16" ,"17" ,"18" ,"19" ,"20" ,"21" ,"22" ,"23" ,"24" ,"25" ,"26" ,"27" ,"28" ,"29" ,"30");
	public HashSet<Integer> getPackingChannels()
	{
		HashSet<Integer> aPackingChannels = new HashSet<Integer>();
		for(String aChannel : fPackingChannels)
		{
			aPackingChannels.add(Integer.valueOf(aChannel));
		}
		return aPackingChannels;
	}
	
	@Parameter(names = "-SEED", description = "Seed.")
	private long fSeed = 1;
	public long getSeed()
	{
		return fSeed;
	}
	
	@Parameter(names = "-SOLVER_CUTOFF", description = "Solver CPU cutoff time in seconds.")
	private double fSolverCutoff = 1800;
	public double getSolverCutoff()
	{
		return fSolverCutoff;
	}

}
