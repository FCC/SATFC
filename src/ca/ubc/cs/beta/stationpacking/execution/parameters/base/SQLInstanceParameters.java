package ca.ubc.cs.beta.stationpacking.execution.parameters.base;

import java.util.List;

import org.apache.commons.math3.util.Pair;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.EncodedInstanceToCNFConverter;

import com.beust.jcommander.Parameter;

@UsageTextField(title="SQL Instance Options",description="Parameters necessary to define a station packing SQL instance.")
public class SQLInstanceParameters extends AbstractOptions implements IInstanceParameters {
    
    private static class SQLInstance
    {
        private final StationPackingInstance fInstance;
        private final String fConfigData;
        
        public SQLInstance(String aFilename,String aInterferenceConfigFoldername)
        {
            List<Pair<StationPackingInstance,String>> instances = EncodedInstanceToCNFConverter.readSQLInstancesFromFile(aFilename, aInterferenceConfigFoldername, null);
            
            if(instances.isEmpty())
            {
                throw new IllegalArgumentException("Could not load any instance from "+aFilename+".");
            }
            else if(instances.size()>1)
            {
                throw new IllegalArgumentException("Loaded more than once instance from "+aFilename+".");
            }
            else
            {
                fInstance = instances.get(0).getFirst();
                fConfigData = instances.get(0).getSecond();
            }
        }
    }
    
    private SQLInstance fInstance = null;
    private boolean fSet = false;
    
    @Parameter(names = "-SQL-INSTANCE", description = "SQL instance file.")
    private String fSQLInstance;
    
    @Parameter(names = "-SQL-CONFIG-FOLDER", description = "SQL config foldername")
    private String fConfigFolder; 
    
    @Parameter(names = "-SQL-CUTOFF", description = "Cutoff time for SQL instance.")
    private double fCutoff = 60.0;
    
    private void setInstance()
    {
        if(!fSet)
        {
            if(fSQLInstance == null)
            {
                throw new IllegalArgumentException("Cannot create instance when SQL instance file is null.");
            }
            if(fConfigFolder == null)
            {
                throw new IllegalArgumentException("Cannot create instance when SQL config folder is null.");
            }   
            
            fInstance = new SQLInstance(fSQLInstance, fConfigFolder);
            fSet = true;
        }
    }
    
    
    @Override
    public String getData() {
        
        setInstance();
        return fInstance.fConfigData;
    }

    @Override
    public double getCutoff() {
        return fCutoff;
    }

    @Override
    public StationPackingInstance getInstance(IStationManager aStationManager) {
        setInstance();
        return fInstance.fInstance;
    }

}
