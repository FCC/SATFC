package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.daemon.simple.datamanager.SATBasedSolverFactory;
import ca.ubc.cs.beta.stationpacking.daemon.simple.datamanager.SolverManager;
import ca.ubc.cs.beta.stationpacking.daemon.simple.server.SolverServer;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters for an executable solver.
 * @author afrechet
 *
 */
@UsageTextField(title="FCC StationPacking Daemon Solver Options",description="Parameters required to launch a daemon solver.")
public class DaemonSolverParameters extends AbstractOptions {	
	
	//Solver parameters
	@ParametersDelegate
	public ClaspLibSATSolverParameters SolverParameters = new ClaspLibSATSolverParameters();
	
	@Parameter(names = "-DATA-FOLDERNAME",description = "a list of data foldernames that the daemon should know about.", required=true)
	public List<String> DataFoldernames;
	
	@Parameter(names = "-PORT",description = "the localhost UDP port to listen to", required=true)
	public int Port;
	
	public SolverManager getSolverManager()
	{
		Logger log = LoggerFactory.getLogger(DaemonSolverParameters.class);
		
		SolverManager aSolverManager = new SolverManager(new SATBasedSolverFactory(SolverParameters.getSATSolver(), new NoGrouper()));
		
		boolean isEmpty = true;
		for(String aDataFoldername : DataFoldernames)
		{
			try {
				if(!aDataFoldername.trim().isEmpty())
				{
					aSolverManager.addData(aDataFoldername);
					log.info("Read station packing data from {}.",aDataFoldername);
					isEmpty=false;
				}
			} catch (FileNotFoundException e) {
				log.warn("Could not read station packing data from {} ({}).",aDataFoldername,e.getMessage());
			}
		}
		if(isEmpty)
		{
			log.warn("The solver manager has been initialized without any station packing data.");
		}
		
		return aSolverManager;
	}
	
	public SolverServer getSolverServer()
	{
		try {
			return new SolverServer(getSolverManager(),Port);
		} catch (SocketException | UnknownHostException e) {
			throw new IllegalArgumentException("Could not create solver server with given information ("+e.getMessage()+").");
		}
	}
	

	
}