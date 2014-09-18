package ca.ubc.cs.beta.stationpacking.modelcount.mbound.parameters;

/**
 * Parameters for the MBound algorithm.
 * @author tqichen
 */
public class MBoundParameters {

    private final Integer fXorClauseSize;
    private final Integer fNumXorClauses;
    private final Integer fNumTrials;

    private final Double fDeviation;
    private final Double fPrecisionSlack;

    /**
     * 
     * @param aXorClauseSize - size of the XOR clauses to add.
     * @param aXorClauseSizeToNumVarsRatio the size of the streamlined XOR clauses as a percentage of the number of variables. (0 to 0.5)
     * @param aNumXorClauses number of streamlined XOR clauses.
     * @param aNumTrials the number of repetitions or trials.
     * @param aDeviation the deviation from the 50-50. Higher deviation will produce better bounds but may fail to find a bound. (0 to 0.5)
     * @param aPrecisionSlack a precision slack variable. (>=1)
     */
    public MBoundParameters(Integer aXorClauseSize, Integer aNumXorClauses, Integer aNumTrials, Double aDeviation, Double aPrecisionSlack) {

        if (aDeviation > 0 && aDeviation <= 0.5) {
            fDeviation = aDeviation;
        } else {
            throw new IllegalArgumentException("Deviation parameter must be between 0 (exclusive) and 0.5 (inclusive). Given " +aDeviation+".");
        }

        if (aPrecisionSlack >= 1) {
            fPrecisionSlack = aPrecisionSlack;
        } else {
            throw new IllegalArgumentException("Precision slack parameter must be greater than or equal to 1. Given "+aPrecisionSlack+".");
        }

        if (aXorClauseSize <= 0) throw new IllegalArgumentException("XOR clause size must be a positive integer");
        if (aNumXorClauses <= 0) throw new IllegalArgumentException("Number of XOR clauses must be a positive integer");
        if (aNumTrials <= 0) throw new IllegalArgumentException("Number of trials must be a positive integer");
        
        fXorClauseSize = aXorClauseSize;
        fNumXorClauses = aNumXorClauses;
        fNumTrials = aNumTrials;
    }
    
    /**
     * @return the XOR clause size.
     */
    public Integer getXorClauseSize() {
        return fXorClauseSize;
    }

    /**
     * @return the number of XOR clauses to add.
     */
    public Integer getNumXorClauses() {
        return fNumXorClauses;
    }

    /**
     * @return the number of trials to make.
     */
    public Integer getNumTrials() {
        return fNumTrials;
    }
    
    /**
     * @return the deviation to tolerate.
     */
    public Double getDeviation() {
        return fDeviation;
    }

    /**
     * @return the precision slack to tolerate.
     */
    public Double getPrecisionSlack() {
        return fPrecisionSlack;
    }    

}
