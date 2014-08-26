package ca.ubc.cs.beta.stationpacking.modelcount.mbound.base;

public class MBoundResult {
    
    public enum MBoundResultType {
        UPPERBOUND, LOWERBOUND, FAILURE
    }

    private final MBoundResultType fResultType;
    private final Double fBound;
    
    public MBoundResult(MBoundResultType aResultType, Double aBound) {
        
        if (aResultType == null) throw new IllegalArgumentException("Result Type cannot be null.");
        
        fResultType = aResultType;
        
        if (aResultType.equals(MBoundResultType.FAILURE)) {
            fBound = Double.NaN;
        } else {
            if (aBound == null) throw new IllegalArgumentException("Bound cannot be null.");
            fBound = new Double(aBound);
        }
    }
    
    public MBoundResultType getResultType() {
        return fResultType;
    }
    
    public Double getBound() {
        return fBound;
    }
    
}
