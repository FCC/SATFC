package ca.ubc.cs.beta.stationpacking.modelcount.mbound.parameters;

public class MBoundParameters {
    
    private final Integer fXorClauseSizeToNumVarsRatio;
    private final Integer fNumXorClauses;
    private final Integer fNumTrials;
    
    private final Double fDeviation;
    private final Double fPrecisionSlack;
    
    /**
     * 
     * @param aXorClauseSizeToNumVarsRatio the size of the streamlined XOR clauses as a percentage of the number of variables. (0 to 0.5)
     * @param aNumXorClauses number of streamlined XOR clauses.
     * @param aNumTrials the number of repetitions or trials.
     * @param aDeviation the deviation from the 50-50. Higher deviation will produce better bounds but may fail to find a bound. (0 to 0.5)
     * @param aPrecisionSlack a precision slack variable. (>=1)
     */
    public MBoundParameters(Integer aXorClauseSizeToNumVarsRatio, Integer aNumXorClauses, Integer aNumTrials, Double aDeviation, Double aPrecisionSlack) {
        
        if (aXorClauseSizeToNumVarsRatio > 0 && aXorClauseSizeToNumVarsRatio <= 0.5) {
            fXorClauseSizeToNumVarsRatio = aXorClauseSizeToNumVarsRatio;
        } else {
            throw new IllegalArgumentException("Size of XOR clauses to number of variables must be between 0 (exclusive) and 0.5 (inclusive).");
        }
        
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
        
        fNumXorClauses = aNumXorClauses;
        fNumTrials = aNumTrials;
    }
    
    public Integer getXorClauseSizeRatio() {
        return fXorClauseSizeToNumVarsRatio;
    }
    
    public Integer getNumXorClauses() {
        return fNumXorClauses;
    }
    
    public Integer getNumTrials() {
        return fNumTrials;
    }
    
    public Double getDeviation() {
        return fDeviation;
    }
    
    public Double getPrecisionSlack() {
        return fPrecisionSlack;
    }    
    
}
