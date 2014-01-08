package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/* NA - Code to read the new DAC file format
 * 
 */
public class DACConstraintManager implements IConstraintManager{
	
	private static Logger log = LoggerFactory.getLogger(DACConstraintManager.class);
	
	Map<Integer,Station> fStations = new HashMap<Integer,Station>();
	HashMap<Station,Set<Station>> fLowerVHFCOConstraints;
	HashMap<Station,Set<Station>> fUpperVHFCOConstraints;
	HashMap<Station,Set<Station>> fUHFCOConstraints;
	HashMap<Station,Set<Station>> fLowerVHFADJConstraints;
	HashMap<Station,Set<Station>> fUpperVHFADJConstraints;
	HashMap<Station,Set<Station>> fUHFADJConstraints;
	HashMap<Station,Set<Station>> fLowerVHFADJminusConstraints;
	HashMap<Station,Set<Station>> fUpperVHFADJminusConstraints;
	HashMap<Station,Set<Station>> fUHFADJminusConstraints;
	
	public static final Integer LVHFmin = 2, LVHFmax = 6, UVHFmin=7, UVHFmax = 13, UHFmin = 14, UHFmax = 51;
	public static final HashSet<Integer> LVHF_CHANNELS = new HashSet<Integer>(Arrays.asList(2,3,4,5,6));
	public static final HashSet<Integer> HVHF_CHANNELS = new HashSet<Integer>(Arrays.asList(7,8,9,10,11,12,13));
	public static final HashSet<Integer> UHF_CHANNELS = new HashSet<Integer>(Arrays.asList(14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,38,39,40,41,42,43,44,45,46,47,48,49,50,51));
	
	public DACConstraintManager(IStationManager aStationManager, String aPairwiseConstraintsFilename) throws FileNotFoundException{	
		
		Set<Station> aStations = aStationManager.getStations();
		
		ArrayList<HashMap<Station,Set<Station>>> aCOConstraints = new ArrayList<HashMap<Station,Set<Station>>>(3);
		ArrayList<HashMap<Station,Set<Station>>> aADJplusConstraints = new ArrayList<HashMap<Station,Set<Station>>>(3);
		ArrayList<HashMap<Station,Set<Station>>> aADJminusConstraints = new ArrayList<HashMap<Station,Set<Station>>>(3);
		
		//Each ArrayList has three hashmaps: one each for Lower VHF, Upper VHF, and UHF
		for(int j = 0; j < 3; j++){
			aCOConstraints.add(new HashMap<Station,Set<Station>>());
			aADJplusConstraints.add(new HashMap<Station,Set<Station>>());
			aADJminusConstraints.add(new HashMap<Station,Set<Station>>());	
		}
		for(Station aStation : aStations){
			fStations.put(aStation.getID(), aStation);
			for(int j = 0; j < 3; j++){
				aCOConstraints.get(j).put(aStation,new HashSet<Station>());
				aADJplusConstraints.get(j).put(aStation,new HashSet<Station>());
				aADJminusConstraints.get(j).put(aStation,new HashSet<Station>());
			}
		}

		CSVReader aReader = new CSVReader(new FileReader(aPairwiseConstraintsFilename),',');
		
		try{
			int aChannelType;
			String[] aLine;
			String aString;
			Integer aID;
			Station aStation;
			while((aLine = aReader.readNext())!=null){	//NA - perform some sanity checks
				aID = Integer.valueOf(aLine[3].replaceAll("\\s", ""));
				if((aStation=fStations.get(aID)) != null){
					Integer aChannelLower = Integer.valueOf(aLine[1].replaceAll("\\s", ""));
					Integer aChannelUpper = Integer.valueOf(aLine[2].replaceAll("\\s", ""));
					//NA - check to see if CO constraints are symmetric, if channel range matters
					if(aChannelLower==LVHFmin && aChannelUpper==LVHFmax){ 
						aChannelType = 0;
					} else if(aChannelLower==UVHFmin && aChannelUpper==UVHFmax){
						aChannelType = 1;
					} else if(aChannelLower==UHFmin && aChannelUpper==UHFmax){
						aChannelType = 2;
					} else {
						aReader.close();
						throw new IllegalArgumentException("Unexpected channel range "+aChannelLower+"-"+aChannelUpper+" found for Station "+aID+".");
					} 
					if(aLine[0].replaceAll("\\s", "").equals("CO")){ //NA - handle CO constraints
						for(int i = 4; i < aLine.length; i++){
							aString = aLine[i].replaceAll("\\s", "");
							if(aString.length()>0){
								Integer aID2 = Integer.valueOf(aString);
								if(fStations.containsKey(aID2)){ //NA - don't assume that CO constraints are symmetric
									aCOConstraints.get(aChannelType).get(aStation).add(fStations.get(aID2));
									
									/*
									 * CO constraints should be symmetric though.
									 * 
									 * @author wtaysom
									 */
									aCOConstraints.get(aChannelType).get(fStations.get(aID2)).add(aStation);
								} else {
									aReader.close();
									throw new IllegalStateException("Station "+aID2+" not fount in fStations.");
								}
							}
						}
					} else if(aLine[0].replaceAll("\\s", "").equals("ADJ+1")){ //NA - handle ADJ constraints
						for(int i = 4; i < aLine.length; i++){
							aString = aLine[i].replaceAll("\\s", "");
							if(aString.length()>0){
								Integer aID2 = Integer.valueOf(aString);
								if(fStations.containsKey(aID2)){ //NA - ADJ+1 constraints are asymmetric
									/*
									 * Switched semantics with ADJ-1.
									 * 
									 * @author wtaysom
									 */
									aADJminusConstraints.get(aChannelType).get(fStations.get(aID2)).add(aStation);
									aADJplusConstraints.get(aChannelType).get(aStation).add(fStations.get(aID2));
									
									/*
									 * Add CO constraints too.
									 * 
									 * @author wtaysom
									 */
									aCOConstraints.get(aChannelType).get(aStation).add(fStations.get(aID2));
									aCOConstraints.get(aChannelType).get(fStations.get(aID2)).add(aStation);
								} else {
									aReader.close();
									throw new IllegalArgumentException("Station "+aID2+" not fount in fStations.");
								}
							}
						}
					}
					else if(aLine[0].replaceAll("\\s", "").equals("ADJ-1")){ //NA - handle ADJ constraints
					for(int i = 4; i < aLine.length; i++){
						aString = aLine[i].replaceAll("\\s", "");
						if(aString.length()>0){
							Integer aID2 = Integer.valueOf(aString);
							if(fStations.containsKey(aID2)){ //NA - ADJ-1 constraints are asymmetric
								/*
								 * Switched semantics with ADJ+1.
								 * 
								 * @author wtaysom
								 */
								aADJminusConstraints.get(aChannelType).get(aStation).add(fStations.get(aID2)); 
								aADJplusConstraints.get(aChannelType).get(fStations.get(aID2)).add(aStation);
								
								/*
								 * Add CO constraints too.
								 * 
								 * @author wtaysom
								 */
								aCOConstraints.get(aChannelType).get(aStation).add(fStations.get(aID2));
								aCOConstraints.get(aChannelType).get(fStations.get(aID2)).add(aStation);
							} else {
								aReader.close();
								throw new IllegalArgumentException("Station "+aID2+" not fount in fStations.");
							}
						}
					}
				}
					else {
						aReader.close();
						throw new IllegalArgumentException("Error reading constraint file: unrecognized constraint type "+aLine[0].replaceAll("\\s", "")+".");	
					}
				} 
				else 
				{
					aReader.close();
					throw new IllegalArgumentException("Station "+aID+" not found in manager's stations.");
				}
			}
			aReader.close();
		} catch(IOException e){
			throw new IllegalStateException("There was an exception while reading the constraint manager file ("+e.getMessage()+").");
		}
		
		fLowerVHFCOConstraints = aCOConstraints.get(0);
		fUpperVHFCOConstraints = aCOConstraints.get(1);
		fUHFCOConstraints = aCOConstraints.get(2);
		fLowerVHFADJConstraints = aADJplusConstraints.get(0);
		fUpperVHFADJConstraints = aADJplusConstraints.get(1);
		fUHFADJConstraints = aADJplusConstraints.get(2);
		fLowerVHFADJminusConstraints = aADJminusConstraints.get(0);
		fUpperVHFADJminusConstraints = aADJminusConstraints.get(1);
		fUHFADJminusConstraints = aADJminusConstraints.get(2);
	}

	@Override
	public Set<Station> getCOInterferingStations(Station aStation, Set<Integer> aChannelRange) {
		Set<Station> aInterfering;
		if(LVHF_CHANNELS.containsAll(aChannelRange)){
			aInterfering = fLowerVHFCOConstraints.get(aStation);
		} else if(HVHF_CHANNELS.containsAll(aChannelRange)){
			aInterfering = fUpperVHFCOConstraints.get(aStation);
		} else if(UHF_CHANNELS.containsAll(aChannelRange)) {
			aInterfering = fUHFCOConstraints.get(aStation);
		} else {
			throw new IllegalStateException("Specified channel range contains channels from multiple bands.");
		}
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return new HashSet<Station>(aInterfering);
	}
	
	@Override
	public Set<Station> getADJplusInterferingStations(Station aStation, Set<Integer> aChannelRange){
		Set<Station> aInterfering;
		if(LVHF_CHANNELS.containsAll(aChannelRange)){
			aInterfering = fLowerVHFADJConstraints.get(aStation);
		} else if(HVHF_CHANNELS.containsAll(aChannelRange)){
			aInterfering = fUpperVHFADJConstraints.get(aStation);
		} else if(UHF_CHANNELS.containsAll(aChannelRange)) {
			aInterfering = fUHFADJConstraints.get(aStation);
		} else {
			throw new IllegalStateException("Specified channel range contains channels from multiple bands.");
		}
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return new HashSet<Station>(aInterfering);
	}
	
	
	private Set<Station> getADJplusInterferingStations(Station aStation, Integer aChannel){
		Set<Integer> aChannelSet = new HashSet<Integer>();
		aChannelSet.add(aChannel);
		return getADJplusInterferingStations(aStation,aChannelSet);
	}
	
	private Set<Station> getCOInterferingStations(Station aStation, Integer aChannel){
		Set<Integer> aChannelSet = new HashSet<Integer>();
		aChannelSet.add(aChannel);
		return getCOInterferingStations(aStation,aChannelSet);
	}
	
	@Override
	public Boolean isSatisfyingAssignment(Map<Integer,Set<Station>> aAssignment){
		
		Set<Station> aAllStations = new HashSet<Station>();
		
		Set<Station> aStations;
		for(Integer aChannel : aAssignment.keySet()){ //For each channel
			aStations = aAssignment.get(aChannel);
			for(Station aStation1 : aStations){ //Look at all stations on that channel
				if(aAllStations.contains(aStation1))
				{
					log.error("Station {} appears twice in the assignment.",aStation1);
					return false;
				}
				
				for(Station aStation2 : aStations){ //Check to see if aStaion violates a co-channel constraint
					if(getCOInterferingStations(aStation1,aChannel).contains(aStation2)){
						log.error("\n"+aStation1 + " and "+aStation2+" share channel "+aChannel+", on which they interfere.\n");
						return false; 
					}
				}
				if(aAssignment.containsKey(aChannel+1)){ //Check to see if aStation violates an adj-channel constraint
					for(Station aStation2 : aAssignment.get(aChannel+1)){
						if(getADJplusInterferingStations(aStation1,aChannel).contains(aStation2)){
							log.error("\n"+aStation1 + " is on channel "+aChannel+", while "+aStation2+" is on channel "+(aChannel+1)+", causing them to interfere.\n");
							return false;
						}
					}
				}
			}
			aAllStations.addAll(aStations);
		}
		
		return true;
	}
	
	
}
