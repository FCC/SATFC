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
import java.net.URISyntaxException;
import java.net.URL;

import lombok.NonNull;
import ca.ubc.cs.beta.aeatk.logging.LogLevel;
import ca.ubc.cs.beta.stationpacking.execution.parameters.SATFCFacadeParameters;
import ca.ubc.cs.beta.stationpacking.facade.SATFCFacadeParameter.SolverChoice;
import ca.ubc.cs.beta.stationpacking.solvers.decorators.CNFSaverSolverDecorator;

/**
 * Builder in charge of creating a SATFC facade, feeding it the necessary options.
 * DO NOT USE LOGGER IN THIS CLASS. LOGGING HAS NOT BEEN INITIALIZED
 *
 * @author afrechet
 */
public class SATFCFacadeBuilder {

    public static final String SATFC_CLASP_LIBRARY_ENV_VAR = "SATFC_CLASP_LIBRARY";
    private boolean fPresolve;
    private boolean fUnderconstrained;
    private boolean fDecompose;
    private boolean fInitializeLogging;
    private String fLibrary;
    private String fResultFile;
    private SATFCFacadeParameter.SolverChoice fSolverChoice;
    private String serverURL;
    private CNFSaverSolverDecorator.ICNFSaver CNFSaver;
    private int parallelismLevel;
    private LogLevel logLevel;

    /**
     * Create a SATFCFacadeBuilder with the default parameters - no logging initialized, autodetected clasp library, no saving of CNFs and results.
     */
    public SATFCFacadeBuilder() {
        fInitializeLogging = false;
        fLibrary = findSATFCLibrary();
        fResultFile = null;
        parallelismLevel = Runtime.getRuntime().availableProcessors();
        fSolverChoice = parallelismLevel >= 4 ? SolverChoice.SATFC_PARALLEL : SolverChoice.SATFC_SEQUENTIAL;
        fPresolve = true;
        fUnderconstrained = true;
        fDecompose = true;
        serverURL = null;
        logLevel = LogLevel.INFO;
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
        if (fSolverChoice.equals(SolverChoice.SATFC_PARALLEL)) {
            if (parallelismLevel < 4) {
                throw new IllegalArgumentException("Trying to initialize the parallel solver with too few cores! Use the " + SolverChoice.SATFC_SEQUENTIAL + " solver instead. We recommend the " + SolverChoice.SATFC_PARALLEL + " solver with >= than 4 threads");
            }
        }
        return new SATFCFacade(new SATFCFacadeParameter(
                fLibrary,
                fInitializeLogging,
                fResultFile,
                fSolverChoice,
                fPresolve,
                fUnderconstrained,
                fDecompose,
                CNFSaver,
                serverURL,
                parallelismLevel,
                logLevel
        ));
    }

    /**
     * Set whether SATFC should initialize the logging on construction.
     *
     * @param aInitializeLogging
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setInitializeLogging(boolean aInitializeLogging) {
        fInitializeLogging = aInitializeLogging;
        return this;
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
    public SATFCFacadeBuilder setSolverChoice(SATFCFacadeParameter.SolverChoice aSolverChoice) {
        fSolverChoice = aSolverChoice;
        return this;
    }

    public SATFCFacadeBuilder setPresolve(boolean presolve) {
        this.fPresolve = presolve;
        return this;
    }

    public SATFCFacadeBuilder setUnderconstrained(boolean underconstrained) {
        this.fUnderconstrained = underconstrained;
        return this;
    }

    public SATFCFacadeBuilder setDecompose(boolean decompose) {
        this.fDecompose = decompose;
        return this;
    }

    public SATFCFacadeBuilder setCNFSaver(@NonNull CNFSaverSolverDecorator.ICNFSaver CNFSaver) {
        this.CNFSaver = CNFSaver;
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
     *
     * @param parallelismLevel
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setParallelismLevel(int parallelismLevel) {
        this.parallelismLevel = parallelismLevel;
        return this;
    }
    
    /**
     * Set the log level for SATFC to use. Use in conjunction with {@link #setInitializeLogging(boolean)}
     *
     * @param logLevel
     * @return this {@code Builder} object
     */
    public SATFCFacadeBuilder setLogLevel(LogLevel logLevel) {
    	this.logLevel = logLevel;
    	return this;
    }

    public static SATFCFacade buildFromParameters(@NonNull SATFCFacadeParameters parameters) {

        final SATFCFacadeBuilder builder = new SATFCFacadeBuilder();

        if (parameters.fClaspLibrary != null) {
            builder.setLibrary(parameters.fClaspLibrary);
        }
        builder.setParallelismLevel(parameters.numCores);
        builder.setInitializeLogging(true);
        builder.setSolverChoice(parameters.fSolverChoice);
        builder.setDecompose(parameters.fSolverOptions.decomposition);
        builder.setUnderconstrained(parameters.fSolverOptions.underconstrained);
        builder.setPresolve(parameters.fSolverOptions.presolve);
        builder.setLogLevel(parameters.fLoggingOptions.logLevel);
        if (parameters.fSolverOptions.cachingParams.serverURL != null) {
            builder.setServerURL(parameters.fSolverOptions.cachingParams.serverURL);
        }
        if (parameters.fSolverChoice.equals(SolverChoice.CNF)) {
            System.out.println("Saving CNFs to disk in " + parameters.fCNFDir);
            CNFSaverSolverDecorator.ICNFSaver CNFSaver = new CNFSaverSolverDecorator.FileCNFSaver(parameters.fCNFDir);
            if (parameters.fRedisParameters.areValid()) {
                System.out.println("Saving CNF index to redis");
                CNFSaver = new CNFSaverSolverDecorator.RedisIndexCNFSaver(CNFSaver, parameters.fRedisParameters.getJedis(), parameters.fRedisParameters.fRedisQueue);
            }
            builder.setCNFSaver(CNFSaver);
        }
        return builder.build();
    }

}
