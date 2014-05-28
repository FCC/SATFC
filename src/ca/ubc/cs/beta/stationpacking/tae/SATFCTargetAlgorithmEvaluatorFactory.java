package ca.ubc.cs.beta.stationpacking.tae;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;

/**
 * Factory for the SATFC target algorithm evaluator.
 * @author afrechet
 *
 */
@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class SATFCTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory{

	@Override
	public String getName() {
		return "SATFC";
	}

	@Override
	public SATFCTargetAlgorithmEvaluator getTargetAlgorithmEvaluator(
			AbstractOptions options) {
		
		SATFCTargetAlgorithmEvaluatorOptions SATFCoptions;
		try
		{
			SATFCoptions = (SATFCTargetAlgorithmEvaluatorOptions) options;
		}
		catch(ClassCastException e)
		{
			throw new IllegalStateException("Could not cast given options to SATFC TAE options.");
		}
		
		SATFCFacadeBuilder facadeBuilder = new SATFCFacadeBuilder();
		
		String library = SATFCoptions.fLibrary;
		if(library != null)
		{
			facadeBuilder.setLibrary(library);
		}
		facadeBuilder.setInitializeLogging(false);
		
		SATFCFacade facade = facadeBuilder.build();
		
		return new SATFCTargetAlgorithmEvaluator(facade, SATFCoptions.fStationConfigFolder);
	}

	@Override
	public SATFCTargetAlgorithmEvaluatorOptions getOptionObject() {
		return new SATFCTargetAlgorithmEvaluatorOptions();
	}
}
