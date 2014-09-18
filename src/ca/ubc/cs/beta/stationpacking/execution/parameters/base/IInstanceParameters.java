package ca.ubc.cs.beta.stationpacking.execution.parameters.base;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;

/**
 * Interface for station packing instance building parameters.
 * @author afrechet
 */
public interface IInstanceParameters {
    
    /**
     * @return the interference data foldername.
     */
    public String getInterferenceData();
    
    /**
     * @return the instance cutoff time.
     */
    public double getCutoff();
    
    /**
     * @param aStationManager - a station manager to select instance stations from.
     * @return station packing instance represented by parameters.
     */
    public StationPackingInstance getInstance(IStationManager aStationManager);
}
