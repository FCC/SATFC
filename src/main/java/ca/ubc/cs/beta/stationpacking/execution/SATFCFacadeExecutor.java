/**
 * Copyright 2014, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;

import com.beust.jcommander.ParameterException;

/**
 * Executes a SATFC facade built from parameters on an instance given in parameters.
 * @author afrechet
 */
public class SATFCFacadeExecutor {

	/**
	 * @param args - parameters satisfying {@link SATFCFacadeParameters}.
	 */
	public static void main(String[] args) {
		
		//Parse the command line arguments in a parameter object.
		Logger log = null ;
		try 
		{
			SATFCFacadeParameters parameters = new SATFCFacadeParameters();
			try 
			{
				//Check for help
				JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters,TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
				SATFCFacade.initializeLogging(parameters.fLoggingOptions.logLevel);
			}
			finally
			{
				log = LoggerFactory.getLogger(SATFCFacadeExecutor.class);
			}
			
			log.info("Initializing facade.");
			SATFCFacadeBuilder satfcBuilder = new SATFCFacadeBuilder();
			
			String library = parameters.fClaspLibrary;
			if(library != null)
			{
				satfcBuilder.setLibrary(parameters.fClaspLibrary);
			}
			satfcBuilder.setInitializeLogging(true);
			
			try(SATFCFacade satfc = satfcBuilder.build())
			{
				log.info("Solving ...");
				SATFCResult result = satfc.solve(
						parameters.fInstanceParameters.getPackingStationIDs(),
						parameters.fInstanceParameters.getPackingChannels(),
						parameters.fInstanceParameters.getDomains(),
						parameters.fInstanceParameters.getPreviousAssignment(),
						parameters.fInstanceParameters.Cutoff,
						parameters.fInstanceParameters.Seed,
						parameters.fInstanceParameters.fDataFoldername);
				
				log.info("..done!");
				
				System.out.println(result.getResult());
				System.out.println(result.getRuntime());
				System.out.println(result.getWitnessAssignment());
			}
		} catch (ParameterException e)
		{
			log.error("Invalid parameter argument detected ({}).",e.getMessage());
			e.printStackTrace();
			System.exit(AEATKReturnValues.PARAMETER_EXCEPTION);
		} catch (RuntimeException e)
		{
			log.error("Runtime exception encountered ({})",e.getMessage());
			e.printStackTrace();
			System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
		}catch(UnsatisfiedLinkError e)
		{
			log.error("Couldn't initialize facade, see previous log messages and/or try logging with DEBUG.");
			System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
		}catch(Throwable t)
		{
			log.error("Throwable encountered ({})",t.getMessage());
			t.printStackTrace();
			System.exit(AEATKReturnValues.UNCAUGHT_EXCEPTION);
		}
	}

}
