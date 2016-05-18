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
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.io.Resources;

import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverChoice;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.ConfigFile;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;
import ch.qos.logback.classic.Level;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Builder;

/**
 * Builder in charge of creating a SATFC facade, feeding it the necessary options.
 * DO NOT USE LOGGER IN THIS CLASS. LOGGING HAS NOT BEEN INITIALIZED
 *
 * @author afrechet
 */
public class SATFCFacadeBuilder {

    private volatile static boolean logInitialized = false;

    // public params
    private boolean initializeLogging;
    private String fClaspLibrary;
    private String fSATensteinLibrary;
    private String fResultFile;
    private String serverURL;
    private Level logLevel;
    private String logFileName;
    private int numServerAttempts;
    private boolean noErrorOnServerUnavailable;
    private ConfigFile configFile;
    private DeveloperOptions developerOptions;
    private AutoAugmentOptions autoAugmentOptions;

    /**
     * Set the YAML file used to build up the SATFC solver bundle
     * @param configFile path to a config file
     */
    public SATFCFacadeBuilder setConfigFile(String configFile) {
        this.configFile = new ConfigFile(configFile, false);
        return this;
    }

    /**
     * Set the YAML file used to build up the SATFC solver bundle
     * @param configFile
     */
    public SATFCFacadeBuilder setConfigFile(InternalSATFCConfigFile configFile) {
        this.configFile = new ConfigFile(configFile.getFilename(), true);
        return this;
    }

    // developer params
    @Builder
    @Data
    public static class DeveloperOptions {
    	private CNFSaverSolverDecorator.ICNFSaver CNFSaver;
        private DataManager dataManager;
        private SolverChoice solverChoice;
    }

    /**
     * Keeps track of the locations of the different shared libraries used by SATFC.
     * Adding pathing for a new library is as simple as one line of code.
     * @author pcernek
     */
    public enum SATFCLibLocation {
        CLASP ("SATFC_CLASP_LIBRARY", "clasp" + File.separator + "jna" + File.separator + "libjnaclasp.so"),
        SATENSTEIN ("SATFC_SATENSTEIN_LIBRARY", "satenstein" + File.separator + "jna" + File.separator + "libjnasatenstein.so");

        /**
         * The name of the environment variable that the user may set to specify the library location.
         */
        public final String SATFC_ENV_VAR;

        /**
         * The relative path to the .so file. This location is determined by the library's compile.sh script.
         */
        public final String relativePath;

        SATFCLibLocation(String aENV_VAR, String aRelativePath) {
            this.SATFC_ENV_VAR = aENV_VAR;
            this.relativePath = aRelativePath;
        }
    }

    /**
     * Create a SATFCFacadeBuilder with the default parameters - no logging initialized, autodetected clasp library, no saving of CNFs and results.
     */
    public SATFCFacadeBuilder() {
        // public params
        fClaspLibrary = findSATFCLibrary(SATFCLibLocation.CLASP);
        fSATensteinLibrary = findSATFCLibrary(SATFCLibLocation.SATENSTEIN);
        fResultFile = null;
        serverURL = null;
        logLevel = Level.INFO;
        logFileName = "SATFC.log";
        configFile = autoDetectBundle();
        numServerAttempts = 3;
        noErrorOnServerUnavailable = false;
        autoAugmentOptions = AutoAugmentOptions.builder().build();
        developerOptions = DeveloperOptions.builder().solverChoice(SolverChoice.YAML).build();
    }

    public static ConfigFile autoDetectBundle() {
        return new ConfigFile(Runtime.getRuntime().availableProcessors() >= 4 ? InternalSATFCConfigFile.SATFC_PARALLEL.getFilename() : InternalSATFCConfigFile.SATFC_SEQUENTIAL.getFilename(), true);
    }

    /**
     * Some autodetection magic to find libraries used by SATFC.
     *
     * @return the path detected for the given library, null if none found.
     * @param lib
     */
    public static String findSATFCLibrary(SATFCLibLocation lib) {
        final String envPath = System.getenv(lib.SATFC_ENV_VAR);
        if (envPath != null) {
            System.out.println("Using path set from env variable " + lib.SATFC_ENV_VAR + ", " + envPath);
            return envPath;
        }

        //Relative path pointing to the clasp .so
        final String relativeLibPath = lib.relativePath;

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
        if (fClaspLibrary == null || fSATensteinLibrary == null) {
            throw new IllegalArgumentException("Facade builder did not auto-detect default library, and no other library was provided.");
        }
        if (developerOptions.getSolverChoice().equals(SolverChoice.YAML)) {
            Preconditions.checkNotNull(configFile, "No YAML config file was given to initialize the solver bundle with!");
        }
        if (initializeLogging) {
            initializeLogging(logLevel, logFileName);
        }
        return new SATFCFacade(
                SATFCFacadeParameter.builder()
                        .claspLibrary(fClaspLibrary)
                        .satensteinLibrary(fSATensteinLibrary)
                        .resultFile(fResultFile)
                        .serverURL(serverURL)
                        .configFile(configFile)
                        .numServerAttempts(numServerAttempts)
                        .noErrorOnServerUnavailable(noErrorOnServerUnavailable)
                        .autoAugmentOptions(autoAugmentOptions)
                        // developer
                        .dataManager(developerOptions.getDataManager())
                        .CNFSaver(developerOptions.getCNFSaver())
                        .solverChoice(developerOptions.getSolverChoice())
                        .build()
                        );
    }

    /**
     * Set the clasp library SATFC should use.
     *
     * @param aLibrary
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setClaspLibrary(String aLibrary) {
        if (aLibrary == null) {
            throw new IllegalArgumentException("Cannot provide a null clasp library.");
        }
        fClaspLibrary = aLibrary;
        return this;
    }

    /**
     * Set the SATenstein library SATFC should use.
     *
     * @param aLibrary
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setSATensteinLibrary(String aLibrary) {
        if (aLibrary == null) {
            throw new IllegalArgumentException("Cannot provide a null SATenstein library.");
        }
        fSATensteinLibrary = aLibrary;
        return this;
    }

    /**
     * Set the file in which SATFC writes encountered problem/results pairs.
     *
     * @param aResultFile
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setResultFile(@NonNull String aResultFile) {
        fResultFile = aResultFile;
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
     * Set the number of attempts to retry a connection to the SATFCServer per query before giving up and continuing without the server's help (or throwing an error)
     * @param numServerAttempts number of times to attempt the server for a given query
     * @param noErrorOnServerUnavailable if true, just continue solving the problem without the server's help. if false, throw an error after numServerAttempts is exceeded
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setServerAttempts(int numServerAttempts, boolean noErrorOnServerUnavailable) {
        Preconditions.checkState(numServerAttempts > 0, "number of server attempts must be positive");
        this.numServerAttempts = numServerAttempts;
        this.noErrorOnServerUnavailable = noErrorOnServerUnavailable;
        return this;
    }

    /**
     * Call this method to have SATFC configure logging (this would only have any effect if the calling application hasn't initialized logging)
     *
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder initializeLogging(String logFileName, @NonNull Level logLevel) {
        this.initializeLogging = true;
        this.logLevel = logLevel;
        this.logFileName = logFileName;
        return this;
    }

    /**
     * Call this method to have SATFC configure logging (this would only have any effect if the calling application hasn't initialized logging)
     *
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder initializeLogging(@NonNull Level logLevel) {
        return initializeLogging(null, logLevel);
    }

    /**
     */
    public SATFCFacadeBuilder setAutoAugmentOptions(@NonNull AutoAugmentOptions autoAugmentOptions) {
        this.autoAugmentOptions = autoAugmentOptions;
        return this;
    }

    // Developer methods
    public SATFCFacadeBuilder setDeveloperOptions(@NonNull DeveloperOptions developerOptions) {
    	this.developerOptions = developerOptions;
    	return this;
    }


    public static SATFCFacadeBuilder builderFromParameters(@NonNull SATFCFacadeParameters parameters) {
        final SATFCFacadeBuilder builder = new SATFCFacadeBuilder();
        // regular parameters
        if (parameters.fClaspLibrary != null) {
            builder.setClaspLibrary(parameters.fClaspLibrary);
        }
        if (parameters.fSATensteinLibrary != null) {
            builder.setSATensteinLibrary(parameters.fSATensteinLibrary);
        }
        if (parameters.configFile != null) {
            builder.setConfigFile(parameters.configFile);
        }
        builder.initializeLogging(parameters.logFileName, parameters.getLogLevel());
        if (parameters.cachingParams.serverURL != null) {
            builder.setServerURL(parameters.cachingParams.serverURL);
        }

        CNFSaverSolverDecorator.ICNFSaver CNFSaver = null;
        if (parameters.fCNFDir != null) {
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
        		.CNFSaver(CNFSaver)
        		.solverChoice(parameters.solverChoice)
        		.build()
        		);
        return builder;
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