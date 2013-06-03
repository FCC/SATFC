package ca.ubc.cs.beta.stationpacking.datastructures;

import java.util.HashSet;
import java.util.Set;

public class Clause {
	
	private Set<Integer> fVars, fNegatedVars;
	
	public Clause(Set<Integer> aVars, Set<Integer> aNegatedVars){
		fVars = aVars;
		fNegatedVars = aNegatedVars;
	}
	
	public Clause(){
		fVars = new HashSet<Integer>();
		fNegatedVars = new HashSet<Integer>();
	}
	
	public Set<Integer> getVars(){
		return new HashSet<Integer>(fVars);
	}
	
	public Set<Integer> getNegatedVars(){
		return new HashSet<Integer>(fNegatedVars);
	}
	
	public void addLiteral(Integer var, Boolean state){
		if(state) fVars.add(var);
		else fNegatedVars.add(var);
	}

}
