package ca.ubc.cs.beta.stationpacking.test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Ignore;
import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacade;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeBuilder;
import ca.ubc.cs.beta.stationpacking.facade.SATFCResult;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

import com.google.common.collect.Sets;

/**
 * Created by newmanne on 29/06/15.
 */
@Slf4j
public class SATFCProblemDriver {

    final int clearingTarget = 31;

    @Ignore
    @Test
    public void driver() throws Exception {
        SATFCFacadeBuilder b = new SATFCFacadeBuilder();
        try (SATFCFacade facade = b.build()) {
            final String interference = "/ubc/cs/research/arrow/satfc/public/interference/021814SC3M";
            final Set<Integer> allChannels = Sets.union(StationPackingUtils.LVHF_CHANNELS, Sets.union(StationPackingUtils.UHF_CHANNELS, StationPackingUtils.HVHF_CHANNELS))
                    .stream().filter(c -> c <= clearingTarget).collect(Collectors.toSet());
            final IStationManager manager = new DomainStationManager(interference + File.separator + DataManager.DOMAIN_FILE);
            final Set<Station> forbiddenStations = new HashSet<>();
            final List<Station> stations = manager.getStations().stream().collect(Collectors.toList());
            final int cutoff = 60;
            while (true) {
                final Map<Integer, Set<Integer>> domains = getDomains(manager, allChannels, forbiddenStations);
                log.info("Solving problem with " + domains.size() + " stations");
                final SATFCResult solve = facade.solve(
                        domains,
                        new HashMap<Integer, Integer>(),
                        cutoff,
                        1,
                        interference
                );
                if (solve.getResult().equals(SATResult.SAT)) {
                    log.info("SAT result with " + forbiddenStations.size() + " stations forbidden");
                    log.info(solve.toString());
                    forbiddenStations.clear();
                    forbiddenStations.add(stations.get(RandomUtils.nextInt(0, stations.size())));
                } else {
                    forbiddenStations.add(stations.get(RandomUtils.nextInt(0, stations.size())));
                }
            }
        }
    }

    Map<Integer, Set<Integer>> getDomains(IStationManager manager, Set<Integer> allChannels, Set<Station> forbiddenStations) {
        final Map<Integer, Set<Integer>> domains = new HashMap<>();
        manager.getStations().stream().filter(s -> !forbiddenStations.contains(s)).forEach(s -> {
            final Set<Integer> collect = manager.getDomain(s).stream().filter(c -> c <= clearingTarget).collect(Collectors.toSet());
            if (!collect.isEmpty()) {
                domains.put(s.getID(), allChannels);
            } else {
                forbiddenStations.add(s);
            }
        });
        return domains;
    }
}