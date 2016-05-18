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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aeatk.logging.ConsoleOnlyLoggingOptions;
import ca.ubc.cs.beta.aeatk.logging.LoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml.EncodingType;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

/**
 * In charge of converting different station repacking instance formats to other formats (either .sprk, or SAT/MIP encodings).
 * Intended for internal use only
 *
 * @author afrechet
 */
public class Converter {

    private static Logger log;

    private static enum OutType {
        INSTANCE,
        CNF
    }

    @UsageTextField(title = "Converter Parameters", description = "Parameters needed to convert station packing instances.")
    private static class ConverterParameters extends AbstractOptions {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Parameter(names = "--instance", description = "The instances to convert. Can be a single instance file (.qstn for a question file, .sql for a sql instance, .srpk for a station repacking instance), a folder containing a bunch of files, or a .txt/.csv file containing a list of files, or any composition of the previous.")
        List<String> fInstanceNames = null;

        @Parameter(names = "--interference-folder", description = "Interference folder associated with the given instances (required for instances that do not specify interference folders).")
        String fInterferenceFolder = null;

        @Parameter(names = "--interference-folder-prefix", description = "Prefix to add to any interference folder, usually a path to a directory containing all interference folders.")
        String fInterferenceFolderPrefix = null;

        @UsageTextField(defaultValues = "<current directory>")
        @Parameter(names = "--out-directory", description = "Folder where to write converted instance")
        String fOutDirectory = null;

        @Parameter(names = "--out-type", description = "what to convert instances to.")
        OutType fOutType = OutType.INSTANCE;

        @ParametersDelegate
        private LoggingOptions fLoggingOptions = new ConsoleOnlyLoggingOptions();

    }

    /**
     * @param aInstanceNames - list of instance names, either instance file names (.qstn, .sql, .srpk), folder containing instances or instance lists (.txt, .csv).
     * @return a list of proper instance files.
     */
    private static List<String> getInstancesFilenames(List<String> aInstanceNames) {

        final List<String> instancesFilenames = new ArrayList<String>();

        final Queue<String> instanceNames = new LinkedList<String>(aInstanceNames);

        while (!instanceNames.isEmpty()) {
            log.debug("Still {} possible instances to add...", instanceNames.size());
            final String instanceName = instanceNames.remove();
            final File instanceFile = new File(instanceName);

            if (!instanceFile.exists()) {
                throw new ParameterException("Provided instance name " + instanceName + " is not a file/directory that exists.");
            }
            if (instanceFile.isDirectory()) {
                log.debug("Adding all the instances from the directory {} ...", instanceFile.getAbsolutePath());
                final File[] subFiles = instanceFile.listFiles();

                for (final File subFile : subFiles) {
                    instanceNames.add(subFile.getPath());
                }
                log.debug("Added {} instances.", subFiles.length);
            } else {
                final String extension = FilenameUtils.getExtension(instanceName);
                switch (extension) {
                    case "txt":
                        log.debug("Adding all the instances listed in the file {} ...", instanceFile.getAbsolutePath());
                        int count = 0;
                        try {
                            try (final BufferedReader br = new BufferedReader(new FileReader(instanceFile));) {
                                String line;
                                while ((line = br.readLine()) != null) {
                                    instanceNames.add(line);
                                    count++;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new ParameterException("Could not read instances from instance list file " + instanceName + " (" + e.getMessage() + ").");
                        }
                        log.debug("Added {} instances.", count);

                        break;
                    case "sql":
                    case "srpk":
                        instancesFilenames.add(instanceName);
                        break;
                    default:
                        log.warn("Unrecognized instance extension {} for file in provided instance {}. Skipping instance.", extension, instanceName);
                }
            }
        }
        return instancesFilenames;
    }

    /**
     * Station packing problem specs that contains the necessary information to instantiate a station packing problem.
     *
     * @author afrechet
     */
    @Data
    @AllArgsConstructor
    public static class StationPackingProblemSpecs {
        private String source;

        @NonNull
        private final Map<Integer, Set<Integer>> domains;

        private Map<Integer, Integer> previousAssignment;

        @NonNull
        private final String dataFoldername;

        private Double cutoff;

        public StationPackingProblemSpecs(Map<Integer, Set<Integer>> domains, Map<Integer, Integer> previousAssignment, String dataFoldername) {
            this.dataFoldername = dataFoldername;
            this.domains = domains;
            this.previousAssignment = previousAssignment;
        }


        /**
         * @param aSQLInstanceFilename - a sql instance filename. Taken from MYSQLDBTAE additional run data from auction simulator runs.
         * @return a station packing problem spec from the SQL instance.
         * @throws IOException
         */
        public static StationPackingProblemSpecs fromSQL(String aSQLInstanceFilename) throws IOException {

            List<String> lines = FileUtils.readLines(new File(aSQLInstanceFilename));
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("SQL instance file " + aSQLInstanceFilename + " is empty.");
            } else if (lines.size() > 1) {
                log.warn("SQL instance file {} has more than one lines ({}), but only the first line will be read.", aSQLInstanceFilename, lines.size());
            }

            String sqlInstanceString = lines.get(0);

            final Map<Integer, Set<Integer>> stationID_domains = new HashMap<Integer, Set<Integer>>();
            final Map<Integer, Integer> previous_assignmentID = new HashMap<Integer, Integer>();
            final String config_foldername;

            String[] encoded_instance_parts = sqlInstanceString.split("_");

            if (encoded_instance_parts.length == 0) {
                throw new IllegalArgumentException("Unparseable encoded instance string \"" + sqlInstanceString + "\".");
            }

            config_foldername = encoded_instance_parts[0];

            //Get problem info.
            for (int i = 1; i < encoded_instance_parts.length; i++) {
                Integer station;
                Integer previousChannel;
                Set<Integer> domain;

                String station_info_string = encoded_instance_parts[i];
                String[] station_info_parts = station_info_string.split(";");

                String station_string = station_info_parts[0];
                try {
                    station = Integer.parseInt(station_string);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Unparseable station info \"" + station_info_string + "\" (station ID " + station_string + " is not an integer).");
                }

                String previous_channel_string = station_info_parts[1];
                try {
                    previousChannel = Integer.parseInt(previous_channel_string);
                    if (previousChannel <= 0) {
                        previousChannel = null;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Unparseable station info \"" + station_info_string + "\" (previous channel " + previous_channel_string + " is not an integer).");
                }


                domain = new HashSet<Integer>();
                String channels_string = station_info_parts[2];
                String[] channels_parts = channels_string.split(",");
                for (String channel_string : channels_parts) {
                    try {
                        domain.add(Integer.parseInt(channel_string));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException("Unparseable station info \"" + station_info_string + "\" (domain channel " + channel_string + " is not an integer).");
                    }
                }


                stationID_domains.put(station, domain);
                if (previousChannel != null) {
                    previous_assignmentID.put(station, previousChannel);
                }
            }

            return new StationPackingProblemSpecs(aSQLInstanceFilename, stationID_domains, previous_assignmentID, config_foldername, null);
        }

        /**
         * @param aStationRepackingInstanceFilename - a srpk instance filename.
         * @return a station repacking instance taken from the srpk file.
         * @throws IOException
         */
        public static StationPackingProblemSpecs fromStationRepackingInstance(String aStationRepackingInstanceFilename) throws IOException {
            Map<Integer, Set<Integer>> domains = new HashMap<Integer, Set<Integer>>();
            Map<Integer, Integer> previousAssignment = new HashMap<Integer, Integer>();
            String configFoldername = null;
            Double cutoff = null;


            List<String> lines = FileUtils.readLines(new File(aStationRepackingInstanceFilename));
            for (String line : lines) {
                final String[] lineParts = line.split(",");

                String key = lineParts[0];
                switch (key) {
                    case "INTERFERENCE":
                        configFoldername = lineParts[1];
                        break;
                    case "CUTOFF":
                        cutoff = Double.valueOf(lineParts[1]);
                        break;
                    case "SOURCE":
                        break;
                    default:
                        try {
                            Integer stationID = Integer.valueOf(lineParts[0]);

                            Integer previousChannel = Integer.valueOf(lineParts[1]);
                            if (previousChannel > 0) {
                                previousAssignment.put(stationID, previousChannel);
                            }

                            Set<Integer> domain = new HashSet<Integer>();
                            for (int i = 2; i < lineParts.length; i++) {
                                Integer channel = Integer.valueOf(lineParts[i]);
                                domain.add(channel);
                            }
                            domains.put(stationID, domain);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            throw new IllegalArgumentException("Unparseable station info \"" + line + "\" (could not cast one of the components to integer).");
                        }
                }
            }
            return new StationPackingProblemSpecs(aStationRepackingInstanceFilename, domains, previousAssignment, configFoldername, cutoff);
        }

    }

    /**
     * @param aInstanceFilename - a station repacking instance file.
     * @return a station repacking problem spec from the given instance file.
     */
    private static StationPackingProblemSpecs getStationPackingProblemSpecs(String aInstanceFilename) {
        final String extension = FilenameUtils.getExtension(aInstanceFilename);


        final StationPackingProblemSpecs specs;
        switch (extension) {
            case "sql":
                try {
                    specs = StationPackingProblemSpecs.fromSQL(aInstanceFilename);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Could not read lines from SQL instance file " + aInstanceFilename + ".");
                }
                break;
            case "srpk":
                try {
                    specs = StationPackingProblemSpecs.fromStationRepackingInstance(aInstanceFilename);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Could not convert station repacking instance " + aInstanceFilename + " (" + e.getMessage() + ").");
                }

                break;
            default:
                throw new IllegalArgumentException("Unsupported instance " + aInstanceFilename + " with extension " + extension + ".");
        }
        return specs;
    }


    public static void main(String[] args) {

        ConverterParameters parameters = new ConverterParameters();
        JCommanderHelper.parseCheckingForHelpAndVersion(args, parameters);
        parameters.fLoggingOptions.initializeLogging();
        log = LoggerFactory.getLogger(Converter.class);

		/*
		 * Gather all instances.
		 */
        log.debug("Gathering all instance names...");
        if (parameters.fInstanceNames == null) {
            throw new ParameterException("Must specify at least one instance.");
        }
        List<String> problemFilenames = getInstancesFilenames(parameters.fInstanceNames);
		
		/*
		 * Convert the instances.
		 */
        log.debug("Converting the instances ...");
        final DataManager dataManager = new DataManager();
        final Map<String, ISATEncoder> satEncoders = new HashMap<String, ISATEncoder>();

        String outputDir = parameters.fOutDirectory != null ? parameters.fOutDirectory : "";

        OutType outType = parameters.fOutType;
        int i = 0;
        for (String problemFilename : problemFilenames) {
            if (i % 50 == 0) {
                System.gc();
            }
            log.debug("Reading in instance {}/{}.", i++, problemFilenames.size());

            StationPackingProblemSpecs spec = getStationPackingProblemSpecs(problemFilename);

            final String source = spec.getSource();

            String configFoldername;
            if (spec.getDataFoldername() != null) {
                configFoldername = spec.getDataFoldername();
            } else if (parameters.fInterferenceFolder == null) {
                throw new IllegalArgumentException("Instance " + source + " does not specify interference config folder, and the latter hasn't been specified as an option.");
            } else {
                configFoldername = parameters.fInterferenceFolder;
            }

            //Append prefix
            if (parameters.fInterferenceFolderPrefix != null) {
                configFoldername = new File(parameters.fInterferenceFolderPrefix, configFoldername).toString();
            }

            //Load in the interference data.
            log.debug("Loading in interference data from {} ...", configFoldername);
            ManagerBundle bundle;
            try {
                bundle = dataManager.getData(configFoldername);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Could not load in interference data (" + e.getMessage() + ").");
            }
            final IStationManager stationManager = bundle.getStationManager();
            final IConstraintManager constraintManager = bundle.getConstraintManager();

            final Map<Station, Set<Integer>> domains = new HashMap<Station, Set<Integer>>();
            for (Entry<Integer, Set<Integer>> entryDomains : spec.getDomains().entrySet()) {
                domains.put(stationManager.getStationfromID(entryDomains.getKey()), entryDomains.getValue());
            }

            Map<Station, Integer> previousAssignment = null;
            if (spec.getPreviousAssignment() != null) {
                previousAssignment = new HashMap<Station, Integer>();
                for (Entry<Integer, Integer> entryPreviousAssignment : spec.getPreviousAssignment().entrySet()) {
                    previousAssignment.put(stationManager.getStationfromID(entryPreviousAssignment.getKey()), entryPreviousAssignment.getValue());
                }
            }

            final StationPackingInstance instance;
            if (previousAssignment == null) {
                instance = new StationPackingInstance(domains);
            } else {
                instance = new StationPackingInstance(domains, previousAssignment);
            }

            switch (outType) {
                case INSTANCE:
                    List<String> lines = new ArrayList<String>();
                    lines.add("SOURCE," + source);
                    lines.add("INTERFERENCE," + configFoldername);
                    if (spec.getCutoff() != null) {
                        lines.add("CUTOFF," + spec.getCutoff());
                    }

                    List<Station> stations = new ArrayList<Station>(instance.getStations());
                    Collections.sort(stations);

                    for (Station station : stations) {
                        List<Integer> domain = new ArrayList<Integer>(instance.getDomains().get(station));
                        Collections.sort(domain);

                        Integer previousChannel = instance.getPreviousAssignment().get(station);
                        if (previousChannel == null || previousChannel < 0) {
                            previousChannel = -1;
                        }

                        lines.add(station.getID() + "," + previousChannel + "," + StringUtils.join(domain, ","));
                    }

                    String instanceFilename = FilenameUtils.concat(outputDir, FilenameUtils.getBaseName(source) + ".srpk");
                    File instanceFile = new File(instanceFilename);
                    if (instanceFile.exists()) {
                        throw new IllegalStateException("Instance file already exists with name \"" + instanceFilename + "\".");
                    }
                    try {
                        FileUtils.writeLines(instanceFile, lines);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new IllegalStateException("Could not write instance to file.");
                    }

                    break;
                case CNF:
                    final ISATEncoder satEncoder;
                    if (satEncoders.containsKey(configFoldername)) {
                        satEncoder = satEncoders.get(configFoldername);
                    } else {
                        satEncoder = new SATCompressor(constraintManager, EncodingType.DIRECT);
                        satEncoders.put(configFoldername, satEncoder);
                    }
                    log.debug("Converting instance from {} to CNF ...", source);
                    Pair<CNF, ISATDecoder> satEncoding = satEncoder.encode(instance);
                    CNF cnf = satEncoding.getFirst();

                    List<Integer> sortedStationIDs = new ArrayList<Integer>();
                    for (Station station : instance.getStations()) {
                        sortedStationIDs.add(station.getID());
                    }
                    Collections.sort(sortedStationIDs);
                    List<Integer> sortedAllChannels = new ArrayList<Integer>(instance.getAllChannels());
                    Collections.sort(sortedAllChannels);
                    String[] aComments = new String[]{
                            "FCC Feasibility Checking Instance",
                            "Original Instance File: " + source,
                            "Interference folder: " + configFoldername,
                            "Channels: " + StringUtils.join(sortedAllChannels, ","),
                            "Stations: " + StringUtils.join(sortedStationIDs, ",")};

                    String aCNFFilename = FilenameUtils.concat(outputDir, FilenameUtils.getBaseName(source) + ".cnf");
                    File cnfFile = new File(aCNFFilename);
                    if (cnfFile.exists()) {
                        log.warn("CNF file already exists with name \"" + cnfFile + "\".");
                    }
                    try {
                        FileUtils.writeStringToFile(cnfFile, cnf.toDIMACS(aComments));
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new IllegalStateException("Could not write CNF to file.");
                    }

                    break;
                default:
                    throw new ParameterException("Unrecognized out type " + outType + ".");
            }
        }

    }


}
