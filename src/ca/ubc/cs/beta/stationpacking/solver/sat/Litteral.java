package ca.ubc.cs.beta.stationpacking.solver.sat;

/**
 * The construction blocks of SAT clauses, consists of an integral variable (long) and its sign/negation (true=not negated, false=negated). 
 * @author afrechet
 */
public class Litteral {

	private final long fVariable;
	private final boolean fSign;
	
	/**
	 * @param aVariable - the litteral's (positive) variable.
	 * @param aSign - the litteral's sign/negation (true=not negated, false=negated).
	 */
	public Litteral(long aVariable,boolean aSign)
	{
		if(aVariable < 0)
		{
			throw new IllegalArgumentException("A litteral's variable must be a strictly positive long.");
		}
		fVariable = aVariable;
		fSign = aSign;
	}
	
	public long getVariable() {
		return fVariable;
	}

	public boolean getSign() {
		return fSign;
	}
	
	@Override
	public String toString()
	{
		return (fSign ? "" : "-") + Long.toString(fVariable);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (fSign ? 1231 : 1237);
		result = prime * result + (int) (fVariable ^ (fVariable >>> 32));
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
		Litteral other = (Litteral) obj;
		if (fSign != other.fSign)
			return false;
		if (fVariable != other.fVariable)
			return false;
		return true;
	}


}
