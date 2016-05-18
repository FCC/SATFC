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
package ca.ubc.cs.beta.stationpacking.datamanagers.constraints;

/**
 * Type of possible constraints.
 *
 * @author afrechet
 */
public enum ConstraintKey {
    CO,
    ADJp1,
    ADJm1,
    ADJp2,
    ADJm2;

    public static ConstraintKey fromString(String string) {
        switch (string) {
            case "CO":
                return CO;
            case "ADJ+1":
                return ADJp1;
            case "ADJ-1":
                return ADJm1;
            case "ADJ+2":
                return ADJp2;
            case "ADJ-2":
                return ADJm2;
            default:
                throw new IllegalArgumentException("Unrecognized constraint key " + string);
        }
    }
}