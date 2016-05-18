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
import java.util.List;

import com.google.common.base.Splitter;

import ca.ubc.cs.beta.stationpacking.execution.AuctionCSVParser;
import ca.ubc.cs.beta.stationpacking.execution.Converter;

/**
 * Created by newmanne on 2016-03-24.
 */
public class CsvToProblem extends ANameToProblem {

    final String csvRoot;
    final boolean useSolutionIfExists;

    public CsvToProblem(String interferencesFolder, String csvRoot, boolean useSolutionIfExists) {
        super(interferencesFolder);
        this.csvRoot = csvRoot;
        this.useSolutionIfExists = useSolutionIfExists;
    }

    @Override
    public Converter.StationPackingProblemSpecs getSpecs(String name) throws IOException {
        final List<String> splits = Splitter.on('_').splitToList(name);
        final String auction = splits.get(0);
        final String csvFile = csvRoot + File.separator + auction + File.separator + splits.get(1) + ".csv";
        final int index = Integer.parseInt(splits.get(splits.size() - 1));
        final List<AuctionCSVParser.ProblemAndAnswer> problemAndAnswers;
        problemAndAnswers = AuctionCSVParser.parseAuctionFile(auction, csvFile);
        final AuctionCSVParser.ProblemAndAnswer problemAndAnswer = problemAndAnswers.get(index - 1);
        final Converter.StationPackingProblemSpecs stationPackingProblemSpecs = new Converter.StationPackingProblemSpecs(name, problemAndAnswer.getDomains(), problemAndAnswer.getPreviousAssignment(), problemAndAnswer.getInterference(), null);
        if (useSolutionIfExists && problemAndAnswer.getResult().equals("yes")) {
            stationPackingProblemSpecs.setPreviousAssignment(problemAndAnswer.getAnswer());
        }
        return stationPackingProblemSpecs;
    }

}
