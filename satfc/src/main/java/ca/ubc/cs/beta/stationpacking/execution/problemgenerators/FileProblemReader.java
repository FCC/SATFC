package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import ca.ubc.cs.beta.stationpacking.execution.AProblemReader;
import ca.ubc.cs.beta.stationpacking.execution.Converter;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by newmanne on 12/05/15.
 */
@Slf4j
public class FileProblemReader extends AProblemReader {

    private final String interferencesFolder;
    private final List<String> instanceFiles;
    private int listIndex = 0;

    public FileProblemReader(String fileOfSrpkFiles, String interferencesFolder) {
        this.interferencesFolder = interferencesFolder;
        log.info("Reading instances from file {}", fileOfSrpkFiles);
        try {
            instanceFiles = Files.readLines(new File(fileOfSrpkFiles), Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read instance files from " + fileOfSrpkFiles, e);
        }
    }

    @Override
    public SATFCFacadeProblem getNextProblem() {
        String instanceFile = null;
        Converter.StationPackingProblemSpecs stationPackingProblemSpecs = null;
        while (listIndex < instanceFiles.size()) {
            instanceFile = instanceFiles.get(listIndex++);
            try {
                stationPackingProblemSpecs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(instanceFile);
                break;
            } catch (IOException e) {
                log.warn("Error parsing file {}", instanceFile);
            }
        }
        if (stationPackingProblemSpecs != null) {
            log.info("This is problem {} out of {}", index, instanceFiles.size());
            final Set<Integer> stations = stationPackingProblemSpecs.getDomains().keySet();
            return new SATFCFacadeProblem(
                    stations,
                    stationPackingProblemSpecs.getDomains().values().stream().reduce(new HashSet<>(), Sets::union),
                    stationPackingProblemSpecs.getDomains(),
                    stationPackingProblemSpecs.getPreviousAssignment(),
                    interferencesFolder + File.separator + stationPackingProblemSpecs.getDataFoldername(),
                    new File(instanceFile).getName()
            );
        } else {
            return null;
        }
    }

}
