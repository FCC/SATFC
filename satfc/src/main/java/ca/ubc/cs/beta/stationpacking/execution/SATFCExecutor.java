/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.execution;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCExecutableParameters;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;

import com.beust.jcommander.ParameterException;
/**
 * Executes SATFC to solve a single question file.
 * @author afrechet
 */
public class SATFCExecutor {

	private final static long SEED = 1;
	
	/**
	 * @param args - arguments satisfying {@link SATFCExecutableParameters}.
	 */
	public static void main(String[] args) {
		
		//Parse the command line arguments in a parameter object.
		SATFCExecutableParameters parameters = new SATFCExecutableParameters();
		//Check for help
		try
		{
			JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters,TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
		}
		catch (ParameterException aParameterException)
		{
			throw aParameterException;
		}
		parameters.SATFCParameters.LoggingOptions.initializeLogging();
		Logger log = LoggerFactory.getLogger(SATFCExecutor.class);
		
		//Setup the solver manager.
		log.debug("Setting up solver manager...");
		SolverManager solverManager = parameters.SATFCParameters.SolverManagerParameters.getSolverManager();

		log.debug("Initializing problem data...");
		String data = parameters.WorkDirectory+File.separator+parameters.QuestionParameters.getInterferenceData(); 
		
		ISolverBundle bundle;
		try {
			bundle = solverManager.getData(data);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Could not initialized data at "+data);
		}
		
		IStationManager stationManager = bundle.getStationManager();

		log.debug("Initializing instance...");
		StationPackingInstance instance = parameters.QuestionParameters.getInstance(stationManager);
		log.info("Solving instance: \n {}",instance.toString());
		
		log.debug("Selecting solver...");
		ISolver solver = bundle.getSolver(instance);
		
		log.debug("Setting up termination criterion...");
		double cutoff = parameters.QuestionParameters.getCutoff();
		ITerminationCriterion cputimeCriterion = new CPUTimeTerminationCriterion(cutoff);
		ITerminationCriterion walltimeCriterion = new WalltimeTerminationCriterion(cutoff*1.5);
		ITerminationCriterion terminationCriterion = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(cputimeCriterion,walltimeCriterion));
		
		log.debug("Solving instance...");
		SolverResult result = solver.solve(instance, terminationCriterion, SEED);
		
		SATResult satisfiability = result.getResult();
		double runtime = result.getRuntime();
		log.info("Result: {}",satisfiability);
		log.info("Time taken: {} ",runtime);
		if(satisfiability.equals(SATResult.SAT))
		{
			log.info("Witness assignment: {}",result.getAssignment());
		}
		
		System.out.println(result.toParsableString());
		

	}

}
