/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Kevin Leyton-Brown.
 *
 * This file is part of satfc.
 *
 * satfc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * satfc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with satfc.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.execution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.ManagerBundle;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATCompressor;

import com.google.common.collect.Sets;

/**
 * Utility that converts an encoded instance to a CNF.
 * @author afrechet
 */
public class EncodedInstanceToCNFConverter {
    
    private static final Logger log = LoggerFactory.getLogger(EncodedInstanceToCNFConverter.class);
    
    private static final String USAGE = "java -jar EncodedInstanceToCNFConverter.java <interference config folder> <encoded instance file> <output folder>\n"
            + "Transforms an encoded instance (see how SATFC TAE writes instances as instance specific information) to a CNF.\n"
            + "<interference config folder> -- where to look for interference config data.\n"
            + "<encoded instance file> -- file containing each encoded instance on a different line.\n"
            + "<output folder> -- where to save the CNF.";
    
    /**
     * @param aSQLInstanceString - a SQL instance string (special encoding used to convert station packing instance to string).
     * @param aInterferenceConfigFoldername - config foldername.
     * @param aDataManager - a data manager (usually corresponding to the aInterferenceConfigFoldername, we provide both because the string config foldername is actually needed).
     * @return a station packing instance with its corresponding interference config folder name.
     */
    public static Pair<StationPackingInstance,String> getInstanceFromSQLString(String aSQLInstanceString, String aInterferenceConfigFoldername, DataManager aDataManager)
    {
        /*
         * Get data from instance.
         */
        Map<Integer,Set<Integer>> stationID_domains = new HashMap<Integer,Set<Integer>>();
        Map<Integer,Integer> previous_assignmentID = new HashMap<Integer,Integer>();
        String config_foldername;
        
        String[] encoded_instance_parts = aSQLInstanceString.split("_");
        
        if(encoded_instance_parts.length == 0)
        {
            throw new IllegalArgumentException("Unparseable encoded instance string \""+aSQLInstanceString+"\".");
        }
        
        //Get the config folder name.
        config_foldername = aInterferenceConfigFoldername + File.separator + encoded_instance_parts[0];
        
        //Get problem info.
        for(int i=1;i<encoded_instance_parts.length;i++)
        {
            Integer station;
            Integer previousChannel;
            Set<Integer> domain;
            
            String station_info_string = encoded_instance_parts[i];
            String[] station_info_parts = station_info_string.split(";");
            
            String station_string = station_info_parts[0];
            if(isInteger(station_string))
            {
                station = Integer.parseInt(station_string);
            }
            else
            {
                throw new IllegalArgumentException("Unparseable station info \""+station_info_string+"\" (station ID "+station_string+" is not an integer).");
            }
            
            String previous_channel_string = station_info_parts[1];
            if(isInteger(previous_channel_string))
            {
                previousChannel = Integer.parseInt(previous_channel_string);
                if(previousChannel <= 0)
                {
                    previousChannel = null;
                }
            }
            else
            {
                throw new IllegalArgumentException("Unparseable station info \""+station_info_string+"\" (previous channel "+previous_channel_string+" is not an integer).");
            }
            
            
            domain = new HashSet<Integer>();
            String channels_string = station_info_parts[2];
            String[] channels_parts = channels_string.split(",");
            for(String channel_string : channels_parts)
            {
                if(isInteger(channel_string))
                {
                    domain.add(Integer.parseInt(channel_string));
                }
                else
                {
                    throw new IllegalArgumentException("Unparseable station info \""+station_info_string+"\" (domain channel "+channel_string+" is not an integer).");
                }
            }
            
            
            stationID_domains.put(station, domain);
            if(previousChannel != null)
            {
                previous_assignmentID.put(station, previousChannel);
            }
        }
        
        /*
         * Validate instance data.
         */
        File config_folder = new File(config_foldername);
        if(!config_folder.exists())
        {
            throw new IllegalArgumentException("Encoded instance's interference config folder \""+config_foldername+"\" does not exist.");
        }
        else if(!config_folder.isDirectory())
        {
            throw new IllegalArgumentException("Encoded instance's interference config folder \""+config_foldername+"\" is not a directory.");
        }
        
        if(!stationID_domains.keySet().containsAll(previous_assignmentID.keySet()))
        {
            log.warn("Encoded instance's previous assignment contains stations not in the indicated domains.");
        }
        
        /*
         * Construct station packing instances. 
         */
        
        ManagerBundle data_bundle;
        try {
            data_bundle = aDataManager.getData(config_foldername);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not load interference data from \""+config_foldername+"\".");
        }
        IStationManager station_manager = data_bundle.getStationManager();
       
        Map<Station,Set<Integer>> domains = new HashMap<Station,Set<Integer>>();
        for(Entry<Integer,Set<Integer>> stationID_domains_entry : stationID_domains.entrySet())
        {
            Integer stationID = stationID_domains_entry.getKey();
            Set<Integer> domain = stationID_domains_entry.getValue();
            
            Station station = station_manager.getStationfromID(stationID);
            
            Set<Integer> validDomain = station_manager.getDomain(station);
            if(!validDomain.containsAll(domain))
            {
                //log.warn("Domain {} of station {} does not contain all stations specified in problem domain {}.",truedomain,stationID,domain);
            }
            
            domains.put(station, Sets.intersection(domain, validDomain));
        }
        
        Map<Station,Integer> previous_assignment = new HashMap<Station,Integer>();
        for(Entry<Integer,Integer> previous_assignmentID_entry : previous_assignmentID.entrySet())
        {
            Integer stationID = previous_assignmentID_entry.getKey();
            Integer previous_channel = previous_assignmentID_entry.getValue();
            
            Station station = station_manager.getStationfromID(stationID);
            
            Set<Integer> truedomain = station_manager.getDomain(station);
            if(!truedomain.contains(previous_channel))
            {
                log.warn("Domain {} of station {} does not contain previous assigned channel {}.",truedomain,stationID,previous_channel);
            }
            
            previous_assignment.put(station, previous_channel);
            
        }
        
        StationPackingInstance instance = new StationPackingInstance(domains,previous_assignment);
        
        return new Pair<StationPackingInstance,String>(instance,config_foldername);
    }
    
    /**
     * Converts an (SQL) encoded instance to CNF.
     * @param args - see static <code>USAGE</code> string.
     */
    public static void main(String[] args) {
        
        //Static objects used throughout the method.
        final DataManager data_manager = new DataManager();
        final Map<ManagerBundle,ISATEncoder> SATencoders = new HashMap<ManagerBundle,ISATEncoder>();
        
        
      //Check for help or invalid number of arguments.
        boolean needHelp = false;
        for(String arg : args)
        {
            if(arg.equals("-h") || arg.equals("-help") || arg.equals("--help") || arg.equals("--h"))
            {
                needHelp = true;
                break;
            }
        }
        if(needHelp || args.length!=3)
        {
            if(args.length != 3)
            {
                System.out.println("Invalid number of arguments: \""+Arrays.toString(args)+"\".");
            }
            
            System.out.println(USAGE);
            return;
        }
        
        
        /*
         * Get arguments.
         */
        
        String interference_config_foldername = args[0];
        String encoded_instance_filename = args[1];
        String output_foldername = args[2];
        
        /*
         * Validate arguments.
         */
        File interference_config_folder = new File(interference_config_foldername);
        if(!interference_config_folder.exists())
        {
            throw new IllegalArgumentException("Provided interference config folder \""+interference_config_foldername+"\" does not exist.");
        }
        else if(!interference_config_folder.isDirectory())
        {
            throw new IllegalArgumentException("Provided interference config folder \""+interference_config_foldername+"\" is not a directory.");
        }
        
        File encoded_instance_file = new File(encoded_instance_filename);
        if(!encoded_instance_file.exists())
        {
            throw new IllegalArgumentException("Provided encoded instance file \""+encoded_instance_file+"\" does not exist.");
        }
        if(!encoded_instance_file.isFile())
        {
            throw new IllegalArgumentException("Provided encoded instance file \""+encoded_instance_file+"\" is not a file.");
        }
        
        File output_folder = new File(output_foldername);
        if(!output_folder.exists())
        {
            throw new IllegalArgumentException("Provided output folder \""+output_foldername+"\" does not exist.");
        }
        else if(!output_folder.isDirectory())
        {
            throw new IllegalArgumentException("Provided output folder \""+output_foldername+"\" is not a directory.");
        }
        
        log.debug("Reading instance from {} ...",encoded_instance_filename);
        try(CSVReader reader = new CSVReader(new FileReader(encoded_instance_filename), ',', '\"', '\n'))
        {
            String[] row = null;
            
            int l=0;
            while((row = reader.readNext()) != null)
            {
                //Get data from instance.
                log.debug("Parsing instance {} ...",++l);
                String encoded_instance_string = row[2];
                Pair<StationPackingInstance,String> SQLinstance = getInstanceFromSQLString(encoded_instance_string, interference_config_foldername, data_manager);
                
                //Write instance to file.
                log.debug("Saving instance {} ...",l);
                StationPackingInstance instance = SQLinstance.getFirst();
                String config_foldername = SQLinstance.getSecond();
                
                List<Integer> sortedStationIDs = new ArrayList<Integer>();
                for(Station station : instance.getStations())
                {
                    sortedStationIDs.add(station.getID());
                }
                Collections.sort(sortedStationIDs);
                List<Integer> sortedAllChannels = new ArrayList<Integer>(instance.getAllChannels());
                Collections.sort(sortedAllChannels);
                
                String aCNFFilename = output_foldername+ File.separator +instance.getHashString()+".cnf";
                
                String[] aComments = new String[]{
                        "FCC Feasibility Checking Instance",
                        "Original Encoded Instance File"+encoded_instance_filename+" line "+l,
                        "Channels: "+StringUtils.join(sortedAllChannels,","),
                        "Stations: "+StringUtils.join(sortedStationIDs,",")};
                
                /*
                 * Create/Get SAT encoder.
                 */
                ManagerBundle data_bundle;
                try {
                    data_bundle = data_manager.getData(config_foldername);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Could not load interference data from \""+config_foldername+"\".");
                }
                
                IConstraintManager constraint_manager = data_bundle.getConstraintManager();
                
                ISATEncoder SATencoder;
                if(SATencoders.containsKey(data_bundle))
                {
                    SATencoder = SATencoders.get(data_bundle);
                }
                else
                {
                    SATencoder = new SATCompressor(constraint_manager);
                    SATencoders.put(data_bundle, SATencoder);
                }
        
                /*
                 * Encode instance into CNF.
                 */
                log.debug("Encoding into SAT...");
                Pair<CNF,ISATDecoder> encoding = SATencoder.encode(instance);
                CNF cnf = encoding.getKey();
                
                log.debug("Saving CNF to {} ...",aCNFFilename);
                
                File cnfFile = new File(aCNFFilename);
                
                if(cnfFile.exists())
                {
                    log.warn("CNF file already exists with name \"{}\".",cnfFile);
                }
                
                try {
                    FileUtils.writeStringToFile(new File(aCNFFilename), cnf.toDIMACS(aComments));
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalStateException("Could not write CNF to file.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Could not read instance from file "+encoded_instance_filename+".",e);
        }
        
        return;
        
    }
    
    private static boolean isInteger(String s) {
        try { 
            Integer.parseInt(s); 
        } catch(NumberFormatException e) { 
            return false; 
        }
        return true;
    }
    
}
