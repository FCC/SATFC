package ca.ubc.cs.beta.stationpacking.solvers.sat.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * A SAT formula in Conjunctive Normal Form (a conjunction of clauses - AND's of OR's of litterals). Implementation wise just a clause set wrapper. 
 * @author afrechet
 */
public class CNF implements Set<Clause>{

	private final HashSet<Clause> fClauses;

	public CNF()
	{
		fClauses = new HashSet<Clause>();
	}
	
	/**
	 * Builds and returns the <a href="http://fairmut3x.wordpress.com/2011/07/29/cnf-conjunctive-normal-form-dimacs-format-explained/">DIMACS</a> string representation of the CNF.
	 * @param aComments - the comments to add at the beginning of the CNF, if any.
	 * @return the DIMACS string representation of the CNF.
	 */
	public String toDIMACS(String[] aComments)
	{
		StringBuilder aStringBuilder = new StringBuilder();
		
		int aNumClauses = fClauses.size();
		long aMaxVariable = 0;
		
		for(Clause aClause : fClauses)
		{
			ArrayList<String> aLitteralStrings = new ArrayList<String>();
			
			for(Litteral aLitteral : aClause)
			{
				if(aLitteral.getVariable()<=0)
				{
					throw new IllegalArgumentException("Cannot transform to DIMACS a CNF that has a litteral with variable value <= 0 (clause: "+aClause.toString()+").");
				}
				else if(aLitteral.getVariable()>aMaxVariable)
				{
					aMaxVariable = aLitteral.getVariable();
				}
				aLitteralStrings.add((aLitteral.getSign() ? "" : "-") + Long.toString(aLitteral.getVariable()));
			}
			
			aStringBuilder.append(StringUtils.join(aLitteralStrings," ")+" 0\n");
		}
		
		aStringBuilder.insert(0, "p cnf "+aMaxVariable+" "+aNumClauses+"\n");
		
		for(int i=aComments.length-1;i>=0;i--)
		{
			aStringBuilder.insert(0, "c "+aComments[i].trim()+"\n");
		}
		
		return aStringBuilder.toString();
	}
	
	/**
	 * @return all the variables present in the CNF.
	 */
	public HashSet<Long> getVariables()
	{
		HashSet<Long> aVariables = new HashSet<Long>();
		
		for(Clause aClause : fClauses)
		{
			for(Litteral aLitteral : aClause)
			{
				aVariables.add(aLitteral.getVariable());
			}
		}
		
		return aVariables;
	}
	
	@Override
	public String toString()
	{
		HashSet<String> aClauseStrings = new HashSet<String>();
		for(Clause aClause : fClauses)
		{
			aClauseStrings.add("("+aClause.toString()+")");
		}
		return StringUtils.join(aClauseStrings," ^ ");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fClauses == null) ? 0 : fClauses.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CNF other = (CNF) obj;
		if (fClauses == null) {
			if (other.fClauses != null)
				return false;
		} else if (!fClauses.equals(other.fClauses))
			return false;
		return true;
	}

	@Override
	public int size() {
		return fClauses.size();
	}

	@Override
	public boolean isEmpty() {
		return fClauses.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return fClauses.contains(o);
	}

	@Override
	public Iterator<Clause> iterator() {
		return fClauses.iterator();
	}

	@Override
	public Object[] toArray() {
		return fClauses.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return fClauses.toArray(a);
	}

	@Override
	public boolean add(Clause e) {
		return fClauses.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return fClauses.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return fClauses.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Clause> c) {
		return fClauses.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return fClauses.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return fClauses.remove(c);
	}

	@Override
	public void clear() {
		fClauses.clear();
	}

}
