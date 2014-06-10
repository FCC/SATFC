package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundleFactory;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.SATFCSolverBundle;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@UsageTextField(title="SATFC Solver Manager Parameters",description="Parameters defining a SATFC solver manager.")
public class SolverManagerParameters extends AbstractOptions {
	
	//Solver parameters
	@ParametersDelegate
	public ClaspLibSATSolverParameters SolverParameters = new ClaspLibSATSolverParameters();
	
	@Parameter(names = "-DATA-FOLDERNAME",description = "a list of data foldernames that SATFC should know about.", required=true)
	public List<String> DataFoldernames = new ArrayList<String>();
	
	@Parameter(names = "-CNF-DIRECTORY",description = "a directory in which to put CNF problems encountered by SATFC.")
	public String CNFDirectory = null;
	
	@Parameter(names = "-RESULT-FILE", description = "a file in which to save the results of problems encountered.")
	public String ResultFile = null;
	
	public SolverManager getSolverManager()
	{
		Logger log = LoggerFactory.getLogger(SolverManagerParameters.class);
		
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
						return new SATFCSolverBundle(clasplibrary, aStationManager, aConstraintManager,CNFDirectory,ResultFile);
						
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
