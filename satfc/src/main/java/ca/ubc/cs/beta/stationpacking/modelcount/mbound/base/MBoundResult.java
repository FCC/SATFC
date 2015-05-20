/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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
package ca.ubc.cs.beta.stationpacking.modelcount.mbound.base;

/**
 * #SAT approximate counting result from algorithm MBound..
 * @author tqichen
 */
public class MBoundResult {
    
    /**
     * Type of result obtained.
     * @author tqichen
     */
    public enum MBoundResultType {
        /**
         * Count is an upperbound.
         */
        UPPERBOUND,
        /**
         * Count is a lowerbound.
         */
        LOWERBOUND,
        /**
         * Count is unreliable.
         */
        FAILURE
    }

    private final MBoundResultType fResultType;
    private final Double fBound;
    private final Double fErrorProbability;

    /**
     * Create a MBoundResult.
     * @param aResultType - type of result.
     * @param aBound - count bound.
     * @param aErrorProbability - error probability.
     */
    public MBoundResult(MBoundResultType aResultType, Double aBound, Double aErrorProbability) {

        if (aResultType == null) throw new IllegalArgumentException("Result Type cannot be null.");

        fResultType = aResultType;

        if (aResultType.equals(MBoundResultType.FAILURE)) {

            fBound = Double.NaN;
            fErrorProbability = Double.NaN;

        } else {

            if (aBound == null) throw new IllegalArgumentException("Bound cannot be null.");
            fBound = new Double(aBound);

            if (aErrorProbability > 1 || aErrorProbability < 0) throw new IllegalArgumentException("Error probability must be betwee 0 and 1, inclusive.");
            fErrorProbability = aErrorProbability;

        }
    }
    
    /**
     * @return approximate count result type.
     */
    public MBoundResultType getResultType() {
        return fResultType;
    }
    
    /**
     * @return approximate count bound.
     */
    public Double getBound() {
        return fBound;
    }

    /**
     * @return probability of error.
     */
    public Double getProbabilityOfError() {
        return fErrorProbability;
    }

    @Override
    public String toString() {
        return "MBoundResult("
                +fResultType+","
                +"Bound:"+fBound+","
                +"ErrorProbability:"+fErrorProbability
                +")";
    }

}
