package ca.ubc.cs.beta.stationpacking.execution.parameters.datamanagement;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.AsyncCachedCNFLookup;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.HybridCNFResultLookup;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Data Management Options",description="Parameters used to set up data management structures.")
public class DataManagementParameters extends AbstractOptions {
	
	@Parameter(names = "-CNF_DIR", description = "Directory location where to write CNFs. Will be created if inexistant.",required=true)
	public String CNFDirectory;
	
	@Parameter(names = "-CNFLOOKUP_OUTPUT_FILE", description = "Name of the file to store CNF results (the file is saved in the CNF directory).")
	public String CNFOutputName = "CNFOutput";

	public HybridCNFResultLookup getHybridCNFResultLookup()
	{
		Logger log = LoggerFactory.getLogger(DataManagementParameters.class);
		
		log.info("Checking if the CNF directory exists...");
		
		File aCNFDir = new File(CNFDirectory);
		
		if(aCNFDir.exists())
		{
			log.info("CNF directory exists.");
		}
		else
		{
			log.info("CNF directory does not exist, creating it.");
			aCNFDir.mkdir();
		}
		
		log.info("Creating the result/CNF lookup...");
		return new HybridCNFResultLookup(CNFDirectory, CNFOutputName);
	}
	
	public AsyncCachedCNFLookup getAsyncCachedCNFLookup()
	{
Logger log = LoggerFactory.getLogger(DataManagementParameters.class);
		
		log.info("Checking if the CNF directory exists...");
		
		File aCNFDir = new File(CNFDirectory);
		
		if(aCNFDir.exists())
		{
			log.info("CNF directory exists.");
		}
		else
		{
			log.info("CNF directory does not exist, creating it.");
			aCNFDir.mkdir();
		}
		
		log.info("Creating the result/CNF lookup...");
		return new AsyncCachedCNFLookup(CNFDirectory);
	}

	
}
