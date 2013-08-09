package ca.ubc.cs.beta.stationpacking.solver.sat.cnfwriter;


import java.util.HashSet;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solver.sat.base.Clause_old;

/**
 * @deprecated
 */
public class CNFStringWriter {

	
	public String clausesToString(StationPackingInstance aInstance,Set<Clause_old> aClauseSet){
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
		for(Clause_old aClause : aClauseSet){
			writeClause(aClause,aBuilder,aVars);
		}
		int maxVar = 0; for(Integer aVar : aVars) if(aVar > maxVar) maxVar = aVar;
		//System.out.println("MaxVar is "+maxVar+", numVars is "+aVars.size());
		aBuilder.insert(0,"p cnf "+aVars.size()+" "+aClauseSet.size()+"\n");
		return aBuilder.toString();
	}
	
	private void writeClause(Clause_old aClause, StringBuilder aBuilder, Set<Integer> aVars){
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
	
	public Clause_old stringToAssignmentClause(StationPackingInstance aInstance, String aCNFAssignment){
		Clause_old aAssignmentClause = new Clause_old();
		for(String aLiteral : aCNFAssignment.split(";"))
		{
			boolean aValue = !aLiteral.contains("-"); 
			Integer aVariable = Integer.valueOf(aLiteral.replace("-", ""));
			if(!aAssignmentClause.addLiteral(aVariable,aValue)){
				throw new IllegalStateException("Variable "+aVariable+" assigned to multiple truth values.");
			}	
		}
		return aAssignmentClause;
	}
	
}
