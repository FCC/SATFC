/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.execution.problemgenerators.problemparsers;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.Sets;

import ca.ubc.cs.beta.stationpacking.execution.Converter;
import ca.ubc.cs.beta.stationpacking.execution.problemgenerators.SATFCFacadeProblem;

/**
 * Created by newmanne on 2016-03-24.
 */
public abstract class ANameToProblem implements IProblemParser {

    public ANameToProblem(String interferencesFolder) {
        this.interferencesFolder = interferencesFolder;
    }

    final String interferencesFolder;

    public abstract Converter.StationPackingProblemSpecs getSpecs(String name) throws IOException;

    @Override
    public SATFCFacadeProblem problemFromName(String name) throws IOException {
        String n = FilenameUtils.getBaseName(name);
        Converter.StationPackingProblemSpecs stationPackingProblemSpecs = getSpecs(name);
        return new SATFCFacadeProblem(
                stationPackingProblemSpecs.getDomains().keySet(),
                stationPackingProblemSpecs.getDomains().values().stream().reduce(new HashSet<>(), Sets::union),
                stationPackingProblemSpecs.getDomains(),
                stationPackingProblemSpecs.getPreviousAssignment(),
                interferencesFolder + File.separator + stationPackingProblemSpecs.getDataFoldername(),
                n
        );
    }

}
