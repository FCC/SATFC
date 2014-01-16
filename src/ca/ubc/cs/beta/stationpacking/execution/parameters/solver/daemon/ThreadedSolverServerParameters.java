package ca.ubc.cs.beta.stationpacking.execution.parameters.solver.daemon;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundleFactory;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.SATFCSolverBundle;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parameters for an executable solver.
 * @author afrechet
 *
 */
@UsageTextField(title="FCC StationPacking Daemon Solver Options",description="Parameters required to launch a daemon solver.")
public class ThreadedSolverServerParameters extends AbstractOptions {	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//Solver parameters
	@ParametersDelegate
	public ClaspLibSATSolverParameters SolverParameters = new ClaspLibSATSolverParameters();
	
	@Parameter(names= "-ALLOW-ANYONE", description = "whether the server should listen to all message on its port, or just localhost ones.")
	public boolean AllowAnyone = false;
	
	@Parameter(names = "-DATA-FOLDERNAME",description = "a list of data foldernames that the daemon should know about.", required=true)
	public List<String> DataFoldernames = new ArrayList<String>();
	
	@Parameter(names = "-PORT",description = "the localhost UDP port to listen to", required=true, validateWith=PortValidator.class)
	public int Port;
	
	@ParametersDelegate
	public ComplexLoggingOptions LoggingOptions = new ComplexLoggingOptions();
	
	public SolverManager getSolverManager()
	{
		Logger log = LoggerFactory.getLogger(ThreadedSolverServerParameters.class);
		
		//Setup solvers.
		final String clasplibrary = SolverParameters.Library; 
		SolverManager aSolverManager = new SolverManager(
				new ISolverBundleFactory() {
			
					@Override
					public ISolverBundle getBundle(IStationManager aStationManager,
							IConstraintManager aConstraintManager) {
						
						/*
						 * Set what solver selector will be used here.
						 */
						return new SATFCSolverBundle(clasplibrary, aStationManager, aConstraintManager);
						
					}
				}
				
				);
		
		//Gather any necessary station packing data.
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
	
	
	
}