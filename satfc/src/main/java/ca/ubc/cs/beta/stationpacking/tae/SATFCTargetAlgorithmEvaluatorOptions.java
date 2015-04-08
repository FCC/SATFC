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
package ca.ubc.cs.beta.stationpacking.tae;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

@UsageTextField(title="SATFC Target Algorithm Evaluator Options",description="Options needed to create a SATFC target algorithm evaluator.",claimRequired="--satfctae-station-folder,--satfctae-library")
public class SATFCTargetAlgorithmEvaluatorOptions extends AbstractOptions {

	@Parameter(names = "--satfctae-config-folder",description = "where to find station config data folders")
	public String fStationConfigFolder;
	
	@Parameter(names = {"--satfctae-library"},description = "the location of the necessary (clasp) SAT solver library")
	public String fLibrary;
	
}
