package ca.ubc.cs.beta.stationpacking.solvers.sat.base;

import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Iterator;

public class Clause extends AbstractSet<Litteral>{
	
	ArrayDeque<Litteral> fLitterals;
	
	@Override
	public boolean add(Litteral l)
	{
		return fLitterals.add(l);
	}
	
	@Override
	public Iterator<Litteral> iterator() {
		return fLitterals.iterator();
	}

	@Override
	public int size() {
		return fLitterals.size();
	}

}
