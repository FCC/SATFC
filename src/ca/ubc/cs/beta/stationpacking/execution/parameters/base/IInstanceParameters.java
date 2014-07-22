package ca.ubc.cs.beta.stationpacking.execution.parameters.base;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

public interface IInstanceParameters {
    public String getData();
    
    public double getCutoff();
    
    public StationPackingInstance getInstance(IStationManager aStationManager);
}
