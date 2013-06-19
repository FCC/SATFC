package ca.ubc.cs.beta.stationpacking.solver.taesolver;


import java.util.HashSet;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;

public class CNFStringWriter {

	
	public String clausesToString(Instance aInstance,Set<Clause> aClauseSet) throws Exception{
		StringBuilder aBuilder = new StringBuilder();
		aBuilder.append("c Stations: ");
		for(Station aStation : aInstance.getStations()){
			aBuilder.append(aStation.getID()+" ");
		}
		aBuilder.append("\nc Channels: ");
		for(Integer aChannel : aInstance.getChannels()){
			aBuilder.append(aChannel+" ");
		}
		aBuilder.append("\n");
		Set<Integer> aVars = new HashSet<Integer>();
		for(Clause aClause : aClauseSet){
			writeClause(aClause,aBuilder,aVars);
		}
		aBuilder.insert(0,"p cnf "+aVars.size()+" "+aClauseSet.size()+"\n");
		return aBuilder.toString();
	}
	
	private void writeClause(Clause aClause, StringBuilder aBuilder, Set<Integer> aVars){
		for(Integer aVar : aClause.getVars()){ 
			aBuilder.append(aVar+" "); 
			aVars.add(aVar);
		}
		for(Integer aVar : aClause.getNegatedVars()){ 
			aBuilder.append("-"+aVar+" "); 
			aVars.add(aVar);
		}
		aBuilder.append("0\n");
	}
	
	public Clause stringToAssignmentClause(Instance aInstance, String aCNFAssignment) throws Exception{
		Clause aAssignmentClause = new Clause();
		for(String aLiteral : aCNFAssignment.split(";"))
		{
			boolean aValue = !aLiteral.contains("-"); 
			Integer aVariable = Integer.valueOf(aLiteral.replace("-", ""));
			if(!aAssignmentClause.addLiteral(aVariable,aValue)){
				throw new Exception("Variable "+aVariable+" assigned to multiple truth values.");
			}	
		}
		return aAssignmentClause;
	}
	
}
