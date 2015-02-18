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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;

import com.beust.jcommander.ParameterException;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

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
				JCommanderHelper.logCallString(args, SATFCFacadeExecutor.class);
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
			satfcBuilder.setSolverChoice(parameters.fSolverChoice);
			satfcBuilder.setCustomizationOptions(parameters.fSolverOptions.getOptions());
			
			SATFCFacade satfc = satfcBuilder.build();
			// TODO: actual parameter validation for user friendliness
			if (parameters.fInstanceFile != null && parameters.fInterferencesFolder != null && parameters.fInstanceFolder != null)
			{
				log.info("Reading instances from {}", parameters.fInstanceFile);
				final List<String> instanceFiles = Files.readLines(new File(parameters.fInstanceFile), Charsets.UTF_8);
				log.info("Read {} instances form {}", instanceFiles.size(), parameters.fInstanceFile);
				final List<String> errorInstanceFileNames = Lists.newArrayList();
				final Map<String, SATFCResult> instanceToResult = Maps.newHashMap();
				for (String instanceFileName : instanceFiles)
				{
					log.info("Beginning problem {}", instanceFileName);
					final Converter.StationPackingProblemSpecs stationPackingProblemSpecs;
					try
					{
						// TODO: detect extension and use the appropriate converter. For now I'm just assuming sprk
						stationPackingProblemSpecs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(parameters.fInstanceFolder + File.separator + instanceFileName);
					} catch (IOException e) {
						log.warn("Error parsing file {}", instanceFileName);
						errorInstanceFileNames.add(instanceFileName);
						e.printStackTrace();
						continue;
					}
					log.info("Solving ...");
					SATFCResult result = satfc.solve(
							stationPackingProblemSpecs.getDomains().keySet(),
							stationPackingProblemSpecs.getDomains().values().stream().reduce(Sets.newHashSet(), Sets::union),
							stationPackingProblemSpecs.getDomains(),
							stationPackingProblemSpecs.getPreviousAssignment(),
							parameters.fInstanceParameters.Cutoff,
							parameters.fInstanceParameters.Seed,
							parameters.fInterferencesFolder + File.separator + stationPackingProblemSpecs.getDataFoldername());
					log.info("..done!");
					System.out.println(result.getResult());
					System.out.println(result.getRuntime());
					System.out.println(result.getWitnessAssignment());
					instanceToResult.put(instanceFileName, result);
				}
				log.info("Finished all of the problems in {}!", parameters.fInstanceFile);
				if (!errorInstanceFileNames.isEmpty()) {
					log.error("The following files were not processed correctly: {}", errorInstanceFileNames);
				}
				log.info("Reporting metrics");
				if (parameters.fCsvOutputFile != null) {
					log.info("Logging output to csv: {}", parameters.fCsvOutputFile);
					final CSVWriter csvWriter = new CSVWriter(new BufferedWriter(new FileWriter(parameters.fCsvOutputFile)));
					csvWriter.writeAll(instanceToResult.entrySet().stream().map(result -> new String[]{result.getKey(), Double.toString(result.getValue().getRuntime()), result.getValue().getResult().toString()}).collect(Collectors.toList()));
					csvWriter.close();
				}
			} else {
				// assume SATFC called normally
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
