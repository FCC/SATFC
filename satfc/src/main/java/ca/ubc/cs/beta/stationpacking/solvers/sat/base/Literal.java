/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
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
		 //TODO Litterals should not be allowed to be < 0 , but to support the current incremental code, we let it be.
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
