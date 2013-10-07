package ca.ubc.cs.beta.stationpacking.solvers.sat.base;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

/**
 * A disjunctive clause (OR's of litterals).
 * @author afrechet
 *
 */
public class OldClause implements Collection<Litteral>{

	private final Collection<Litteral> fLitterals;
	
	public OldClause()
	{
		fLitterals = new HashSet<Litteral>();
	}
	
	public Collection<Litteral> getLitterals()
	{
		return fLitterals;
	}
	
	@Override
	public String toString()
	{
		TreeSet<String> aLitteralStrings = new TreeSet<String>();
		for(Litteral aLitteral : fLitterals)
		{
			aLitteralStrings.add(aLitteral.toString());
		}
		return StringUtils.join(aLitteralStrings," v ");
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fLitterals == null) ? 0 : fLitterals.hashCode());
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
		OldClause other = (OldClause) obj;
		if (fLitterals == null) {
			if (other.fLitterals != null)
				return false;
		} else if (!fLitterals.equals(other.fLitterals))
			return false;
		return true;
	}

	@Override
	public int size() {
		return fLitterals.size();
	}

	@Override
	public boolean isEmpty() {
		return fLitterals.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return fLitterals.contains(o);
	}

	@Override
	public Iterator<Litteral> iterator() {
		return fLitterals.iterator();
	}

	@Override
	public Object[] toArray() {
		return fLitterals.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return fLitterals.toArray(a);
	}

	@Override
	public boolean add(Litteral e) {
		return fLitterals.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return fLitterals.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return fLitterals.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Litteral> c) {
		return fLitterals.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return fLitterals.removeAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return fLitterals.removeAll(c);
	}

	@Override
	public void clear() {
		fLitterals.clear();
	}
	
}
