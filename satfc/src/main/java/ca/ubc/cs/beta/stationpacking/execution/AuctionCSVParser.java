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
package ca.ubc.cs.beta.stationpacking.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import ch.qos.logback.classic.Level;
import lombok.Cleanup;
import lombok.Data;

/**
 * Created by newmanne on 2016-02-18.
 */
public class AuctionCSVParser {

    private static org.slf4j.Logger log;

    private final static AtomicInteger nProblems = new AtomicInteger();

    @UsageTextField(title = "s", description = "j")
    public static class AuctionCSVParserParams extends AbstractOptions {

        @Parameter(names = "-SERVER-URL", description = "server url")
        public String serverURL;

    }

    public static void main(String[] args) throws Exception {
        SATFCFacadeBuilder.initializeLogging(Level.INFO, null);
        log = org.slf4j.LoggerFactory.getLogger(AuctionCSVParser.class);
        final AuctionCSVParserParams p = new AuctionCSVParserParams();
        JCommanderHelper.parseCheckingForHelpAndVersion(args, p);
        final String INTERFERENCE_ROOT = "/ubc/cs/research/arrow/satfc/instances/interference-data/";

        @Cleanup
        final SATFCFacade facade = new SATFCFacadeBuilder()
                .setServerURL(p.serverURL)
                .build();

        final String CSV_FILE_ROOT = "/ubc/cs/research/arrow/satfc/instances/rawdata/csvs";
        final File[] csvFolders = new File(CSV_FILE_ROOT).listFiles(File::isDirectory);
        for (File auctionDir : Arrays.stream(csvFolders).filter(d -> Integer.parseInt(d.getName()) >= 3240).collect(Collectors.toList())) {
            final String auction = auctionDir.getName();
            log.info("Reading files from auction {}", auction);
            final File[] csvFiles = auctionDir.listFiles();
            for (File csvFile : csvFiles) {
                final List<ProblemAndAnswer> problems = parseAuctionFile(auction, csvFile.getAbsolutePath());
                for (ProblemAndAnswer problem : problems) {
                    if (problem.getInterference().equals("102015SC44U") && problem.getResult().equals("yes")) {
                        int maxChan = problem.getDomains().values().stream().flatMap(Collection::stream).mapToInt(Integer::valueOf).max().getAsInt();
                        if (StationPackingUtils.UHF_CHANNELS.contains(maxChan)) {
                            final SATFCResult satfcResult = facade.solve(problem.getDomains(), new HashMap<>(), 60.0, 1, INTERFERENCE_ROOT + File.separator + problem.getInterference(), problem.getInstanceName());
                            Preconditions.checkState(satfcResult.getResult().equals(SATResult.SAT), "Result was not SAT!");
                            int nSolved = nProblems.getAndIncrement();
                            if (nSolved % 100 == 0) {
                                log.info("Solved {} problems", nSolved);
                            }
                        }
                    }
                }
            }
        }

    }

    public static List<ProblemAndAnswer> parseAuctionFile(String auction, String fileName) throws IOException {
        final List<String> lines = Files.readAllLines(Paths.get(fileName));
        String interferenceData = null;
        Double cutoff = null;
        Long sequenceId = null;
        int i;
        for (i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final List<String> split = Splitter.on(',').splitToList(line);
            // Parse header
            if (split.get(0).equals("constraints")) {
                interferenceData = split.get(1);
            } else if (split.get(0).equals("timeout")) {
                cutoff = Double.parseDouble(split.get(1)) / 1000.0;
            } else if (split.get(0).equals("sequence_id")) {
                sequenceId = Long.parseLong(split.get(1));
            } else if (split.get(0).equals("shared_info")) {
                i++;
                break;
            } else {
                throw new IllegalStateException("Unrecognized key " + line);
            }
        }
        Preconditions.checkNotNull(interferenceData);
        Preconditions.checkNotNull(cutoff);
        Preconditions.checkNotNull(sequenceId);

        final Map<Integer, Integer> prevAssign = new HashMap<>();
        final Map<Integer, Set<Integer>> currentProblem = new HashMap<>();

        // Parse assignment

        for (; i < lines.size(); i++) {
            final String line = lines.get(i);
            final List<String> split = Splitter.on(',').splitToList(line);
            if (split.get(0).equals("problems")) {
                i++;
                break;
            } else {
                int station = Integer.parseInt(split.get(0));
                int prevChan = Integer.parseInt(split.get(1));
                Set<Integer> domains = split.subList(2, split.size()).stream().map(Integer::parseInt).collect(Collectors.toSet());
                prevAssign.put(station, prevChan);
                currentProblem.put(station, domains);
            }
        }

        // Parse problems
        final List<ProblemAndAnswer> problems = new ArrayList<>();

        int p = 0;
        for (; i < lines.size(); i++) {
            p += 1;
            final String line = lines.get(i);
            final List<String> split = Splitter.on(',').splitToList(line);
            if (split.get(0).equals("answers")) {
                i++;
                break;
            } else {
                int station = Integer.parseInt(split.get(0));
                Set<Integer> domains = split.subList(1, split.size()).stream().map(Integer::parseInt).collect(Collectors.toSet());
                Map<Integer, Set<Integer>> problem = new HashMap<>(currentProblem);
                problem.put(station, domains);

                final String fName = new File(fileName).getName().replace(".csv", "");
                final String name = auction + "_" + fName + "_" + sequenceId + "_" + p + ".srpk";
                problems.add(new ProblemAndAnswer(problem, prevAssign, interferenceData, name));
            }
        }

        // Parse answers
        p = 0;
        for (; i < lines.size(); i++) {
            p += 1;
            final String line = lines.get(i);
            final List<String> split = Splitter.on(',').splitToList(line);
            final String result = split.get(1);
            final Set<String> results = Sets.newHashSet("yes", "no", "unknown", "error", "interrupted");
            Preconditions.checkState(results.contains(result), "Unrecognized result %s", result);
            problems.get(p - 1).setResult(result);
            if (result.equals("yes")) {
                Map<Integer, Integer> stationToChannel = new HashMap<>();
                for (int j = 2; j < split.size(); j++) {
                    final List<String> sc = Splitter.on(':').splitToList(split.get(j));
                    int station = Integer.parseInt(sc.get(0));
                    int chan = Integer.parseInt(sc.get(1));
                    stationToChannel.put(station, chan);
                }
                problems.get(p - 1).setAnswer(stationToChannel);
            }
        }

        return problems;
    }

    @Data
    public static class ProblemAndAnswer {

        private final Map<Integer, Set<Integer>> domains;
        private final Map<Integer, Integer> previousAssignment;
        private final String interference;
        private final String instanceName;
        private String result;
        private Map<Integer, Integer> answer;

    }

}
