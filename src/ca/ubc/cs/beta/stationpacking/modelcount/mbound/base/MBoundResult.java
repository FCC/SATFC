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
