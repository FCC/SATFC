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

import static ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverChoice.SATFC_PARALLEL;
import static ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverChoice.SATFC_SEQUENTIAL;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.common.io.Resources;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Builder;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.smac.SATFCHydraParams;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverChoice;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.SATFCParallelSolverBundle;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;
import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder in charge of creating a SATFC facade, feeding it the necessary options.
 * DO NOT USE LOGGER IN THIS CLASS. LOGGING HAS NOT BEEN INITIALIZED
 *
 * @author afrechet
 */
public class SATFCFacadeBuilder {

    public static final String SATFC_CLASP_LIBRARY_ENV_VAR = "SATFC_CLASP_LIBRARY";
    private volatile static boolean logInitialized = false;

    // public params
    private boolean initializeLogging;
    private String fLibrary;
    private String fResultFile;
    private SolverChoice fSolverChoice;
    private String serverURL;
    private int parallelismLevel;
    private Level logLevel;
    private String logFileName;
    private boolean cacheResults;
    private DeveloperOptions developerOptions;

    // developer params
    @Builder
    @Data
    public static class DeveloperOptions {
    	private CNFSaverSolverDecorator.ICNFSaver CNFSaver = null;
        private boolean presolve = true;
        private boolean underconstrained = true;
        private boolean decompose = true;
        private SATFCHydraParams hydraParams = null;
        private DataManager dataManager;
    }

    /**
     * Create a SATFCFacadeBuilder with the default parameters - no logging initialized, autodetected clasp library, no saving of CNFs and results.
     */
    public SATFCFacadeBuilder() {
        // public params
        fLibrary = findSATFCLibrary();
        fResultFile = null;
        parallelismLevel = Math.min(SATFCParallelSolverBundle.PORTFOLIO_SIZE, Runtime.getRuntime().availableProcessors());
        fSolverChoice = parallelismLevel >= SATFCParallelSolverBundle.PORTFOLIO_SIZE ? SolverChoice.SATFC_PARALLEL : SolverChoice.SATFC_SEQUENTIAL;
        serverURL = null;
        logLevel = Level.INFO;
        logFileName = "SATFC.log";
        cacheResults = true;
        developerOptions = DeveloperOptions.builder().build();
    }

    /**
     * Some autodetection magic to find clasp library.
     *
     * @return the path to the detected clasp library, null if none found.
     */
    public static String findSATFCLibrary() {
        final String envPath = System.getenv(SATFC_CLASP_LIBRARY_ENV_VAR);
        if (envPath != null) {
            System.out.println("Using path set from env variable " + SATFC_CLASP_LIBRARY_ENV_VAR + ", " + envPath);
            return envPath;
        }

        //Relative path pointing to the clasp .so
        final String relativeLibPath = "clasp" + File.separator + "jna" + File.separator + "libjnaclasp.so";

        //Find the root of the clasp relative path.
        final URL url = SATFCFacadeBuilder.class.getProtectionDomain().getCodeSource().getLocation();
        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            f = new File(url.getPath());
        }
        final String currentLocation;

        if (f.isDirectory()) {
            if (f.getName().equals("bin")) {
                // eclipse
                currentLocation = new File(f.getParentFile(), "src" + File.separator + "dist").getAbsolutePath();
            } else {
                // intellij
                currentLocation = new File(f.getParentFile().getParentFile().getParentFile(), "src" + File.separator + "dist").getAbsolutePath();
            }

        } else {
            //Deployed, probably under the gradle install build structure.
            currentLocation = f.getParentFile().getParentFile().getAbsolutePath();
        }

        final File file = new File(currentLocation + File.separator + relativeLibPath);
        if (file.exists()) {
            System.out.println("Found default library " + file.getAbsolutePath() + ".");
            return file.getAbsolutePath();
        } else {
            System.err.println("Did not find SATFC library at " + file.getAbsolutePath());
        }

        return null;
    }

    /**
     * Build a SATFC facade with the builder's options. These are either the default options (if available), or the ones provided with the
     * builder's setters.
     *
     * @return a SATFC facade configured according to the builder's options.
     */
    public SATFCFacade build() {
        if (fLibrary == null) {
            throw new IllegalArgumentException("Facade builder did not auto-detect default library, and no other library was provided.");
        }
        if (fSolverChoice.equals(SATFC_PARALLEL)) {
            if (parallelismLevel < 4) {
                throw new IllegalArgumentException("Trying to initialize the parallel solver with too few cores! Use the " + SATFC_SEQUENTIAL + " solver instead. We recommend the " + SATFC_PARALLEL + " solver with >= than 4 threads");
            }
        }
        if (initializeLogging) {
            initializeLogging(logLevel, logFileName);
        }
        return new SATFCFacade(
                SATFCFacadeParameter.builder()
                        .claspLibrary(fLibrary)
                        .resultFile(fResultFile)
                        .solverChoice(fSolverChoice)
                        .serverURL(serverURL)
                        .parallelismLevel(parallelismLevel)
                        .cacheResults(cacheResults)
                        // developer
                        .hydraParams(developerOptions.getHydraParams())
                        .presolve(developerOptions.isPresolve())
                        .decompose(developerOptions.isDecompose())
                        .underconstrained(developerOptions.isUnderconstrained())
                        .dataManager(developerOptions.getDataManager())
                        .build()
                        );
    }

    /**
     * Set the (clasp) library SATFC should use.
     *
     * @param aLibrary
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setLibrary(String aLibrary) {
        if (aLibrary == null) {
            throw new IllegalArgumentException("Cannot provide a null library.");
        }
        fLibrary = aLibrary;
        return this;
    }

    /**
     * Set the file in which SATFC writes encountered problem/results pairs.
     *
     * @param aResultFile
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setResultFile(String aResultFile) {
        fResultFile = aResultFile;
        return this;
    }

    /**
     * Set the type of solver choice to use in SATFC.
     *
     * @param aSolverChoice
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setSolverChoice(SolverChoice aSolverChoice) {
        fSolverChoice = aSolverChoice;
        return this;
    }

    /**
     * Set the URL of the SATFCServer. This is only required if you are using the SATFCServer module.
     *
     * @param serverURL
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setServerURL(@NonNull String serverURL) {
        this.serverURL = serverURL;
        return this;
    }

    /**
     * Set the maximum number of solvers that SATFC will execute in parallel
     * This will have little effect past {@link SATFCParallelSolverBundle#PORTFOLIO_SIZE}
     *
     * @param parallelismLevel
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setParallelismLevel(int parallelismLevel) {
        this.parallelismLevel = parallelismLevel;
        return this;
    }


    /**
     * Call this method to have SATFC configure logging (this would only have any effect if the calling application hasn't initialized logging)
     *
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setInitializeLogging(String logFileName, @NonNull Level logLevel) {
        this.initializeLogging = true;
        this.logLevel = logLevel;
        this.logFileName = logFileName;
        return this;
    }

    /**
     * Set whether or not to cache results
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setCacheResults(boolean cacheResults) {
        this.cacheResults = cacheResults;
        return this;
    }

    // Developer methods
    public SATFCFacadeBuilder setDeveloperOptions(DeveloperOptions developerOptions) {
    	this.developerOptions = developerOptions;
    	return this;
    }


    public static SATFCFacade buildFromParameters(@NonNull SATFCFacadeParameters parameters) {
        final SATFCFacadeBuilder builder = new SATFCFacadeBuilder();

        // regular parameters
        if (parameters.fClaspLibrary != null) {
            builder.setLibrary(parameters.fClaspLibrary);
        }
        builder.setParallelismLevel(parameters.numCores);
        builder.setSolverChoice(parameters.fSolverChoice);
        builder.setInitializeLogging(parameters.logFileName, parameters.getLogLevel());
        if (parameters.cachingParams.serverURL != null) {
            builder.setServerURL(parameters.cachingParams.serverURL);
        }
        builder.setCacheResults(parameters.cachingParams.cacheResults);

        CNFSaverSolverDecorator.ICNFSaver CNFSaver = null;
        if (parameters.fSolverChoice.equals(SolverChoice.CNF)) {
            System.out.println("Saving CNFs to disk in " + parameters.fCNFDir);
            CNFSaver = new CNFSaverSolverDecorator.FileCNFSaver(parameters.fCNFDir);
            if (parameters.fRedisParameters.areValid()) {
                System.out.println("Saving CNF index to redis");
                CNFSaver = new CNFSaverSolverDecorator.RedisIndexCNFSaver(CNFSaver, parameters.fRedisParameters.getJedis(), parameters.fRedisParameters.fRedisQueue);
            }
        }
        
        // developer parameters
        builder.setDeveloperOptions(
        		DeveloperOptions
        		.builder()
        		.decompose(parameters.fSolverOptions.decomposition)
        		.presolve(parameters.fSolverOptions.presolve)
        		.underconstrained(parameters.fSolverOptions.underconstrained)
        		.hydraParams(parameters.fHydraParams)
        		.CNFSaver(CNFSaver)
        		.build()
        		);
        return builder.build();
    }


    private static final String LOGBACK_CONFIGURATION_FILE_PROPERTY = "logback.configurationFile";

    public static void initializeLogging(Level logLevel, String logFileName) {
        if (logInitialized) {
            return;
        }
        if (System.getProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY) != null) {
            Logger log = LoggerFactory.getLogger(SATFCFacade.class);
            log.debug("System property for logback.configurationFile has been found already set as {} , logging will follow this file.", System.getProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY));
        } else {
            String logback = Resources.getResource("logback_satfc.groovy").toString();
            System.setProperty("SATFC.root.log.level", logLevel.toString());
            if (logFileName != null) {
                System.setProperty("SATFC.log.filename", logFileName);
            }
            System.setProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY, logback);
            Logger log = LoggerFactory.getLogger(SATFCFacade.class);
            log.debug("Logging initialized to use file: {}", logback);
        }
        logInitialized = true;
    }

}
