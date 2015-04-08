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
package ca.ubc.cs.beta.stationpacking.execution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;
import ca.ubc.cs.beta.stationpacking.tae.SATFCTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.stationpacking.tae.SATFCTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.stationpacking.tae.SATFCTargetAlgorithmEvaluatorOptions;

/**
 * Executes SATFC TAE. For testing purposes only.
 * @author afrechet
 */
public class SATFCTAEExecutor {
	
	@SuppressWarnings("javadoc")
    public static void main(String[] args) {
	    
		System.out.println(TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators());
		SATFCTargetAlgorithmEvaluatorFactory factory = new SATFCTargetAlgorithmEvaluatorFactory();
		
		SATFCTargetAlgorithmEvaluatorOptions options = factory.getOptionObject();
		
		//Populate options.
		options.fLibrary = "/ubc/cs/home/a/afrechet/arrow-space/git/fcc-station-packing/SATsolvers/clasp/jna/libjnaclasp.so";
		options.fStationConfigFolder = "/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingDeliverable/";
		
		TargetAlgorithmEvaluator tae = factory.getTargetAlgorithmEvaluator(options);
		
		String instance = "SATFC-TAE_TestInstance.csv";
		
		Map<String,String> context =  new HashMap<String,String>();
		context.put(SATFCTargetAlgorithmEvaluator.SATFC_CONTEXT_KEY, SATFCTargetAlgorithmEvaluator.SATFC_CONTEXT_KEY);
		
		AlgorithmRunConfiguration config = new AlgorithmRunConfiguration(
				new ProblemInstanceSeedPair(new ProblemInstance(instance), 0),
				60.0,
				ParameterConfigurationSpace.getSingletonConfigurationSpace().getDefaultConfiguration(),
				new AlgorithmExecutionConfiguration("", "", ParameterConfigurationSpace.getSingletonConfigurationSpace(), false, 60.0, context)
		);
		
		List<AlgorithmRunResult> results = tae.evaluateRun(Arrays.asList(config));
		
		System.out.println(results);
		
	}
	
}
