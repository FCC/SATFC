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
package ca.ubc.cs.beta.stationpacking.cache.scripts;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.cache.CacheCoordinate;
import ca.ubc.cs.beta.stationpacking.cache.ISATFCCacheEntry;
import ca.ubc.cs.beta.stationpacking.cache.RedisCacher;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheSATEntry;
import ca.ubc.cs.beta.stationpacking.cache.containment.ContainmentCacheUNSATEntry;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.facade.*;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.utils.CacheUtils;
import ca.ubc.cs.beta.stationpacking.utils.GuavaCollectors;
import ca.ubc.cs.beta.stationpacking.utils.RedisUtils;
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Cleanup;
import lombok.Getter;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by newmanne on 2016-02-11.
 */
public class ConstrainCache {

    private static org.slf4j.Logger log;

    @UsageTextField(title = "Cache constrain script", description = "Iterate over a cache to accumulate another cache (via the server)")
    public static class FilterMandatoryStationsOptions extends AbstractOptions {

        @Parameter(names = "-MAX-CHANNEL", description = "The clearing target used for any cache entries that trigger resolving")
        @Getter
        private int maxChannel = StationPackingUtils.UHFmax;

        @ParametersDelegate
        @Getter
        private SATFCFacadeParameters facadeParameters = new SATFCFacadeParameters();

        @Parameter(names = "-MANDATORY-STATIONS", description = "Stations that must be present in each entry")
        private List<Integer> stations;

        public Set<Station> getRequiredStations() {
            return stations == null ? Collections.emptySet() : stations.stream().map(Station::new).collect(GuavaCollectors.toImmutableSet());
        }

        @Parameter(names = "-CONSTRAINT-SET", description = "All cache entries will be validated against this constraint set. Absolute path to folder")
        @Getter
        private String masterConstraintFolder;

        @Parameter(names = "-ALL-CONSTRAINTS", description = "Folder with all constraint sets", required = true)
        @Getter
        private String allConstraints;

        @Getter
        @Parameter(names = "-ENFORCE-MAX-CHANNEL", description = "Force a resolve (and delete) any cache entry where a station is assigned above the max channel")
        private boolean enforceMaxChannel = false;

        @Getter
        @Parameter(names = "-DISTRIBUTED", description = "Use the redis queuing system")
        private boolean distributed = false;

        @Getter
        @Parameter(names = "-ASSUME-IMPAIRING", description = "Whether or not to assume a station above max channel is impairing")
        private boolean assumeImpairing = false;

        @Getter
        @Parameter(names = "-NO-SOLVE", description = "Whether or not any solving is allowed")
        private boolean noSolve = true;

        @Getter
        @Parameter(names = "-IGNORE-UNSAT", description = "Whether to ignore UNSAT entries")
        private boolean ignoreUNSAT = true;

    }


    public static void main(String[] args) throws Exception {
        // Parse args
        final FilterMandatoryStationsOptions options = new FilterMandatoryStationsOptions();
        JCommanderHelper.parseCheckingForHelpAndVersion(args, options);

        SATFCFacadeBuilder.initializeLogging(options.facadeParameters.getLogLevel(), options.getFacadeParameters().logFileName);
        log = org.slf4j.LoggerFactory.getLogger(ConstrainCache.class);

        if (options.isDistributed()) {
            Preconditions.checkArgument(options.facadeParameters.cachingParams.serverURL != null, "No server URL specified!");
        } else if (options.facadeParameters.cachingParams.serverURL == null) {
            log.warn("No server URL specified. This script likely won't have side effects...");
        }

        final Jedis jedis = options.getFacadeParameters().fRedisParameters.getJedis();

        final Set<Station> requiredStations = options.getRequiredStations();
        log.info("Retrying every cache entry that does not contain stations {}", requiredStations);

        // Load constraint folders
        final DataManager dataManager = new DataManager();
        dataManager.loadMultipleConstraintSets(options.getAllConstraints());
        final ManagerBundle managerBundle = dataManager.getData(options.getMasterConstraintFolder());
        final CacheCoordinate masterCoordinate = managerBundle.getCacheCoordinate();

        log.info("Migrating all cache entries to match constraints in {} ({})", options.getMasterConstraintFolder(), masterCoordinate);

        // Load up a facade
        @Cleanup
        final SATFCFacade facade = SATFCFacadeBuilder.builderFromParameters(options.facadeParameters).build();

        @Cleanup
        final SATFCFacade unsatFacade = SATFCFacadeBuilder.builderFromParameters(options.facadeParameters).setConfigFile(InternalSATFCConfigFile.UNSAT_LABELLER).build();

        final RedisCacher redisCacher = new RedisCacher(dataManager, options.getFacadeParameters().fRedisParameters.getStringRedisTemplate(), options.getFacadeParameters().fRedisParameters.getBinaryJedis());

        if (!options.isDistributed()) {
            for (ISATFCCacheEntry cacheEntry : redisCacher.iterateSAT()) {
                processEntry(options, requiredStations, managerBundle, masterCoordinate, facade, unsatFacade, cacheEntry);
            }
        } else {
            String key;
            String queueName = options.getFacadeParameters().fRedisParameters.fRedisQueue;
            while (true) {
                key = jedis.rpoplpush(RedisUtils.makeKey(queueName), RedisUtils.makeKey(queueName, RedisUtils.PROCESSING_QUEUE));
                if (key == null) {
                    break;
                }
                final CacheUtils.ParsedKey parsedKey;
                try {
                    parsedKey = CacheUtils.parseKey(key);
                } catch (Exception e) {
                    if (!key.equals(RedisCacher.HASH_NUM)) {
                        log.warn("Exception parsing key " + key, e);
                    }
                    continue;
                }
                processEntry(options, requiredStations, managerBundle, masterCoordinate, facade, unsatFacade, redisCacher.cacheEntryFromKey(key));
            }
        }
        log.info("Finished. You should now restart the SATFCServer");
    }

    private static void processEntry(FilterMandatoryStationsOptions options, Set<Station> requiredStations, ManagerBundle managerBundle, CacheCoordinate masterCoordinate, SATFCFacade facade, SATFCFacade unsatFacade, ISATFCCacheEntry cacheEntry) {
        if (cacheEntry instanceof ContainmentCacheSATEntry) {
            processSATEntry((ContainmentCacheSATEntry) cacheEntry, requiredStations, options, facade, managerBundle);
        } else if (cacheEntry instanceof ContainmentCacheUNSATEntry) {
            if (!options.isIgnoreUNSAT()) {
                processUNSATEntry((ContainmentCacheUNSATEntry) cacheEntry, masterCoordinate, options, unsatFacade, managerBundle);
            }
        } else {
            throw new IllegalStateException("Cache entry neither sat or unsat?");
        }
    }

    public static void processUNSATEntry(ContainmentCacheUNSATEntry entry, CacheCoordinate masterCoordinate, FilterMandatoryStationsOptions options, SATFCFacade unsatFacade, ManagerBundle managerBundle) {
        final CacheCoordinate coordinate = CacheCoordinate.fromKey(entry.getKey());
        boolean rightCoordinate = coordinate.equals(masterCoordinate);
        if (!rightCoordinate) {
            log.debug("Key {} is not in right coordinate and UNSAT, skipping", entry.getKey());
            return; // We can't do anything with this
        }
        final Map<Integer, Set<Integer>> domains = entry.getDomains().entrySet().stream().collect(Collectors.toMap(k -> k.getKey().getID(), Map.Entry::getValue));
        unsatFacade.solve(domains, new HashMap<>(), options.facadeParameters.fInstanceParameters.Cutoff, options.facadeParameters.fInstanceParameters.Seed, managerBundle.getInterferenceFolder());
    }


    public static void processSATEntry(ContainmentCacheSATEntry entry, Set<? extends Station> requiredStations, FilterMandatoryStationsOptions options, SATFCFacade facade, ManagerBundle managerBundle) {
        log.info("Entry {} stats: Domain size is {}", entry.getKey(), entry.getElements().size());
        final int maxChannel = options.getMaxChannel();

        final IStationManager stationManager = managerBundle.getStationManager();
        final IConstraintManager constraintManager = managerBundle.getConstraintManager();

        // Some stations may have been deleted in between constraint sets, so let's get rid of them
        final Set<Station> entryStations = Sets.intersection(entry.getElements(), stationManager.getStations());
        final Set<Station> allRequiredStations = Sets.union(entryStations, requiredStations);

        final Map<Integer, Integer> previousAssignment;
        final Map<Integer, Set<Integer>> domains;

        // A) I can transfer b/c once I've removed stations that no longer exist, I'm still valid (and I meet any channel requirements if they are being enforced)
        final Map<Integer, Integer> entryAssignment = Maps.filterKeys(entry.getAssignmentStationToChannel(), s -> entryStations.contains(new Station(s)));
        if (entryStations.size() == allRequiredStations.size() && (!options.isAssumeImpairing() || entryAssignment.values().stream().allMatch(c -> c <= options.getMaxChannel())) && constraintManager.isSatisfyingAssignment(StationPackingUtils.channelToStationFromStationToChannel(entryAssignment))) {
            previousAssignment = entryAssignment;
            domains = previousAssignment.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Collections.singleton(e.getValue())));
        } else {
            if (options.isNoSolve()) {
                return;
            }
            // B) I have to be resolved.
            domains = new HashMap<>();
            for (Station s : allRequiredStations) {
                Integer prevChan = entryAssignment.get(s.getID());
                if (options.isAssumeImpairing() && prevChan != null && prevChan > maxChannel && stationManager.getDomain(s).contains(prevChan)) {
                    log.debug("Station {} above max chan, assuming impairing", s);
                    domains.put(s.getID(), Collections.singleton(prevChan));
                } else {
                    Set<Integer> restrictedDomain;
                    try {
                        restrictedDomain = stationManager.getRestrictedDomain(s, maxChannel, true);
                        if (!restrictedDomain.isEmpty()) {
                            domains.put(s.getID(), restrictedDomain);
                        } else {
                            log.debug("Skipping station {} due to no domain", s);
                        }
                    } catch (Exception e) {
                        log.debug("Skipping station {} due to no domain", s);
                    }
                }
            }
            // Make the previous assignment compatible with these domains

            previousAssignment = Maps.filterEntries(entry.getAssignmentStationToChannel(), e -> domains.get(e.getKey()) != null && domains.get(e.getKey()).contains(e.getValue()));

            log.debug("Pre prev assign size is {}", previousAssignment.size());

            // Make the previous assignment at least consistent in a stupid greedy way
            List<Integer> stations = new ArrayList<>(previousAssignment.keySet());
            for (Integer s1 : stations) {
                if (previousAssignment.containsKey(s1)) {
                    for (Integer s2 : stations) {
                        if (previousAssignment.containsKey(s2) && !Objects.equals(s1, s2)) {
                            if (!constraintManager.isSatisfyingAssignment(new Station(s1), previousAssignment.get(s1), new Station(s2), previousAssignment.get(s2))) {
                                // Violation! Remove s1 arbitrarily and continue
                                previousAssignment.remove(s1);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (!domains.values().stream().anyMatch(Set::isEmpty)) {
            // This will cause a re-cache of the newly done solution (unless something already exists in the cache)
            log.trace("Domains are {}", domains);
            log.info("Solving problem domain size is {}, prev assign size is {}", domains.size(), previousAssignment.size());
            final SATFCResult solve = facade.solve(domains, previousAssignment, options.facadeParameters.fInstanceParameters.Cutoff, options.facadeParameters.fInstanceParameters.Seed, managerBundle.getInterferenceFolder());
            if (solve.getResult().equals(SATResult.SAT)) {
                log.info("Re-solve successful");
            } else {
                log.info("Re-solve failed, {}", solve.getResult());
            }
        } else {
            log.info("Skipping re-solve due to empty domain");
        }

    }

}