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
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import ca.ubc.cs.beta.stationpacking.utils.StationPackingUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * Converts a question type station packing instance to corresponding CNF.
 * @author afrechet
 */
public class QuestionToCNFConverter {
	
	private static Logger log = LoggerFactory.getLogger(QuestionToCNFConverter.class);
	
	private final static String USAGE = "Usage:\n\n" +
										"java -jar CNFExtractor.jar <output directory> <question filename 1> ... <question filename k>\n\n"+
										"Transforms a FCC Incentive Auctions Reverse Auction Simulator feasibility checking question file\n"+
										"into the related DIMACS CNF SAT formula according to SATFC's feasibility checking SAT encoding.\n\n"+
										"<output directory> -- directory where output CNFs will be written.\n"+
										"<question filename i> -- the filename of a feasibility checking question to convert to CNF.";
	/**
	 * See static <code>USAGE</code> string.
	 * @param args
	 */
	public static void main(String[] args) {
		
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
		if(needHelp || args.length<=1)
		{
			System.out.println(USAGE);
			return;
		}
		
		
		DataManager aDataManager = new DataManager();
		String aCNFdir = args[0];
		if(aCNFdir.charAt(aCNFdir.length()-1) != File.separatorChar)
		{
			aCNFdir += File.separator;
		}
		log.info("CNF will be put in {}.",aCNFdir);
		
		//Each entry of args represent a question filename that must be parsed into a problem instance and then encoded to CNF.
		for(int i=1;i<args.length;i++)
		{
			//Question filename.
			String aQuestionFilename = args[i];
			log.info("{}/{}",i,args.length-1);
			log.info("Processing {}...", aQuestionFilename);
			
			String aStationConfig = null;
			String aBand = null;
			Integer aHighest = null;
			Set<Integer> aPackingStations = null;
			
			
			//Parse the question file.
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(aQuestionFilename));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Could not read from question file "+aQuestionFilename+".");
			}
			String line;
			try {
				while ((line = br.readLine()) != null) {
				  
					line = line.trim();
					String key = line.split(",")[0];
					
					if(key.equals("STATION_CONFIG"))
					{
						aStationConfig = line.split(",")[1].trim();
					}
					else if (key.equals("BAND"))
					{
						aBand = line.split(",")[1].trim();
					}
					else if (key.equals("HIGHEST"))
					{
						aHighest = Integer.parseInt(line.split(",")[1]);
					}
					else if (isInteger(key))
					{
						if(aPackingStations==null)
						{
							aPackingStations = new HashSet<Integer>();
						}
						aPackingStations.add(Integer.parseInt(key));
					}
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException("Encountered an error while reading question file.");
			}
			
			ManagerBundle aManagerBundle = null;
			//Build problem instance
			if(aStationConfig!=null)
			{
				try {
					aManagerBundle = aDataManager.getData(aStationConfig);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					throw new IllegalArgumentException("Provided STATION_CONFIG in question file "+aStationConfig+" cannot be found.");
				}
			}
			else
			{
				throw new IllegalArgumentException("Did not find STATION_CONFIG in question file "+aQuestionFilename);
			}
			log.info("Data from: {}.",aStationConfig);
			
			Set<Integer> aPackingChannels = null;
			if(aBand != null)
			{
				if(aBand.equals("UHF") || aBand.equals("3"))
				{
					if(aHighest != null)
					{
						final Integer aFilterHighest = aHighest;
						aPackingChannels = new HashSet<Integer>(
									Collections2.filter(
											StationPackingUtils.UHF_CHANNELS, 
										new Predicate<Integer>()
										{
											@Override
											public boolean apply(Integer c)
											{
												return c<=aFilterHighest;
											}
										}
									)
								);
					}
					else
					{
						throw new IllegalArgumentException("Packing in UHF, but no highest channel specified. Must be problematic.");
					}
					
				}
				else if(aBand.equals("LVHF") || aBand.equals("1"))
				{
					aPackingChannels = StationPackingUtils.LVHF_CHANNELS;
				}
				else if(aBand.equals("HVHF") || aBand.equals("UVHF") || aBand.equals("2"))
				{
					aPackingChannels = StationPackingUtils.HVHF_CHANNELS;
				}
				else
				{
					throw new IllegalArgumentException("Unrecognized value "+aBand+" for BAND entry in question file "+aQuestionFilename+".");
				}
			}
			else
			{
				throw new IllegalArgumentException("Did not find BAND in question file "+aQuestionFilename+".");
			}
			
			List<Integer> aSortedStations = new ArrayList<Integer>(aPackingStations);
			Collections.sort(aSortedStations);
			List<Integer> aSortedChannels = new ArrayList<Integer>(aPackingChannels);
 			Collections.sort(aSortedChannels);
			
			log.info("Packing channels: {}.",StringUtils.join(aSortedChannels,","));
			log.info("Stations : {}.", StringUtils.join(aSortedStations,","));
			
			log.info("Creating station packing instance ...");
			
			IStationManager aStationManager = aManagerBundle.getStationManager();
			IConstraintManager aConstraintManager = aManagerBundle.getConstraintManager();
			
			Set<Station> aStations = aStationManager.getStationsfromID(aPackingStations);
			StationPackingInstance aInstance = StationPackingInstance.constructUniformDomainInstance(aStations, aPackingChannels, new HashMap<Station,Integer>());
			
			
			ISATEncoder aSATEncoder = new SATCompressor(aConstraintManager);

			log.info("Encoding into SAT...");
			Pair<CNF,ISATDecoder> aEncoding = aSATEncoder.encode(aInstance);
			CNF aCNF = aEncoding.getKey();
			
			
			String aCNFFilename = aCNFdir+aInstance.getHashString()+".cnf";
			log.info("Saving CNF to {}...",aCNFFilename);
			
			try {
				FileUtils.writeStringToFile(new File(aCNFFilename), aCNF.toDIMACS(new String[]{"FCC Feasibility Checking Instance","Original Question File: "+aQuestionFilename,"Channels: "+StringUtils.join(aSortedChannels,","),"Stations: "+StringUtils.join(aSortedStations,",")}));
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException("Could not write CNF to file.");
			}
		}
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
