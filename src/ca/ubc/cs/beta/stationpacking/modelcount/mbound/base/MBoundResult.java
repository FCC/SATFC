package ca.ubc.cs.beta.stationpacking.modelcount.mbound.base;

public class MBoundResult {
    
    public enum MBoundResultType {
        UPPERBOUND, LOWERBOUND, FAILURE
    }

    private final MBoundResultType fResultType;
    private final Double fBound;
    private final Double fErrorProbability;
    
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
    
    public MBoundResultType getResultType() {
        return fResultType;
    }
    
    public Double getBound() {
        return fBound;
    }
    
    public Double getProbabilityOfError() {
        return fErrorProbability;
    }
    
}
