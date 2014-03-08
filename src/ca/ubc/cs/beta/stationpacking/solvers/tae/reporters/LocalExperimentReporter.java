package ca.ubc.cs.beta.stationpacking.solvers.tae.reporters;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;

public class LocalExperimentReporter implements IExperimentReporter{

	private String fReportDirectory;
	private String fExperimentName;
	
	public LocalExperimentReporter(String aReportDirectory, String aExperimentName)
	{
		fReportDirectory = aReportDirectory;
		fExperimentName = aExperimentName;
	}
	
	@Override
	public void report(StationPackingInstance aInstance, SolverResult aRunResult){
		
		String aLine = aInstance.toString()+","+aRunResult.toString()+"\n";
		
		try 
		{
			FileUtils.writeStringToFile(new File(fReportDirectory+File.separatorChar+fExperimentName+".csv"),aLine, true);
		} 
		catch (IOException e) 
		{
			throw new IllegalStateException("Writing experiment reporter string to file failed for some reason.",e);
		}
		
	}

}
