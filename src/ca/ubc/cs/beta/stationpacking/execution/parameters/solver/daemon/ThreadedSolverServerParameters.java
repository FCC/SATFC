package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.daemon;

import java.io.FileNotFoundException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.CompressedSATBasedSolverFactory;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.solvers.componentgrouper.NoGrouper;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters for an executable solver.
 * @author afrechet
 *
 */
@UsageTextField(title="FCC StationPacking Daemon Solver Options",description="Parameters required to launch a daemon solver.")
public class ThreadedSolverServerParameters extends AbstractOptions {	
	
	//Solver parameters
	@ParametersDelegate
	public ClaspLibSATSolverParameters SolverParameters = new ClaspLibSATSolverParameters();
	
	@Parameter(names = "-DATA-FOLDERNAME",description = "a list of data foldernames that the daemon should know about.", required=true)
	public List<String> DataFoldernames;
	
	@Parameter(names = "-PORT",description = "the localhost UDP port to listen to", required=true, validateWith=PortValidator.class)
	public int Port;
	
	public SolverManager getSolverManager()
	{
		Logger log = LoggerFactory.getLogger(ThreadedSolverServerParameters.class);
		
		SolverManager aSolverManager = new SolverManager(new CompressedSATBasedSolverFactory(SolverParameters.getSATSolver(), new NoGrouper()));
		
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
	
	public class PortValidator implements IParameterValidator
	{

		@Override
		public void validate(String name, String value)
				throws ParameterException {
			
			int aPort = Integer.valueOf(value);
			if (aPort >= 0 && aPort < 1024)
			{
				throw new ParameterException("Trying to allocate a port < 1024 which generally requires root priviledges (which aren't necessary and discouraged), this may fail");
			}
			if(aPort < -1 || aPort > 65535)
			{
				throw new ParameterException("Port must be in the interval [0,65535]");
			}
			
			
		}
		
	}
	
}