package ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.*;

//import ca.ubc.cs.beta.stationpacking.data.Constraint;
import ca.ubc.cs.beta.stationpacking.data.Station;
import ca.ubc.cs.beta.stationpacking.experiment.instanceencoder.cnfencoder.ICNFEncoder;


public class NickCNFEncoder implements ICNFEncoder {
	
	/*NA - currently all code is in constructor. Should change to put code in encode, write, but I 
	 *haven't dedicated much time to thinking about this.
	 */
	
	private Map<Station,Integer> fInternalID = new HashMap<Station,Integer>();
	private final int fmin_Channel = 14,fmax_Channel =30;
	private final String tempFileName = "/Users/narnosti/Documents/fcc-station-packing/Output/CNF_temp.txt";
	private int fnum_Clauses = 0;

	public NickCNFEncoder(	Map<Station,Set<Integer>> aStationDomains, 
							Map<Station,Set<Station>> aCOConstraints,
							Map<Station,Set<Station>> aADJplusConstraints)
	{

		String writeFileName = "/Users/narnosti/Documents/fcc-station-packing/Output/CNF_file.txt";
		
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFileName));
		
			//NA - could try to run connected components (i.e. use a component grouper)

			//NA - Generate internal ID numbers for variable assignment
			int aID = 1;
			HashSet<Integer> aEmpty = new HashSet<Integer>();
			HashSet<Integer> aNegatedVars = new HashSet<Integer>();
			for(Station aStation : aStationDomains.keySet()){
				fInternalID.put(aStation, aID++);					
				//NA - write BASE clauses (at least one channel, at most one channel per station)
				//NA - could try to be clever and only look at pairs in domain of aStation
				for(int i = fmin_Channel; i < fmax_Channel; i++){
					for(int j = i+1; j <= fmax_Channel; j++){
						aNegatedVars.clear(); 
						aNegatedVars.add(get_variable(aStation,i));
						aNegatedVars.add(get_variable(aStation,j));
						writeClause(aEmpty,aNegatedVars,writer);
					}
				}
				//NA - write DOMAIN constraints to file
				writeClause(aStationDomains.get(aStation),aEmpty,writer);
			}
		
			
			//NA - write CO constraints
			for(Station aStation : aCOConstraints.keySet()){
				Set<Station> aInterferingStations = aCOConstraints.get(aStation);
				for(Station aStation2 : aInterferingStations){
					//NA - avoid writing constraints twice
					if(aStation.getID()<aStation2.getID()){ 
						for(int i = fmin_Channel; i<=fmax_Channel; i++){
							aNegatedVars.clear();
							aNegatedVars.add(get_variable(aStation,i));
							aNegatedVars.add(get_variable(aStation2,i));
							writeClause(aEmpty,aNegatedVars,writer);
						}
					}
				}
			}
		
			//NA - write ADJ constraints
			for(Station aStation : aADJplusConstraints.keySet()){
				Set<Station> aInterferingStations = aADJplusConstraints.get(aStation);
				for(Station aStation2 : aInterferingStations){
					//NA - TODO check ADJ constraint direction
					for(int i = fmin_Channel; i<fmax_Channel; i++){ 
						aNegatedVars.clear();
						aNegatedVars.add(get_variable(aStation,i));
						aNegatedVars.add(get_variable(aStation2,i+1));
						writeClause(aEmpty,aNegatedVars,writer);
					}
				}	
			}
			
			writer.close();
			writer = new BufferedWriter(new FileWriter(writeFileName));
			int numVars = (fmax_Channel-fmin_Channel+1)*fInternalID.keySet().size();
			writer.write("p cnf "+numVars+" "+fnum_Clauses+"\n");
			BufferedReader reader = new BufferedReader(new FileReader(tempFileName));
			while(reader.ready()){
				writer.write(reader.readLine()+"\n");
			}
			writer.close();
			reader.close();
		} catch(IOException e){
			System.out.println("IOException in NickCNFEncoder\n"+e);
		}
	}
	
	//NA - Currently assigns each station a variable for each channel fmax:fmin
	private Integer get_variable(Station aStation, int aChannel){
		return((fInternalID.get(aStation)-1)*(fmax_Channel-fmin_Channel+1)+aChannel-fmin_Channel+1);
	}
	
	//NA - Write a clause using aWriter
	private boolean writeClause(Set<Integer> aVars,Set<Integer> aNegatedVars, BufferedWriter aWriter){
		try{
			for(Integer aVar : aVars){ aWriter.write(aVar+" "); }
			for(Integer aVar : aNegatedVars){ aWriter.write("-"+aVar+" "); }
			aWriter.write("0\n");
			fnum_Clauses++;
			return(true);
		} catch(IOException e) {
			return(false);
		}
		
	}
	
	@Override
	public String encode(Set<Station> aStations) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean write(Set<Station> aStations, String aFileName){
		// TODO Auto-generated method stub
		return true;
	}

}
