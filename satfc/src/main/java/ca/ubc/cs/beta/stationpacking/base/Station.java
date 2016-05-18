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
package ca.ubc.cs.beta.stationpacking.base;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Immutable container class for the station object.
 * Uniquely identified by its integer ID.
 * @author afrechet
 */
@JsonSerialize(using = ToStringSerializer.class, as=String.class)
@JsonDeserialize(using = StationDeserializer.class)
public class Station implements Comparable<Station>, Serializable{
	
	private final int fID;
	
	/**
	 * Construct a station.
	 * @param aID - the station ID.
	 */
	public Station(Integer aID){
		fID = aID;
	}

	/**
	 * @return - the station ID.
	 */
	public int getID(){
		return fID;
	}
	
	
	@Override
	public String toString(){
		return Integer.toString(fID);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fID;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Station other = (Station) obj;
		if (fID != other.fID)
			return false;
		return true;
	}

    @Override
	public int compareTo(Station o) {
		return Integer.compare(fID,o.fID);
	}

}
