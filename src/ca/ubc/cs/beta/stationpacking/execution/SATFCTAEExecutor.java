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

public class SATFCTAEExecutor {
	
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
