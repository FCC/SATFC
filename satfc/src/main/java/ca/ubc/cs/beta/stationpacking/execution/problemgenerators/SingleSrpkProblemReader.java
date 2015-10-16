package ca.ubc.cs.beta.stationpacking.execution.problemgenerators;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import ca.ubc.cs.beta.stationpacking.execution.AProblemReader;
import ca.ubc.cs.beta.stationpacking.execution.Converter;

import com.google.common.collect.Sets;

/**
 * Created by newmanne on 11/06/15.
 */
@Slf4j
public class SingleSrpkProblemReader extends AProblemReader {

    private final String interferencesFolder;
    private final String srpkFile;


    public SingleSrpkProblemReader(String srpkFile, String interferencesFolder) {
        this.interferencesFolder = interferencesFolder;
        this.srpkFile = srpkFile;
    }

    @Override
    public SATFCFacadeProblem getNextProblem() {
        if (index != 1) {
            return null;
        }
        final Converter.StationPackingProblemSpecs stationPackingProblemSpecs;
        try {
            stationPackingProblemSpecs = Converter.StationPackingProblemSpecs.fromStationRepackingInstance(srpkFile);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing file " + srpkFile, e);
        }
        final Set<Integer> stations = stationPackingProblemSpecs.getDomains().keySet();
        return new SATFCFacadeProblem(
                stations,
                stationPackingProblemSpecs.getDomains().values().stream().reduce(new HashSet<>(), Sets::union),
                stationPackingProblemSpecs.getDomains(),
                stationPackingProblemSpecs.getPreviousAssignment(),
                interferencesFolder + File.separator + stationPackingProblemSpecs.getDataFoldername(),
                new File(srpkFile).getName()
        );
    }

}
