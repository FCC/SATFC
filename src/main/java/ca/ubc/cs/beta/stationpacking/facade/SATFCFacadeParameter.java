package ca.ubc.cs.beta.stationpacking.facade;

import lombok.Data;

@Data
public class SATFCFacadeParameter {
	
	private final String claspLibrary;
	private final boolean initializeLogging;
	private final String cNFDirectory;
	private final String resultFile;
	private final SolverChoice solverChoice;
	private final SolverCustomizationOptions options;

	public static enum SolverChoice
	{
		SATFC,
		MIPFC;
	}

	@Data
	public static class SolverCustomizationOptions {
		private boolean presolve = true;
		private boolean underconstrained = true;
		private boolean decompose = true;
	}

}

