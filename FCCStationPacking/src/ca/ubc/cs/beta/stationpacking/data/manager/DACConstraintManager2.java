package ca.ubc.cs.beta.stationpacking.data.manager;

import java.io.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.StationChannelPair;


import au.com.bytecode.opencsv.CSVReader;

/* NA - Code to read the new DAC file format
 * 
 */
public class DACConstraintManager2 implements IConstraintManager{
	
	final int fminChannel = 14, fmaxChannel=30; //Used only to check input file
	Map<Integer,Station> fStations = new HashMap<Integer,Station>();
	Map<Station,Set<Station>> fCOConstraints = new HashMap<Station,Set<Station>>();
	Map<Station,Set<Station>> fADJplusConstraints = new HashMap<Station,Set<Station>>();
	Map<Station,Set<Station>> fADJminusConstraints = new HashMap<Station,Set<Station>>();
	
	public DACConstraintManager2(Set<Station> aStations,Set<Constraint> aPairwiseConstraints){
		for(Station aStation : aStations){
			fStations.put(aStation.getID(), aStation);
			fCOConstraints.put(aStation,new HashSet<Station>());
			fADJplusConstraints.put(aStation,new HashSet<Station>());
			fADJminusConstraints.put(aStation,new HashSet<Station>());
		}		
		
		/*NA - If two stations have ANY co-channel constraint, add them to fCOConstraints
		 *If two stations have ANY adjacent-channel constraints, add them to corresponding fADJ constraint
		 *Could insert code to check whether the constraint set actually has one constraint for EACH channel
		 */
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
	
	public DACConstraintManager2(Set<Station> aStations, String aPairwiseConstraintsFilename){
		for(Station aStation : aStations){
			fStations.put(aStation.getID(), aStation);
			fCOConstraints.put(aStation,new HashSet<Station>());
			fADJplusConstraints.put(aStation,new HashSet<Station>());
			fADJminusConstraints.put(aStation,new HashSet<Station>());
		}
		

		try{
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
					if(aChannelLower!=fminChannel || aChannelUpper!=fmaxChannel){ 
						aReader.close();
						throw new Exception("Constraint other than "+fminChannel+"-"+fmaxChannel+" found for Station "+aID+".");
					}
					if(aLine[0].replaceAll("\\s", "").equals("CO")){ //NA - handle CO constraints
						for(int i = 4; i < aLine.length; i++){
							aString = aLine[i].replaceAll("\\s", "");
							if(aString.length()>0){
								Integer aID2 = Integer.valueOf(aString);
								if(fStations.containsKey(aID2)){ //NA - CO constraints are symmetric
									fCOConstraints.get(aStation).add(fStations.get(aID2)); 
									fCOConstraints.get(fStations.get(aID2)).add(aStation);
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
									fADJminusConstraints.get(aStation).add(fStations.get(aID2)); 
									fADJplusConstraints.get(fStations.get(aID2)).add(aStation);
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

	}


	public Set<Station> getCOInterferingStations(Station aStation) {
		Set<Station> aInterfering = fCOConstraints.get(aStation);
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return aInterfering;
	}
	
	public Set<Station> getADJplusInterferingStations(Station aStation) {
		Set<Station> aInterfering = fADJplusConstraints.get(aStation);
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return aInterfering;
	}
	
	public Set<Station> getADJminusInterferingStations(Station aStation) {
		Set<Station> aInterfering = fADJminusConstraints.get(aStation);
		if(aInterfering==null) aInterfering = new HashSet<Station>();
		return aInterfering;	
	}
	
	//NA - just assume that at least two feasible channels are adjacent (so that ADJ constraints are relevant).
	public Set<Set<Station>> group(Set<Station> aStations){
		SimpleGraph<Station,DefaultEdge> aConstraintGraph = new SimpleGraph<Station,DefaultEdge>(DefaultEdge.class);
		for(Station aStation : aStations){
			aConstraintGraph.addVertex(aStation);
		}
		for(Station aStation1 : aStations){
			for(Station aStation2 : getCOInterferingStations(aStation1)){
				if(aConstraintGraph.containsVertex(aStation2)){
					aConstraintGraph.addEdge(aStation1, aStation2);
				}
			}
			for(Station aStation2 : getADJplusInterferingStations(aStation1)){
				if(aConstraintGraph.containsVertex(aStation2)){
					aConstraintGraph.addEdge(aStation1, aStation2);
				}
			}
		}
		ConnectivityInspector<Station, DefaultEdge> aConnectivityInspector = new ConnectivityInspector<Station,DefaultEdge>(aConstraintGraph);
		return(new HashSet<Set<Station>>(aConnectivityInspector.connectedSets()));
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
				for(Station aStation2 : fCOConstraints.get(aStation)){
					Integer aID2 = aStation2.getID();
					if(aID<aID2) writer.write(aID2+",");
				}
				writer.write("\n");
				writer.write("ADJ+1,"+aMinChannel+","+aMaxChannel+","+aID+",");
				for(Station aStation2 : fADJminusConstraints.get(aStation)){
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
				for(Station aStation2 : fCOConstraints.get(fStations.get(aID))){
					Integer aID2 = aStation2.getID();
					if(aID<aID2) writer.write("CO,"+aID+","+aID2+",\n");
				}
				for(Station aStation2 : fADJminusConstraints.get(fStations.get(aID))){
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
}
