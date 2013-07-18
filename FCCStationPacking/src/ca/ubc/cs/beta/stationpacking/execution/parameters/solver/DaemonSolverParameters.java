package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.repackingdata.RepackingDataParameters;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="FCC StationPacking Daemon Solver Options",description="Parameters required to launch a daemon solver.")
public class DaemonSolverParameters extends AbstractOptions {

	//Solver parameters
	@ParametersDelegate
	public TAESolverParameters SolverParameters = new TAESolverParameters();

	//(Global) Data parameters
	@ParametersDelegate
	public RepackingDataParameters RepackingDataParameters = new RepackingDataParameters();
	
	public ISolver getSolver()
	{
		Logger log = LoggerFactory.getLogger(ExecutableSolverParameters.class);
		log.info("Getting solver...");
		
		IStationManager aStationManager = RepackingDataParameters.getDACStationManager();
		IConstraintManager aConstraintManager = RepackingDataParameters.getDACConstraintManager(aStationManager);
		
		return SolverParameters.getSolver(aStationManager,aConstraintManager);
	}
	
}
