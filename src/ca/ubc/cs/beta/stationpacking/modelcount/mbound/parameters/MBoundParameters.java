package ca.ubc.cs.beta.stationpacking.modelcount.mbound.parameters;

public class MBoundParameters {
    
    private final Integer fXorClauseSize;
    private final Integer fNumXorClauses;
    private final Integer fNumTrials;
    
    private final Double fDeviation;
    private final Double fPrecisionSlack;
    
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
        
        fXorClauseSize = aXorClauseSize;
        fNumXorClauses = aNumXorClauses;
        fNumTrials = aNumTrials;
    }
    
    public Integer getXorClauseSize() {
        return fXorClauseSize;
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
