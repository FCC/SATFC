package ca.ubc.cs.beta.stationpacking.solvers.sat.base;

import java.util.Comparator;

public class LitteralComparator implements Comparator<Litteral>
{
	@Override
	public int compare(Litteral o1, Litteral o2) {
		return Long.compare(o1.getVariable(),o2.getVariable());
	}
};