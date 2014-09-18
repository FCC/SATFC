package ca.ubc.cs.beta.stationpacking.solvers.sat.base;

import java.io.Serializable;

/**
 * The construction blocks of SAT clauses, consists of an integral variable (long) and its sign/negation/polarity (true=not negated, false=negated). 
 * @author afrechet
 */
public class Literal implements Serializable{

	private final long fVariable;
	private final boolean fSign;
	
	/**
	 * @param aVariable - the litteral's (positive) variable.
	 * @param aSign - the litteral's sign/negation (true=not negated, false=negated).
	 */
	public Literal(long aVariable, boolean aSign)
	{
		/*
		 * TODO Litterals should not be allowed to be < 0 , but to support the current incremental code, we let it be.
		 */
		fVariable = aVariable;
		fSign = aSign;
	}
	
	/**
	 * @return the literal variable.
	 */
	public long getVariable() {
		return fVariable;
	}

	/**
	 * @return the literal sign.
	 */
	public boolean getSign() {
		return fSign;
	}
	
	@Override
	public String toString()
	{
		return (fSign ? "" : "-") + (fVariable<0 ? "("+Long.toString(fVariable)+")" : Long.toString(fVariable) );
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
		Literal other = (Literal) obj;
		if (fSign != other.fSign)
			return false;
		if (fVariable != other.fVariable)
			return false;
		return true;
	}



}
