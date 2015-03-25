/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.execution.parameters.solver;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCCachingParameters;
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

/**
 * Parameters to cosntruct a SATFC solver manager.
 * @author afrechet
 */
@UsageTextField(title="SATFC Solver Manager Parameters",description="Parameters defining a SATFC solver manager.")
public class SATFCSolverManagerParameters extends AbstractOptions {
	
    /**
     * ISolver parameters.
     */
	@ParametersDelegate
	public ClaspLibSATSolverParameters SolverParameters = new ClaspLibSATSolverParameters();
	
	/**
	 * Config foldernames.
	 */
	@Parameter(names = "-DATA-FOLDERNAME",description = "a list of data foldernames that SATFC should know about.", required=true)
	public List<String> DataFoldernames = new ArrayList<String>();
	
	/**
	 * CNF directory.
	 */
	@Parameter(names = "-CNF-DIRECTORY",description = "a directory in which to put CNF problems encountered by SATFC.")
	public String CNFDirectory = null;
	
	/**
	 * Result file.
	 */
	@Parameter(names = "-RESULT-FILE", description = "a file in which to save the results of problems encountered.")
	public String ResultFile = null;

	/**
	 * 	Caching parameters
	 */
	@ParametersDelegate
	public SATFCCachingParameters satfcCachingParameters = new SATFCCachingParameters();

	/**
	 * @return SATFC solver manager initialized with the given parameters.
	 */
	public SolverManager getSolverManager()
	{
		Logger log = LoggerFactory.getLogger(SATFCSolverManagerParameters.class);
		
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
						// TODO: allow specification of solver customization options
						return new SATFCSolverBundle(clasplibrary, aStationManager, aConstraintManager,CNFDirectory,ResultFile, new SATFCFacadeParameter.SolverCustomizationOptions());

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
