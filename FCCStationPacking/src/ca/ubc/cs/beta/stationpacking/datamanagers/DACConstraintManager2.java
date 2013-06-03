package ca.ubc.cs.beta.stationpacking.datamanagers;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datastructures.Station;


import au.com.bytecode.opencsv.CSVReader;

/* NA - Code to read the new DAC file format
 * 
 */
public class DACConstraintManager2 implements IConstraintManager{
	
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

	/*
	public DACConstraintManager2(Set<Station> aStations,Set<Constraint> aPairwiseConstraints){
		for(Station aStation : aStations){
			fStations.put(aStation.getID(), aStation);
			fCOConstraints.put(aStation,new HashSet<Station>());
			fADJplusConstraints.put(aStation,new HashSet<Station>());
			fADJminusConstraints.put(aStation,new HashSet<Station>());
		}		
	*/
		/*NA - If two stations have ANY co-channel constraint, add them to fCOConstraints
		 *If two stations have ANY adjacent-channel constraints, add them to corresponding fADJ constraint
		 *Could insert code to check whether the constraint set actually has one constraint for EACH channel
		 */
	/*
		try{
			for(Constraint aConstraint : aPairwiseConstraints){
				StationChannelPair aPair1 = aConstraint.getProtectedPair();
				Station aStation1 = aPair1.getStation();
				StationChannelPair aPair2 = aConstraint.getInterferingPair();
				Station aStation2 = aPair2.getStation();
				if(aStations.contains(aStation1) && aStations.contains(aStation2)){
					Integer aDifference = aPair1.getChannel()-aPair2.getChannel();
					if(aDifference == 0){
						fCOConstraints.get(aStation1).add(aStation2);
						fCOConstraints.get(aStation2).add(aStation1);
					} else if(aDifference == -1){
						fADJplusConstraints.get(aStation1).add(aStation2);
						fADJminusConstraints.get(aStation2).add(aStation1);
					} else if(aDifference == 1){
						fADJplusConstraints.get(aStation2).add(aStation1);
						fADJminusConstraints.get(aStation1).add(aStation2);
					} else throw new Exception("Constraint involves a channel difference of "+ aDifference);

				} else throw new Exception("One of stations "+aStation1.getID()+" and "+aStation2.getID()+" are not in fStations.");
			}
		}	catch(Exception e) {
			System.out.println("Exception in DAC Constructor: "+e.getMessage());
			e.printStackTrace();
		}
	}
	*/
	
	
	public DACConstraintManager2(Set<Station> aStations, String aPairwiseConstraintsFilename){	
		ArrayList<HashMap<Station,Set<Station>>> aCOConstraints = new ArrayList<HashMap<Station,Set<Station>>>(3);
		ArrayList<HashMap<Station,Set<Station>>> aADJplusConstraints = new ArrayList<HashMap<Station,Set<Station>>>(3);
		ArrayList<HashMap<Station,Set<Station>>> aADJminusConstraints = new ArrayList<HashMap<Station,Set<Station>>>(3);
		for(int j = 0; j < 3; j++){
			aCOConstraints.add(new HashMap<Station,Set<Station>>());
			aADJplusConstraints.add(new HashMap<Station,Set<Station>>());
			aADJminusConstraints.add(new HashMap<Station,Set<Station>>());	
		}
		for(Station aStation : aStations){
			for(int j = 0; j < 3; j++){
				fStations.put(aStation.getID(), aStation);
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
					if(aChannelLower==2 && aChannelUpper==6){ 
						aChannelType = 0;
					} else if(aChannelLower==7 && aChannelUpper==13){
						aChannelType = 1;
					} else if(aChannelLower==14 && aChannelUpper==51){
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
			System.out.println("Exception in DACConstraintManager constructor: "+e.getMessage());
			e.printStackTrace();
		}
		
		/*
		int aCount = 0;
		for(int j = 0; j < 3; j++){
			for(Station aStation1 : fCOConstraints.get(j).keySet()){
				for(Station aStation2 : fCOConstraints.get(j).get(aStation1)){
					if(! fCOConstraints.get(j).get(aStation2).contains(aStation1)) aCount++;
				}
			}
		}
		System.out.println("Number of asymmetric CO-channel constraints: "+aCount);	
		*/
		
		
		int aCount = 0;
		for(Station aStation1 : aADJplusConstraints.get(2).keySet()){
			for(Station aStation2 : aADJplusConstraints.get(2).get(aStation1)){
				if(! aCOConstraints.get(2).get(aStation2).contains(aStation1)){
					aCount++;
				}
			}
		}
		System.out.println("Number of ADJ constraints without corresponding CO-channel constraints: "+aCount);	
		
		aCount = 0;
		for(Station aStation1 : aADJplusConstraints.get(2).keySet()){
			for(Station aStation2 : aADJplusConstraints.get(2).get(aStation1)){
				if(! aADJplusConstraints.get(2).get(aStation2).contains(aStation1)){
					aCount++;
				}
			}
		}
		System.out.println("Number of asymmetric ADJ constraints: "+aCount);	
		
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


	public Set<Station> getCOInterferingStations(Station aStation) {
		Set<Station> aInterfering = fUHFCOConstraints.get(aStation);
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return new HashSet<Station>(aInterfering);
	}
	
	public Set<Station> getADJplusInterferingStations(Station aStation) {
		Set<Station> aInterfering = fUHFADJConstraints.get(aStation);
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return new HashSet<Station>(aInterfering);
	}
	
	public Set<Station> getADJminusInterferingStations(Station aStation) {
		Set<Station> aInterfering = fUHFADJminusConstraints.get(aStation);
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return new HashSet<Station>(aInterfering);	
	}
	
	
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
	
	public Boolean isSatisfyingAssignment(Map<Integer,Set<Station>> aAssignment){
		Set<Station> aStations;
		for(Integer aChannel : aAssignment.keySet()){ //For each channel
			aStations = aAssignment.get(aChannel);
			for(Station aStation1 : aStations){ //Look at all stations on that channel
				for(Station aStation2 : aStations){ //Check to see if aStaion violates a co-channel constraint
					if(getCOInterferingStations(aStation1).contains(aStation2)) return false; 
				}
				if(aAssignment.containsKey(aChannel+1)){ //Check to see if aStaion violates an adj-channel constraint
					for(Station aStation2 : aAssignment.get(aChannel+1)){
						if(getADJplusInterferingStations(aStation1).contains(aStation2)) return false;
					}
				}
			}
		}
		return true;
	}

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
