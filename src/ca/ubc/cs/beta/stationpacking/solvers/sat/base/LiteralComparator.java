package ca.ubc.cs.beta.stationpacking.solvers.sat.base;

import java.util.Comparator;

/**
 * Compares literal based on their variable value.
 * @author afrechet
 */
public class LiteralComparator implements Comparator<Literal>
{
	@Override
	public int compare(Literal o1, Literal o2) {
		return Long.compare(o1.getVariable(),o2.getVariable());
	}
};