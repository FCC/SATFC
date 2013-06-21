package ca.ubc.cs.beta.stationpacking.execution.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.stationpacking.solver.taesolver.cnflookup.HybridCNFResultLookup;

import com.beust.jcommander.Parameter;

@UsageTextField(title="FCC Station Packing Packing Data Management Options",description="Parameters used to set up data management structures.")
public class DataManagementParameters extends AbstractOptions {
	
	@Parameter(names = "-CNF_DIR", description = "Directory location where to write CNFs.",required=true)
	public String fCNFDirectory;
	
	@Parameter(names = "-CNFLOOKUP_OUTPUT_FILE", description = "Name of the file to store CNF results (will be stored in the CNF directory).")
	private String fCNFOutputName = "CNFOutput";

	public HybridCNFResultLookup getHybridCNFResultLookuo()
	{
		Logger log = LoggerFactory.getLogger(DataManagementParameters.class);
		log.info("Creating the result/CNF lookup...");
		return new HybridCNFResultLookup(fCNFDirectory, fCNFOutputName);
	}

	
}
