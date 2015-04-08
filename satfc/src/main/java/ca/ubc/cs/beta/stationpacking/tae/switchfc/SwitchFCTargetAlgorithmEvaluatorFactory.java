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
