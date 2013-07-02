package ca.ubc.cs.beta.stationpacking.datamanagers;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.datastructures.Station;


import au.com.bytecode.opencsv.CSVReader;

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
	
	static final Integer LVHFmin = 2, LVHFmax = 6, UVHFmin=7, UVHFmax = 13, UHFmin = 14, UHFmax = 51;
	Set<Integer> LVHFChannels = new HashSet<Integer>(), UVHFChannels = new HashSet<Integer>(), UHFChannels = new HashSet<Integer>();
	
	public DACConstraintManager(Set<Station> aStations, String aPairwiseConstraintsFilename){	
		
		LVHFChannels = new HashSet<Integer>();
		for(int i = LVHFmin; i<= LVHFmax; i++) LVHFChannels.add(i);
		for(int i = UVHFmin; i<= UVHFmax; i++) UVHFChannels.add(i);
		for(int i = UHFmin; i<= UHFmax; i++) UHFChannels.add(i);
		UHFChannels.remove(37);

		
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
		

		try{
			int aChannelType;
			String[] aLine;
			String aString;
			Integer aID;
			Station aStation;
			//NA - reading interference constraints
			CSVReader aReader = new CSVReader(new FileReader(aPairwiseConstraintsFilename),',');
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
						throw new Exception("Unexpected channel range "+aChannelLower+"-"+aChannelUpper+" found for Station "+aID+".");
					} 
					if(aLine[0].replaceAll("\\s", "").equals("CO")){ //NA - handle CO constraints
						for(int i = 4; i < aLine.length; i++){
							aString = aLine[i].replaceAll("\\s", "");
							if(aString.length()>0){
								Integer aID2 = Integer.valueOf(aString);
								if(fStations.containsKey(aID2)){ //NA - don't assume that CO constraints are symmetric
									aCOConstraints.get(aChannelType).get(aStation).add(fStations.get(aID2)); 
								} else {
									aReader.close();
									throw new Exception("Station "+aID2+" not fount in fStations.");
								}
							}
						}
					} else if(aLine[0].replaceAll("\\s", "").equals("ADJ+1")){ //NA - handle ADJ constraints
						for(int i = 4; i < aLine.length; i++){
							aString = aLine[i].replaceAll("\\s", "");
							if(aString.length()>0){
								Integer aID2 = Integer.valueOf(aString);
								if(fStations.containsKey(aID2)){ //NA - ADJ+1 constraints are asymmetric
									aADJminusConstraints.get(aChannelType).get(aStation).add(fStations.get(aID2)); 
									aADJplusConstraints.get(aChannelType).get(fStations.get(aID2)).add(aStation);
								} else {
									aReader.close();
									throw new Exception("Station "+aID2+" not fount in fStations.");
								}
							}
						}
					} else {
						aReader.close();
						throw new Exception("ERROR reading constraint file: constraint type could not be determined.");	
					}
				} else {
					aReader.close();
					throw new Exception("Station "+aID+" not found in fStations.");
				}
			}
			aReader.close();
		} catch(Exception e){
			log.error("Exception in DACConstraintManager constructor: "+e.getMessage());
			e.printStackTrace();
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


	public Set<Station> getCOInterferingStations(Station aStation, Set<Integer> aChannelRange) {
		Set<Station> aInterfering;
		if(LVHFChannels.containsAll(aChannelRange)){
			aInterfering = fLowerVHFCOConstraints.get(aStation);
		} else if(UVHFChannels.containsAll(aChannelRange)){
			aInterfering = fUpperVHFCOConstraints.get(aStation);
		} else if(UHFChannels.containsAll(aChannelRange)) {
			aInterfering = fUHFCOConstraints.get(aStation);
		} else {
			throw new IllegalStateException("Specified channel range contains channels from multiple bands.");
		}
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return new HashSet<Station>(aInterfering);
	}
	
	public Set<Station> getADJplusInterferingStations(Station aStation, Set<Integer> aChannelRange){
		Set<Station> aInterfering;
		if(LVHFChannels.containsAll(aChannelRange)){
			aInterfering = fLowerVHFADJConstraints.get(aStation);
		} else if(UVHFChannels.containsAll(aChannelRange)){
			aInterfering = fUpperVHFADJConstraints.get(aStation);
		} else if(UHFChannels.containsAll(aChannelRange)) {
			aInterfering = fUHFADJConstraints.get(aStation);
		} else {
			throw new IllegalStateException("Specified channel range contains channels from multiple bands.");
		}
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return new HashSet<Station>(aInterfering);
	}
	
	private Set<Station> getADJplusInterferingStations(Station aStation, Integer aChannel) throws Exception{
		Set<Integer> aChannelSet = new HashSet<Integer>();
		aChannelSet.add(aChannel);
		return getADJplusInterferingStations(aStation,aChannelSet);
	}
	
	private Set<Station> getCOInterferingStations(Station aStation, Integer aChannel) throws Exception{
		Set<Integer> aChannelSet = new HashSet<Integer>();
		aChannelSet.add(aChannel);
		return getCOInterferingStations(aStation,aChannelSet);
	}
	
	
	public Boolean isSatisfyingAssignment(Map<Integer,Set<Station>> aAssignment){
		try{
			Set<Station> aStations;
			for(Integer aChannel : aAssignment.keySet()){ //For each channel
				aStations = aAssignment.get(aChannel);
				for(Station aStation1 : aStations){ //Look at all stations on that channel
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
			}
		} catch(Exception e){
			log.error("\nStation assigned to channel not in the range 2-51.\n");
			return false; //Station assigned to a channel not in the range 2-51; should we throw an exception?
		}
		return true;
	}
	
	
	/*
	public Set<Station> getADJminusInterferingStations(Station aStation) {
		Set<Station> aInterfering = fUHFADJminusConstraints.get(aStation);
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return new HashSet<Station>(aInterfering);	
	}
	*/

	/*
>>>>>>> branch 'master' of https://afrechet@bitbucket.org/afrechet/fcc-station-packing.git
	public boolean writeConstraints(String fileName, Set<Integer> aChannels){
		try{
			Integer aMinChannel = Integer.MAX_VALUE;
			Integer aMaxChannel = 0;
			for(Integer aChannel : aChannels){
				if(aChannel < aMinChannel) aMinChannel = aChannel;
				if(aChannel > aMaxChannel) aMaxChannel = aChannel;
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			Station aStation;
			for(Integer aID : fStations.keySet()){
				writer.write("CO,"+aMinChannel+","+aMaxChannel+","+aID+",");
				aStation = fStations.get(aID);
				for(Station aStation2 : getCOInterferingStations(aStation)){
					Integer aID2 = aStation2.getID();
					if(aID<aID2) writer.write(aID2+",");
				}
				writer.write("\n");
				writer.write("ADJ+1,"+aMinChannel+","+aMaxChannel+","+aID+",");
				for(Station aStation2 : getADJminusInterferingStations(aStation)){
					writer.write(aStation2.getID()+",");
				}	
				writer.write("\n");
			}
			writer.close();
			return true;
		} catch(IOException e) {
			System.out.println("IOException in writeConstraints "+e.getStackTrace());
			return false;
		}
	}
	*/
	
	/*
	public boolean writePairwiseConstraints(String fileName){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			for(Integer aID : fStations.keySet()){
				for(Station aStation2 : getCOInterferingStations(fStations.get(aID))){
					Integer aID2 = aStation2.getID();
					if(aID<aID2) writer.write("CO,"+aID+","+aID2+",\n");
				}
				for(Station aStation2 : getADJminusInterferingStations(fStations.get(aID))){
					writer.write("ADJ+1,"+aID+","+","+aStation2.getID()+",\n");
				}	
			}
			writer.close();
			return true;
		} catch(IOException e){
			e.printStackTrace();
			return false;
		}
	}
	*/

	/*
	public Set<Constraint> getPairwiseConstraints(Set<Integer> aChannels) {
		Set<Constraint> pairwiseConstraints = new HashSet<Constraint>();
		Station aStation;
		for(Integer aID : fStations.keySet()){
			aStation = fStations.get(aID);
			for(Station aStation2 : getCOInterferingStations(aStation)){
				if(aID<aStation2.getID()) 
					for(Integer aChannel : aChannels){
						pairwiseConstraints.add(new Constraint(	new StationChannelPair(aStation,aChannel),
																new StationChannelPair(aStation2,aChannel)));
					}
			}
			
			for(Station aStation2 : getADJplusInterferingStations(aStation)){
				for(Integer aChannel : aChannels){
					pairwiseConstraints.add(new Constraint(	new StationChannelPair(aStation,aChannel),
															new StationChannelPair(aStation2,aChannel+1)));
				}	
			}
			
			
		}
		return(pairwiseConstraints);
	}
	*/

	/*
	public boolean matchesConstraints(IConstraintManager aOtherManager, Set<Integer> aChannels){
		//return this.getPairwiseConstraints().equals(aOtherManager.getPairwiseConstraints());
		Set<Constraint> myConstraints = this.getPairwiseConstraints(aChannels);
		Set<Constraint> theirConstraints = aOtherManager.getPairwiseConstraints(aChannels);
		if( myConstraints.equals(theirConstraints) ){
			return true;
		} else {
			int aOrigSize = myConstraints.size();
			myConstraints.retainAll(theirConstraints);
			System.out.println("I had "+aOrigSize+" constraints, they had "+theirConstraints.size()+" constraints, there are "+myConstraints.size()+" in the intersection.");
			return false;
		}
	}
	*/
}
