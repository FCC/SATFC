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
package ca.ubc.cs.beta.stationpacking.facade;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.extendedcache.IStationDB;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.UBCSATLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.YAMLBundle;
import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.polling.IPollingService;
import ca.ubc.cs.beta.stationpacking.polling.PollingService;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.cache.ContainmentCacheProxy;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.Clasp3SATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.ubcsat.UBCSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.interrupt.InterruptibleTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.CacheUtils;
import ca.ubc.cs.beta.stationpacking.utils.TimeLimitedCodeBlock;
import ca.ubc.cs.beta.stationpacking.utils.Watch;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * A facade for solving station packing problems with SATFC.
 * Each instance of the facade corresponds to an independent copy
 * of SATFC (with different state).
 * A SATFCFacade should only be involved in one solve operation at a time: do not have multiple threads calling solve concurrently
 *
 * @author afrechet
 */
@Slf4j
public class SATFCFacade implements AutoCloseable {

    private final SolverManager fSolverManager;
    private SATFCCacheAugmenter augmenter;
    private final SATFCFacadeParameter parameter;
    // measures idle time since the last time this facade solved a problem
    private final Watch idleTime;
    private volatile ScheduledFuture<?> future;
    private final IPollingService pollingService;
    private final CloseableHttpAsyncClient httpClient;
    @Getter
    private String versionInfo;

    /**
     * Construct a SATFC solver facade
     * Package protected to enforce use of the builder
     *
     * @param aSATFCParameters parameters needed by the facade.
     */
    SATFCFacade(final SATFCFacadeParameter aSATFCParameters) {
        this.parameter = aSATFCParameters;
        pollingService = new PollingService();
        if (parameter.getServerURL() != null) {
            log.info("Starting http client");
            httpClient = CacheUtils.createHttpClient();
        } else {
            httpClient = null;
        }
        //Check provided library.
        validateLibraries(aSATFCParameters.getClaspLibrary(), aSATFCParameters.getSatensteinLibrary(), pollingService);

        log.info("Using clasp library {}", aSATFCParameters.getClaspLibrary());
        log.info("Using SATenstein library {}", aSATFCParameters.getSatensteinLibrary());
        log.info("Using bundle {}", aSATFCParameters.getSolverChoice());

        fSolverManager = new SolverManager(
                dataBundle -> {
                    switch (aSATFCParameters.getSolverChoice()) {
                        case YAML:
                            return new YAMLBundle(dataBundle, aSATFCParameters, pollingService, httpClient);
                        default:
                            throw new IllegalArgumentException("Unrecognized solver choice " + aSATFCParameters.getSolverChoice());
                    }
                },
                aSATFCParameters.getDataManager() == null ? new DataManager() : aSATFCParameters.getDataManager()
        );

        if (aSATFCParameters.getServerURL() != null && aSATFCParameters.getAutoAugmentOptions().isAugment()) {
            log.info("Augment parameters {}", aSATFCParameters.getAutoAugmentOptions());
            augmenter = new SATFCCacheAugmenter(this);
            // schedule it
            scheduleAugment(aSATFCParameters);
        }

        idleTime = Watch.constructAutoStartWatch();

        try {
            final String versionProperties = Resources.toString(Resources.getResource("version.properties"), Charsets.UTF_8);
            final Iterator<String> split = Splitter.on(System.lineSeparator()).split(versionProperties).iterator();
            Preconditions.checkState(split.hasNext(), "No version string in version properties file");
            final String versionString = Splitter.on('=').splitToList(split.next()).get(1);
            Preconditions.checkState(split.hasNext(), "No build time in version properties file");
            final String buildTimeString = Splitter.on('=').splitToList(split.next()).get(1);
            versionInfo = String.format("Using SATFC version %s, built on %s", versionString, buildTimeString);
            log.info(versionInfo);
        } catch (IllegalArgumentException | IOException e) {
            log.error("Problem retrieving version info");
            versionInfo = "Unknown version";
        }

    }

    /**
     * Solve a station packing problem.
     *
     * @param aDomains             - a map taking integer station IDs to set of integer channels domains.
     * @param aPreviousAssignment  - a valid (proved to not create any interference) partial (can concern only some of the provided station) station to channel assignment.
     * @param aCutoff              - a cutoff in seconds for SATFC's execution.
     * @param aSeed                - a long seed for randomization in SATFC.
     * @param aStationConfigFolder - a folder in which to find station config data (<i>i.e.</i> interferences and domains files).
     * @return a result about the packability of the provided problem, with the time it took to solve, and corresponding valid witness assignment of station IDs to channels.
     */
    public SATFCResult solve(
            Map<Integer, Set<Integer>> aDomains,
            Map<Integer, Integer> aPreviousAssignment,
            double aCutoff,
            long aSeed,
            String aStationConfigFolder) {
        return solve(aDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, null);
    }

    public SATFCResult solve(
            @NonNull Map<Integer, Set<Integer>> aDomains,
            @NonNull Map<Integer, Integer> aPreviousAssignment,
            double aCutoff,
            long aSeed,
            @NonNull String aStationConfigFolder,
            String instanceName) {
        return createInterruptibleSATFCResult(aDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, instanceName, false).computeResult();
    }

    /**
     * Solve a station packing problem. The channel domain of a station will be the intersection of the station's original domain (given in data files) with the packing channels,
     * and additionally intersected with its reduced domain if available and if non-empty.
     *
     * @param aStations            - a collection of integer station IDs.
     * @param aChannels            - a collection of integer channels.
     * @param aReducedDomains      - a map taking integer station IDs to set of integer channels domains.
     * @param aPreviousAssignment  - a valid (proved to not create any interference) partial (can concern only some of the provided station) station to channel assignment.
     * @param aCutoff              - a cutoff in seconds for SATFC's execution.
     * @param aSeed                - a long seed for randomization in SATFC.
     * @param aStationConfigFolder - a folder in which to find station config data (<i>i.e.</i> interferences and domains files).
     * @return a result about the packability of the provided problem, with the time it took to solve, and corresponding valid witness assignment of station IDs to channels.
     */
    public SATFCResult solve(Set<Integer> aStations,
                             Set<Integer> aChannels,
                             Map<Integer, Set<Integer>> aReducedDomains,
                             Map<Integer, Integer> aPreviousAssignment,
                             double aCutoff,
                             long aSeed,
                             String aStationConfigFolder
    ) {
        return solve(aStations, aChannels, aReducedDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, null);
    }

    public SATFCResult solve(@NonNull Set<Integer> aStations,
                             @NonNull Set<Integer> aChannels,
                             @NonNull Map<Integer, Set<Integer>> aReducedDomains,
                             @NonNull Map<Integer, Integer> aPreviousAssignment,
                             double aCutoff,
                             long aSeed,
                             String aStationConfigFolder,
                             String instanceName
    ) {
        log.debug("Transforming instance to a domains only instance.");

        //Construct the domains map.
        final Map<Integer, Set<Integer>> aDomains = new HashMap<>();
        for (Integer station : aStations) {
            Set<Integer> domain = new HashSet<>(aChannels);
            Set<Integer> reducedDomain = aReducedDomains.get(station);
            if (reducedDomain != null && !reducedDomain.isEmpty()) {
                domain = Sets.intersection(domain, reducedDomain);
            }
            aDomains.put(station, domain);
        }
        return solve(aDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, instanceName);
    }

    /**
     * @return An interruptibleSATFCResult. Call {@link InterruptibleSATFCResult#computeResult} to start solving the problem.
     * The problem will not begin solving automatically. The expected use case is that a reference to the {@link InterruptibleSATFCResult} will be made accessible to another thread, that may decide to interrupt the operation based on some external signal.
     * You can call {@link InterruptibleSATFCResult#interrupt()} from another thread to interrupt the problem while it is being solved.
     */
    public InterruptibleSATFCResult solveInterruptibly(Map<Integer, Set<Integer>> aDomains, Map<Integer, Integer> aPreviousAssignment, double aCutoff, long aSeed, String aStationConfigFolder) {
        return solveInterruptibly(aDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, null);
    }

    public InterruptibleSATFCResult solveInterruptibly(Map<Integer, Set<Integer>> aDomains, Map<Integer, Integer> aPreviousAssignment, double aCutoff, long aSeed, String aStationConfigFolder, String instanceName) {
        return createInterruptibleSATFCResult(aDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, instanceName, false);
    }

    InterruptibleSATFCResult createInterruptibleSATFCResult(
            @NonNull Map<Integer, Set<Integer>> aDomains,
            @NonNull Map<Integer, Integer> aPreviousAssignment,
            double aCutoff,
            long aSeed,
            @NonNull String aStationConfigFolder,
            String instanceName,
            boolean internal) {
        log.debug("Setting termination criterion...");
        //Set termination criterion.
        final InterruptibleTerminationCriterion termination = new InterruptibleTerminationCriterion();
        final SATFCProblemSolveCallable satfcProblemSolveCallable = new SATFCProblemSolveCallable(aDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, termination, instanceName, internal);
        return new InterruptibleSATFCResult(termination, satfcProblemSolveCallable);
    }

    /**
     * Augment the cache by generating and solving new problems indefinitely. This call never terminates.
     *
     * @param domains              Domains used for augmentation. A station used to augment the assignment will be drawn from this map, with this domain. A map taking integer station IDs to set of integer channels domains
     * @param assignment           The starting point for the augmentation. All augmentation will proceed from this starting point. A valid (proved to not create any interference) partial (can concern only some of the provided station) station to channel assignment.
     * @param aStationConfigFolder a folder in which to find station config data (<i>i.e.</i> interferences and domains files).
     * @param stationDB
     * @param cutoff               how long to spend on each generated problem before giving up
     */
    public void augment(@NonNull Map<Integer, Set<Integer>> domains, @NonNull Map<Integer, Integer> assignment, @NonNull IStationDB stationDB, @NonNull String aStationConfigFolder, double cutoff) {
        final SATFCCacheAugmenter satfcCacheAugmenter = new SATFCCacheAugmenter(this);
        satfcCacheAugmenter.augment(domains, assignment, stationDB, aStationConfigFolder, cutoff);
    }

    @AllArgsConstructor
    public class SATFCProblemSolveCallable implements Callable<SATFCResult> {

        private final Map<Integer, Set<Integer>> aDomains;
        private final Map<Integer, Integer> aPreviousAssignment;
        private final double aCutoff;
        private final long aSeed;
        private final String aStationConfigFolder;
        private final ITerminationCriterion criterion;
        private final String instanceName;
        // true if the call was generated from a cache augmenter
        private final boolean internal;

        @Override
        public SATFCResult call() throws Exception {
            if (parameter.getAutoAugmentOptions().isAugment() && !internal) {
                log.debug("Cancelling any ongoing augmentation operation (if one exists)");
                // Cancel any ongoing augmentation
                augmenter.stop();
            } else {
                idleTime.reset();
            }

            if (aDomains.isEmpty()) {
                log.warn("Provided an empty domains map.");
                return new SATFCResult(SATResult.SAT, 0.0, ImmutableMap.of());
            }
            Preconditions.checkArgument(aCutoff > 0, "Cutoff must be strictly positive");

            final ISolverBundle bundle = getSolverBundle(aStationConfigFolder);

            final IStationManager stationManager = bundle.getStationManager();

            log.debug("Translating arguments to SATFC objects...");
            //Translate arguments.
            final Map<Station, Set<Integer>> domains = new HashMap<>();

            for (Entry<Integer, Set<Integer>> entry : aDomains.entrySet()) {
                final Station station = stationManager.getStationfromID(entry.getKey());

                final Set<Integer> domain = entry.getValue();
                final Set<Integer> completeStationDomain = stationManager.getDomain(station);

                final Set<Integer> trueDomain = Sets.intersection(domain, completeStationDomain);

                if (trueDomain.isEmpty()) {
                    log.warn("Station {} has an empty domain, cannot pack.", station);
                    return new SATFCResult(SATResult.UNSAT, 0.0, ImmutableMap.of());
                }

                domains.put(station, trueDomain);
            }

            final Map<Station, Integer> previousAssignment = new HashMap<>();
            for (Station station : domains.keySet()) {
                final Integer previousChannel = aPreviousAssignment.get(station.getID());
                if (previousChannel != null && previousChannel > 0) {
                    Preconditions.checkState(domains.get(station).contains(previousChannel), "Provided previous assignment assigned channel " + previousChannel + " to station " + station + " which is not in its problem domain " + domains.get(station) + ".");
                    previousAssignment.put(station, previousChannel);
                }
            }

            log.debug("Constructing station packing instance...");
            //Construct the instance.
            final Map<String, Object> metadata = new HashMap<>();
            if (instanceName != null) {
                metadata.put(StationPackingInstance.NAME_KEY, instanceName);
            }
            StationPackingInstance instance = new StationPackingInstance(domains, previousAssignment, metadata);
            SATFCMetrics.postEvent(new SATFCMetrics.NewStationPackingInstanceEvent(instance, bundle.getConstraintManager()));

            log.debug("Getting solver...");
            //Get solver
            final ISolver solver = bundle.getSolver(instance);

            /*
             * Logging problem info
             */
            log.trace("Solving instance {} ...", instance);
            log.trace("Instance stats:");
            log.trace("{} stations.", instance.getStations().size());
            log.trace("stations: {}.", instance.getStations());
            log.trace("{} all channels.", instance.getAllChannels().size());
            log.trace("all channels: {}.", instance.getAllChannels());
            log.trace("Previous assignment: {}", instance.getPreviousAssignment());

            // Make sure that SATFC doesn't get hung. We give a VERY generous timeout window before throwing an exception
            final int SUICIDE_GRACE_IN_SECONDS = 5 * 60;
            final long totalSuicideGraceTimeInMillis = (long) (aCutoff + SUICIDE_GRACE_IN_SECONDS) * 1000;

            final DisjunctiveCompositeTerminationCriterion disjunctiveCompositeTerminationCriterion = new DisjunctiveCompositeTerminationCriterion(new WalltimeTerminationCriterion(aCutoff), criterion);

            //Solve instance.
            final SolverResult result;
            try {
                result = TimeLimitedCodeBlock.runWithTimeout(() -> solver.solve(instance, disjunctiveCompositeTerminationCriterion, aSeed), totalSuicideGraceTimeInMillis, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("SATFC waited " + totalSuicideGraceTimeInMillis + " ms for a result, but no result came back! The given timeout was " + aCutoff + " s, so SATFC appears to be hung. This is probably NOT a recoverable error");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            SATFCMetrics.postEvent(new SATFCMetrics.InstanceSolvedEvent(instanceName, result));

            log.debug("Transforming result into SATFC output...");
            // Transform back solver result to output result
            final Map<Integer, Integer> witness = new HashMap<>();
            for (Entry<Integer, Set<Station>> entry : result.getAssignment().entrySet()) {
                Integer channel = entry.getKey();
                for (Station station : entry.getValue()) {
                    witness.put(station.getID(), channel);
                }
            }

            final String extraInfo = getExtraInfo(bundle);

            final SATFCResult outputResult = new SATFCResult(result.getResult(), result.getRuntime(), witness, extraInfo);
            log.debug("Result: {}.", outputResult);

            if (!internal && parameter.getAutoAugmentOptions().isAugment()) {
                log.debug("Starting up timer again from 0 for augmentation");
                // Start measuring time again and reschedule jobs
                idleTime.start();
                scheduleAugment(parameter);
            }

            return outputResult;
        }

        private String getExtraInfo(ISolverBundle bundle) {
            final StringBuilder extraInfo = new StringBuilder();
            extraInfo.append(getVersionInfo()).append(System.lineSeparator());
            if (parameter.getServerURL() != null) {
                extraInfo.append("SATFCServer registered at URL ").append(parameter.getServerURL());
                if (ContainmentCacheProxy.lastSuccessfulCommunication != null) {
                    extraInfo.append(" last contacted succesfully at ").append(ContainmentCacheProxy.lastSuccessfulCommunication.toString());
                } else {
                    extraInfo.append(" has not yet been contacted");
                }
                extraInfo.append(System.lineSeparator());
            } else {
                extraInfo.append("SATFCServer is not being used").append(System.lineSeparator());
            }
            extraInfo.append("Enabled checkers: ").append(System.lineSeparator()).append(bundle.getCheckers());
            return extraInfo.toString();
        }

    }

    private ISolverBundle getSolverBundle(String aStationConfigFolder) {
        log.debug("Getting data managers...");
        //Get the data managers and solvers corresponding to the provided station config data.
        final ISolverBundle bundle;
        try {
            bundle = fSolverManager.getData(aStationConfigFolder);
        } catch (FileNotFoundException e) {
            log.error("Did not find the necessary data files in provided station config data folder {}.", aStationConfigFolder);
            throw new IllegalArgumentException("Station config files not found.", e);
        }
        return bundle;
    }

    private void scheduleAugment(final SATFCFacadeParameter aSATFCParameters) {
        final ScheduledExecutorService service = pollingService.getService();
        final long pollingInterval = (long) (1000 * aSATFCParameters.getAutoAugmentOptions().getPollingInterval());
        future = service.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    final double elapsedTime = idleTime.getElapsedTime();
                    log.debug("Checking to see if cache augmentation should happen. SATFC Facade has been idle for {}s and we require it to be idle for {}s", elapsedTime, aSATFCParameters.getAutoAugmentOptions().getIdleTimeBeforeAugmentation());
                    if (elapsedTime >= aSATFCParameters.getAutoAugmentOptions().getIdleTimeBeforeAugmentation()) {
                        log.info("SATFC Facade has been idle for {}, time to start performing cache augmentations", elapsedTime);
                        augmenter.augment(aSATFCParameters.getAutoAugmentOptions().getAugmentStationConfigurationFolder(), aSATFCParameters.getServerURL(), httpClient, aSATFCParameters.getAutoAugmentOptions().getAugmentCutoff());
                    } else {
                        // not time to augment, check again later
                        log.debug("SATFC Facade has been occupied recently, not time to augment");
                        service.schedule(this, pollingInterval, TimeUnit.MILLISECONDS);
                    }
                } catch (Throwable t) {
                    log.error("Caught exception in ScheduledExecutorService for scheduling augment", t);
                }
            }
        }, pollingInterval, TimeUnit.MILLISECONDS);
    }

    private void validateLibraries(final String claspLib, final String satensteinLib, final IPollingService pollingService) {
        validateLibraryFile(claspLib);
        validateLibraryFile(satensteinLib);
        try {
            new Clasp3SATSolver(claspLib, ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1, pollingService);
        } catch (UnsatisfiedLinkError e) {
            unsatisfiedLinkWarn(claspLib, e);
        }
        try {
            new UBCSATSolver(satensteinLib, UBCSATLibSATSolverParameters.DEFAULT_DCCA, pollingService);
        } catch (UnsatisfiedLinkError e) {
            unsatisfiedLinkWarn(satensteinLib, e);
        }
    }

    private void unsatisfiedLinkWarn(final String libPath, UnsatisfiedLinkError e) {
        log.error("\n--------------------------------------------------------\n" +
                "Could not load native library : {}. \n" +
                "Possible Solutions:\n" +
                "1) Try rebuilding the library, on Linux this can be done by going to the clasp folder and running \"bash compile.sh\"\n" +
                "2) Check that all library dependancies are met, e.g., run \"ldd {}\".\n" +
                "3) Manually set the library to use with the \"-CLASP-LIBRARY\" or \"-SATENSTEIN-LIBRARY\" options.\n" +
                "--------------------------------------------------------", libPath, libPath);
        throw new IllegalArgumentException("Could not load JNA library", e);
    }

    private void validateLibraryFile(final String libraryFilePath) {
        Preconditions.checkNotNull(libraryFilePath, "Cannot provide null library");
        final File libraryFile = new File(libraryFilePath);
        Preconditions.checkArgument(libraryFile.exists(), "Provided library " + libraryFile.getAbsolutePath() + " does not exist.");
        Preconditions.checkArgument(!libraryFile.isDirectory(), "Provided library is a directory.");
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down...");
        if (augmenter != null) {
            log.trace("Closing augmenter");
            augmenter.stop();
        }
        fSolverManager.close();
        if (httpClient != null) {
            log.trace("Closing http client");
            httpClient.close();
        }
        if (future != null) {
            future.cancel(false);
        }
        log.trace("Closing polling service");
        pollingService.notifyShutdown();
        log.info("Goodbye!");
    }

}
