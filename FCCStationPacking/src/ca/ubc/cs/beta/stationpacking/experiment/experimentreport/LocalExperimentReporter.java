package ca.ubc.cs.beta.stationpacking.experiment.experimentreport;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import ca.ubc.cs.beta.stationpacking.experiment.instance.IInstance;
import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SolverResult;

public class LocalExperimentReporter implements IExperimentReporter{

	private String fReportDirectory;
	private String fExperimentName;
	
	public LocalExperimentReporter(String aReportDirectory, String aExperimentName)
	{
		fReportDirectory = aReportDirectory;
		fExperimentName = aExperimentName;
	}
	
	@Override
	public void report(IInstance aInstance, SolverResult aRunResult){
		
		String aLine = aInstance.toString()+","+Double.toString(aRunResult.getRuntime())+","+aRunResult.getResult().toString()+"\n";
		
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
