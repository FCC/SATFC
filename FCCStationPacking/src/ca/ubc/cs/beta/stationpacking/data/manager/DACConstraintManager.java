package ca.ubc.cs.beta.stationpacking.data.manager;

import java.io.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//import org.apache.commons.math.util.MultidimensionalCounter.Iterator;
//import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.data.StationChannelPair;


import au.com.bytecode.opencsv.CSVReader;

/* NA - Code to read the new DAC file format
 * 
 */
public class DACConstraintManager implements IConstraintManager{
	
	final int fminChannel = 14, fmaxChannel=30;
	Map<Integer,Station> fStations = new HashMap<Integer,Station>();;
	Map<Station,Set<Integer>> fStationDomains = new HashMap<Station,Set<Integer>>();
	Map<Station,Set<Station>> fCOConstraints = new HashMap<Station,Set<Station>>();
	Map<Station,Set<Station>> fADJplusConstraints = new HashMap<Station,Set<Station>>();
	Map<Station,Set<Station>> fADJminusConstraints = new HashMap<Station,Set<Station>>();
	
	public DACConstraintManager(Map<Station,Set<Integer>> aStationDomains,Set<Constraint> aPairwiseConstraints){
		fStationDomains = aStationDomains;
		for(Station aStation : fStationDomains.keySet()){
			fStations.put(aStation.getID(), aStation);
			fCOConstraints.put(aStation,new HashSet<Station>());
			fADJplusConstraints.put(aStation,new HashSet<Station>());
			fADJminusConstraints.put(aStation,new HashSet<Station>());
		}		
		
		/*NA - If two stations have ANY co-channel constraint, add them to fCOConstraints
		 *If two stations have ANY adjacent-channel constraints, add them to corresponding fADJ constraint
		 *Could insert code to check whether the constraint set actually has one constraint for EACH channel
		 */
		for(Constraint aConstraint : aPairwiseConstraints){
			StationChannelPair aPair1 = aConstraint.getProtectedPair();
			Station aStation1 = aPair1.getStation();
			StationChannelPair aPair2 = aConstraint.getInterferingPair();
			Station aStation2 = aPair2.getStation();
			if(fStations.get(aStation1.getID())!=null && fStations.get(aStation2.getID())!=null){
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
				} else System.out.println("Constraint involves a channel difference of "+ aDifference);

			} else System.out.println("One of stations "+aStation1.getID()+" and "+aStation2.getID()+" are not in fStations.");
		}
	}
	
	public DACConstraintManager(String aAllowedChannelsFilename, String aPairwiseConstraintsFilename)
	{

		CSVReader aReader;

		try{
			//NA - reading channel domains
			aReader = new CSVReader(new FileReader(aAllowedChannelsFilename),',');
			String[] aLine;
			Station aStation;
			Integer aID,aChannel;
			String aString;
			HashSet<Integer> aChannelDomain;
			while((aLine = aReader.readNext())!=null)
			{
				aID = Integer.valueOf(removeSpaces(aLine[1]));
				aStation = new Station(aID);
				aChannelDomain = new HashSet<Integer>();
				for(int i=2;i<aLine.length;i++)
				{ 	//NA - Ideally would have a more robust check here
					aString = removeSpaces(aLine[i]);
					if(aString.length()>0){
						aChannel = Integer.valueOf(aString); 
						aChannelDomain.add(aChannel);
					}
				}
				fStationDomains.put(aStation, aChannelDomain);
				fStations.put(aID, aStation);
				fCOConstraints.put(aStation,new HashSet<Station>());
				fADJplusConstraints.put(aStation,new HashSet<Station>());
				fADJminusConstraints.put(aStation,new HashSet<Station>());
			}
			aReader.close();
			
			//NA - reading interference constraints
			aReader = new CSVReader(new FileReader(aPairwiseConstraintsFilename),',');
			while((aLine = aReader.readNext())!=null)
			{	//NA - perform some sanity checks
				aID = Integer.valueOf(removeSpaces(aLine[3]));
				aStation = fStations.get(aID);
				if(aStation != null){
					Integer aChannelLower = Integer.valueOf(removeSpaces(aLine[1]));
					Integer aChannelUpper = Integer.valueOf(removeSpaces(aLine[2]));
					if(aChannelLower!=fminChannel || aChannelUpper!=fmaxChannel) 
						System.out.println("Constraint other than "+fminChannel+"-"+fmaxChannel+" found for Station "+aID+".");
					if(removeSpaces(aLine[0]).equals("CO")){ //NA - handle CO constraints
						for(int i = 4; i < aLine.length; i++){
							aString = removeSpaces(aLine[i]);
							if(aString.length()>0){
								Integer aID2 = Integer.valueOf(aString);
								if(fStations.containsKey(aID2)){ //NA - CO constraints are symmetric
									fCOConstraints.get(aStation).add(fStations.get(aID2)); 
									fCOConstraints.get(fStations.get(aID2)).add(aStation);
								} else System.out.println("Station "+aID2+" not fount in fStations.");
							}
						}
					} else if(removeSpaces(aLine[0]).equals("ADJ+1")){ //NA - handle ADJ constraints
						for(int i = 4; i < aLine.length; i++){
							aString = removeSpaces(aLine[i]);
							if(aString.length()>0){
								Integer aID2 = Integer.valueOf(aString);
								if(fStations.containsKey(aID2)){ //NA - ADJ+1 constraints are asymmetric
									//fADJplusConstraints.get(aStation).add(fStations.get(aID2)); 
									//fADJminusConstraints.get(fStations.get(aID2)).add(aStation);
									fADJminusConstraints.get(aStation).add(fStations.get(aID2)); 
									fADJplusConstraints.get(fStations.get(aID2)).add(aStation);
								} else System.out.println("Station "+aID2+" not fount in fStations.");
							}
						}
					} else System.out.println("ERROR reading constraint file: constraint type could not be determined.");	
				} else System.out.println("Station "+aID+" not found in fStations.");
			}
			aReader.close();
		} catch(IOException e){
			System.out.println("IOException in DACConstraintManager constructor.");
			e.printStackTrace();
		}

	}


	public Set<Station> getCOInterferingStations(Station aStation) {
		return fCOConstraints.get(aStation);
	}
	
	public Set<Station> getADJplusInterferingStations(Station aStation) {
		return fADJplusConstraints.get(aStation);
	}
	
	public Set<Station> getADJminusInterferingStations(Station aStation) {
		return fADJminusConstraints.get(aStation);
	}
	
	public Map<Station,Set<Integer>> getStationDomains(){
		return fStationDomains;
	}
	
	public boolean writeConstraints(String fileName){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			Station aStation;
			for(Integer aID : fStations.keySet()){
				writer.write("CO,"+fminChannel+","+fmaxChannel+","+aID+",");
				aStation = fStations.get(aID);
				for(Station aStation2 : fCOConstraints.get(aStation)){
					Integer aID2 = aStation2.getID();
					if(aID<aID2) writer.write(aID2+",");
				}
				writer.write("\n");
				writer.write("ADJ+1,"+fminChannel+","+fmaxChannel+","+aID+",");
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
	
	public boolean writeDomains(String fileName){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			Station aStation;
			for( Integer aID : fStations.keySet()){
				Set<Integer> aChannels = fStationDomains.get(fStations.get(aID));
				if(aChannels != null){
					String aChannelList = aChannels.toString();
					aChannelList = aChannelList.substring(aChannelList.indexOf('[')+1,aChannelList.indexOf(']'));
					writer.write("DOMAIN,"+aID+","+aChannelList+",\n");
				} else System.out.println("fStationDomains does not have entry for "+aID);
			}
			writer.close();
			return true;
		} catch(IOException e) {
			e.printStackTrace();
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

	public Set<Constraint> getPairwiseConstraints() {
		Set<Constraint> pairwiseConstraints = new HashSet<Constraint>();
		Station aStation;
		for(Integer aID : fStations.keySet()){
			aStation = fStations.get(aID);
			for(Station aStation2 : fCOConstraints.get(aStation)){
				if(aID<aStation2.getID()) 
					for(int channel = fminChannel; channel<=fmaxChannel; channel++){
						pairwiseConstraints.add(new Constraint(	new StationChannelPair(aStation,channel),
																new StationChannelPair(aStation2,channel)));
					}
			}
			for(Station aStation2 : fADJplusConstraints.get(aStation)){
				for(int channel = fminChannel; channel<fmaxChannel; channel++){
					pairwiseConstraints.add(new Constraint(	new StationChannelPair(aStation,channel),
															new StationChannelPair(aStation2,channel+1)));
				}	
			}
			
		}
		return(pairwiseConstraints);
	}
	
	//Determines whether the pairwise constraints of two ConstraintManagers are identical
	public boolean matchesConstraints(IConstraintManager aOtherManager){
		//return this.getPairwiseConstraints().equals(aOtherManager.getPairwiseConstraints());
		Set<Constraint> myConstraints = this.getPairwiseConstraints();
		Set<Constraint> theirConstraints = aOtherManager.getPairwiseConstraints();
		if( myConstraints.equals(theirConstraints) ){
			return true;
		} else {
			int aOrigSize = myConstraints.size();
			myConstraints.retainAll(theirConstraints);
			System.out.println("I had "+aOrigSize+" constraints, they had "+theirConstraints.size()+" constraints, there are "+myConstraints.size()+" in the intersection.");
			return false;
		}
	}
	
	public boolean matchesDomains(IConstraintManager aOtherManager){
		//return fStationDomains.equals(aOtherManager.getStationDomains());
		Set<Map.Entry<Station,Set<Integer>>> theirDomains = aOtherManager.getStationDomains().entrySet();
		Set<Map.Entry<Station,Set<Integer>>> myDomains = fStationDomains.entrySet();
		int theirNumStations = theirDomains.size();
		if(theirDomains.equals(myDomains)){
			return true;
		} else {
			theirDomains.retainAll(fStationDomains.entrySet());
			System.out.println("I had "+myDomains.size()+" stations, they had "+theirNumStations+" stations, there are "+theirDomains.size()+" stations with common domain.");
			return false;
		}
	}
	

	
	private String removeSpaces(String s){
		int ind;
		while((ind = s.indexOf(" "))!=-1) s = s.substring(0,ind)+s.substring(ind+1);
		return s;
	}

}
