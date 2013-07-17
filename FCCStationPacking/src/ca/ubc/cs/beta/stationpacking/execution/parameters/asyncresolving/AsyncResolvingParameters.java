package ca.ubc.cs.beta.stationpacking.execution.parameters.asyncresolving;

import java.io.FileReader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import au.com.bytecode.opencsv.CSVReader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.AsyncTAESolverParameters;
import ca.ubc.cs.beta.stationpacking.solver.reporters.AsynchronousLocalExperimentReporter;

@UsageTextField(title="FCC Station Packing Instance Generation Options",description="Parameters required for an instance generation experiment.")
public class AsyncResolvingParameters extends AbstractOptions {
	
	//Experiment parameters
	@Parameter(names = "-EXPERIMENT_NAME", description = "Experiment name.", required=true)
	public String ExperimentName;	
	
	@Parameter(names = "-EXPERIMENT_DIR", description = "Experiment directory to write reports to.", required=true)
	public String ExperimentDirectory;
	
	@Parameter(names = "-INSTANCE_FILE", description = "A CSV file on instances to solve, where each line is a toString() version of an instance.", required=true)
	public String InstanceFile;	
	
	//Solving parameters
	@Parameter(names = "-CUTOFF", description = "Time allowed to the feasibility checker (in seconds).")
	public double Cutoff = 1800.0;
	
	@Parameter(names = "-SEED", description = "(Random) seed given to the feasibility checker.")
	public long Seed = 1;
	
	//Solver parameters
	@ParametersDelegate
	public AsyncTAESolverParameters SolverParameters = new AsyncTAESolverParameters();
	
	public AsynchronousLocalExperimentReporter getExperimentReporter()
	{
		Logger log = LoggerFactory.getLogger(AsyncResolvingParameters.class);
		log.info("Getting the experiment reporter...");
		return new AsynchronousLocalExperimentReporter(ExperimentDirectory, ExperimentName);
	}
	
	public ArrayList<StationPackingInstance> getInstances()
	{
		Logger log = LoggerFactory.getLogger(AsyncResolvingParameters.class);
		log.info("Getting instances to solve...");
		
		ArrayList<StationPackingInstance> aInstances = new ArrayList<StationPackingInstance>();
		
		IStationManager aStationManager = SolverParameters.RepackingDataParameters.getDACStationManager();
		
		try {
			CSVReader aReader = new CSVReader(new FileReader(InstanceFile));
			
			String[] aLine;
			
			while((aLine = aReader.readNext())!=null)
			{
				String aInstanceString = aLine[0];
				
				StationPackingInstance aInstance = StationPackingInstance.valueOf(aInstanceString, aStationManager);
				
				aInstances.add(aInstance);
				
			}
			
		} catch (Exception e) {
			throw new IllegalArgumentException("Couldn't read instances from "+InstanceFile+" ("+e.getMessage()+")");
		}
		
		return aInstances;
		
	}
}
