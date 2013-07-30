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
	
	public boolean addLiteral(Integer var, Boolean state){
		boolean aNewVar = !(getVars().contains(var) || getNegatedVars().contains(var));
		if(state) fVars.add(var);
		else fNegatedVars.add(var);
		return aNewVar;
	}
	
	public boolean removeLiteral(Integer var, Boolean state){
		if(state) return fVars.remove(var);
		else return fNegatedVars.remove(var);
	}
	
	public int size(){
		return fVars.size()+fNegatedVars.size();
	}
	
	@Override
	public String toString(){
		return "<"+getVars()+","+getNegatedVars()+">";
	}
	
	@Override
	public boolean equals(Object o){
		if(! (o instanceof Clause)) return false;
		Clause c = (Clause) o;
		return (getVars().equals(c.getVars())&&getNegatedVars().equals(c.getNegatedVars()));
	}

	@Override
	public int hashCode(){
		return getVars().hashCode()+getNegatedVars().hashCode();
	}
}
