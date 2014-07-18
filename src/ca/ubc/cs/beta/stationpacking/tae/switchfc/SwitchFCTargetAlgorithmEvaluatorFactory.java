package ca.ubc.cs.beta.stationpacking.tae.switchfc;

import java.util.Map;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.stationpacking.tae.SATFCTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.stationpacking.tae.SATFCTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.stationpacking.tae.SATFCTargetAlgorithmEvaluatorOptions;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class SwitchFCTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory {

    @Override
    public String getName() {
        return "SWITCHFC";
    }

    @Override
    public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AbstractOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(Map<String, AbstractOptions> optionsMap) {
        
        SATFCTargetAlgorithmEvaluatorFactory satfcTaeFactory = new SATFCTargetAlgorithmEvaluatorFactory();
        CommandLineTargetAlgorithmEvaluatorFactory cliTaeFactory = new CommandLineTargetAlgorithmEvaluatorFactory();
        
        SATFCTargetAlgorithmEvaluator satfcTae = (SATFCTargetAlgorithmEvaluator) satfcTaeFactory.getTargetAlgorithmEvaluator(optionsMap);
        CommandLineTargetAlgorithmEvaluator cliTae = (CommandLineTargetAlgorithmEvaluator) cliTaeFactory.getTargetAlgorithmEvaluator(optionsMap);
        
        String configFolder = ((SATFCTargetAlgorithmEvaluatorOptions) optionsMap.get(satfcTaeFactory.getName())).fStationConfigFolder;
        
        SwitchFCTargetAlgorithmEvaluatorOptions thisOptions = (SwitchFCTargetAlgorithmEvaluatorOptions) optionsMap.get(this.getName());
        
        return new SwitchFCTargetAlgorithmEvaluator(satfcTae, cliTae, configFolder, thisOptions.fTmpDir);
    }

    @Override
    public AbstractOptions getOptionObject() {
        return new SwitchFCTargetAlgorithmEvaluatorOptions();
    }

}
