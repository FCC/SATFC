/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.sat.ClaspLibSATSolverParameters;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.CNFSolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.CacheOnlySolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.ISolverBundleFactory;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.MIPFCSolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.SATFCHydraBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.SATFCParallelSolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.SATFCSolverBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.StatsSolverBundle;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental.Clasp3SATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.TimeLimitedCodeBlock;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A facade for solving station packing problems with SATFC.
 * Each instance of the facade corresponds to an independent copy
 * of SATFC (with different state).
 *
 * @author afrechet
 */
@Slf4j
public class SATFCFacade implements AutoCloseable {

    private final SolverManager fSolverManager;


    SATFCFacade(final SATFCFacadeParameter aSATFCParameters) {
        this(aSATFCParameters, new DataManager());
    }

    /**
     * Construct a SATFC solver facade, with the option of initializing logging if its not already done.
     *
     * @param aSATFCParameters parameters needed by the facade.
     */
    public SATFCFacade(final SATFCFacadeParameter aSATFCParameters, final DataManager dataManager) {
        //Check provided library.
        if (aSATFCParameters.getClaspLibrary() == null) {
            throw new IllegalArgumentException("Cannot provide null library.");
        }

        final File libraryFile = new File(aSATFCParameters.getClaspLibrary());
        if (!libraryFile.exists()) {
            throw new IllegalArgumentException("Provided clasp library " + libraryFile.getAbsolutePath() + " does not exist.");
        }
        if (libraryFile.isDirectory()) {
            throw new IllegalArgumentException("Provided clasp library is a directory.");
        }

        try {
            new Clasp3SATSolver(aSATFCParameters.getClaspLibrary(), ClaspLibSATSolverParameters.UHF_CONFIG_04_15_h1);
        } catch (UnsatisfiedLinkError e) {

            log.error("\n--------------------------------------------------------\n" +
                            "Could not load clasp from library : {}. \n" +
                            "Possible Solutions:\n" +
                            "1) Try rebuilding the library, on Linux this can be done by going to the clasp folder and running \"bash compile.sh\"\n" +
                            "2) Check that all library dependancies are met, e.g., run \"ldd {}\".\n" +
                            "3) Manually set the library to use with the \"-CLASP-LIBRARY\" options. On MacOS it should be a .dylib file, on linux a .so file.\n" +
                            "--------------------------------------------------------", aSATFCParameters.getClaspLibrary(), aSATFCParameters.getClaspLibrary()
            );

            log.debug("Exception occured while loading library:\n", e);

            throw new IllegalArgumentException("Could not load JNA library.");
        }

        log.info("Using library {}.", aSATFCParameters.getClaspLibrary());
        log.info("Using bundle {}", aSATFCParameters.getSolverChoice());
        fSolverManager = new SolverManager(
                new ISolverBundleFactory() {

                    @Override
                    public ISolverBundle getBundle(IStationManager aStationManager,
                                                   IConstraintManager aConstraintManager) {

						/*
						 * SOLVER BUNDLE.
						 *
						 * Set what bundle we're using here.
						 */
                        switch (aSATFCParameters.getSolverChoice()) {
                            case SATFC_SEQUENTIAL:
                                return new SATFCSolverBundle(
                                        aSATFCParameters.getClaspLibrary(),
                                        aStationManager,
                                        aConstraintManager,
                                        aSATFCParameters.getResultFile(),
                                        aSATFCParameters.isPresolve(),
                                        aSATFCParameters.isDecompose(),
                                        aSATFCParameters.isUnderconstrained(),
                                        aSATFCParameters.getServerURL()
                                		);
                            case SATFC_PARALLEL:
                                return new SATFCParallelSolverBundle(
                                    aSATFCParameters.getClaspLibrary(),
                                    aStationManager,
                                    aConstraintManager,
                                    aSATFCParameters.getResultFile(),
                                    aSATFCParameters.isPresolve(),
                                    aSATFCParameters.isDecompose(),
                                    aSATFCParameters.isUnderconstrained(),
                                    aSATFCParameters.getServerURL(),
                                    aSATFCParameters.getNumCores()
                                );
                            case MIPFC:
                                return new MIPFCSolverBundle(aStationManager, aConstraintManager, aSATFCParameters.isPresolve(), aSATFCParameters.isDecompose());
                            case CNF:
                                return new CNFSolverBundle(aStationManager, aConstraintManager, aSATFCParameters.getCNFSaver());
                            case CACHING_SOLVER_FULL_INSTANCES:
                            case CACHING_SOLVER_COMPONENTS:
                                return new CacheOnlySolverBundle(aStationManager, aConstraintManager, aSATFCParameters.getServerURL(), aSATFCParameters.getSolverChoice() == SATFCFacadeParameter.SolverChoice.CACHING_SOLVER_COMPONENTS);
                            case HYDRA:
                                return new SATFCHydraBundle(aStationManager, aConstraintManager, aSATFCParameters.getHydraParams(), aSATFCParameters.getClaspLibrary());
                            case STATS:
                                return new StatsSolverBundle(aStationManager, aConstraintManager, aSATFCParameters.getClaspLibrary());
                            default:
                                throw new IllegalArgumentException("Unrecognized solver choice " + aSATFCParameters.getSolverChoice());
                        }
                    }
                },
                dataManager
        );
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
            String aStationConfigFolder,
            String instanceName) {
        if (aDomains == null || aPreviousAssignment == null || aStationConfigFolder == null) {
            throw new IllegalArgumentException("Cannot provide null arguments.");
        }

        if (aDomains.isEmpty()) {
            log.warn("Provided an empty domains map.");
            return new SATFCResult(SATResult.SAT, 0.0, new HashMap<Integer, Integer>());
        }
        if (aCutoff <= 0) {
            throw new IllegalArgumentException("Cutoff must be strictly positive.");
        }

        log.debug("Getting data managers...");
        //Get the data managers and solvers corresponding to the provided station config data.
        ISolverBundle bundle;
        try {
            bundle = fSolverManager.getData(aStationConfigFolder);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.error("Did not find the necessary data files in provided station config data folder {}.", aStationConfigFolder);
            throw new IllegalArgumentException("Station config files not found.");
        }

        IStationManager stationManager = bundle.getStationManager();

        log.debug("Translating arguments to SATFC objects...");
        //Translate arguments.
        final Map<Station, Set<Integer>> domains = new HashMap<>();

        for (Integer stationID : aDomains.keySet()) {
            Station station = stationManager.getStationfromID(stationID);

            Set<Integer> domain = aDomains.get(stationID);
            Set<Integer> stationDomain = stationManager.getDomain(station);

            Set<Integer> truedomain = Sets.intersection(domain, stationDomain);

            if (truedomain.isEmpty()) {
                log.warn("Station {} has an empty domain, cannot pack.", station);
                return new SATFCResult(SATResult.UNSAT, 0.0, new HashMap<Integer, Integer>());
            }

            domains.put(station, truedomain);
        }

        final Map<Station, Integer> previousAssignment = new HashMap<>();
        for (Station station : domains.keySet()) {
            Integer previousChannel = aPreviousAssignment.get(station.getID());
            if (previousChannel != null && previousChannel > 0) {
                Preconditions.checkState(domains.get(station).contains(previousChannel), "Provided previous assignment assigned channel " + previousChannel + " to station "+station+" which is not in its problem domain "+ domains.get(station)+".");
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
        ISolver solver = bundle.getSolver(instance);
		
		/*
		 * Logging problem info
		 */
        log.debug("Solving instance {} ...", instance);
        log.debug("Instance stats:");
        log.debug("{} stations.", instance.getStations().size());
        log.debug("stations: {}.", instance.getStations());
        log.debug("{} all channels.", instance.getAllChannels().size());
        log.debug("all channels: {}.", instance.getAllChannels());
        log.debug("Previous assignment: {}", instance.getPreviousAssignment());

        log.debug("Setting termination criterion...");
        //Set termination criterion.
        ITerminationCriterion termination = new WalltimeTerminationCriterion(aCutoff);

        // Make sure that SATFC doesn't get hung. We give a VERY generous timeout window before throwing an exception
        final int SUICIDE_GRACE_IN_SECONDS = 5 * 60;
        final long totalTimeInMillis = (long) (aCutoff + SUICIDE_GRACE_IN_SECONDS) * 1000;

        //Solve instance.
        SolverResult result = null;
        try {
            result = TimeLimitedCodeBlock.runWithTimeout(() -> solver.solve(instance, termination, aSeed), totalTimeInMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("SATFC waited " + totalTimeInMillis + " ms for a result, but no result came back! The given timeout was " + aCutoff + " s, so SATFC appears to be hung. This is probably NOT a recoverable error (e.g. it might mean that threads are stuck permanently in clasp)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.debug("Transforming result into SATFC output...");
        //Transform back solver result to output result.
        Map<Integer, Integer> witness = new HashMap<Integer, Integer>();
        for (Entry<Integer, Set<Station>> entry : result.getAssignment().entrySet()) {
            Integer channel = entry.getKey();
            for (Station station : entry.getValue()) {
                witness.put(station.getID(), channel);
            }
        }

        SATFCResult outputResult = new SATFCResult(result.getResult(), result.getRuntime(), witness);

        log.debug("Result: {}.", outputResult);

        return outputResult;

    }

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

    public SATFCResult solve(
            Map<Integer, Set<Integer>> aDomains,
            Map<Integer, Integer> aPreviousAssignment,
            double aCutoff,
            long aSeed,
            String aStationConfigFolder) {
        return solve(aDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, null);
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
                             String aStationConfigFolder,
                             String instanceName
    ) {
        //Check input.
        if (aStations == null || aChannels == null || aPreviousAssignment == null || aStationConfigFolder == null || aReducedDomains == null) {
            throw new IllegalArgumentException("Cannot provide null arguments.");
        }

        if (aStations.isEmpty()) {
            log.warn("Provided an empty set of stations.");
            return new SATFCResult(SATResult.SAT, 0.0, ImmutableMap.of());
        }
        if (aCutoff <= 0) {
            throw new IllegalArgumentException("Cutoff must be strictly positive.");
        }


        log.debug("Transforming instance to a domains only instance.");
        //Get the data managers and solvers corresponding to the provided station config data.
        ISolverBundle bundle;
        try {
            bundle = fSolverManager.getData(aStationConfigFolder);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.error("Did not find the necessary data files in provided station config data folder {}.", aStationConfigFolder);
            throw new IllegalArgumentException("Station config files not found.");
        }

        IStationManager stationManager = bundle.getStationManager();

        //TODO Change facade to only be given a simple domains map.
        //Construct the domains map.
        Map<Integer, Set<Integer>> aDomains = Maps.newLinkedHashMap();

        for (Integer station : aStations) {
            Set<Integer> originalDomain = stationManager.getDomain(stationManager.getStationfromID(station));
            Set<Integer> domain = Sets.intersection(originalDomain, aChannels);

            Set<Integer> reducedDomain = aReducedDomains.get(station);
            if (reducedDomain != null && !reducedDomain.isEmpty()) {
                domain = Sets.intersection(domain, reducedDomain);
            }

            aDomains.put(station, domain);
        }

        return solve(aDomains, aPreviousAssignment, aCutoff, aSeed, aStationConfigFolder, instanceName);
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down...");
        fSolverManager.close();
        log.info("Goodbye!");
    }

}
