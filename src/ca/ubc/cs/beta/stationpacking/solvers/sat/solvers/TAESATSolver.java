package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.CNFCompressor;

/**
 * Lean TAE based SAT solver.
 * @author afrechet
 */
public class TAESATSolver implements ISATSolver{
	
	private final TargetAlgorithmEvaluator fTAE;
	private final ParamConfiguration fParamConfiguration;
	private final String fCNFDir;
	
	/**
	 * Builds a TAE based SAT solver.
	 * @param aTAE - the TAE to execute a SAT solver.
	 * @param aParamConfigurationSpace - a param configuration space for the TAE.
	 * @param aCNFDir - a CNF directory in which to execute 
	 */
	public TAESATSolver(TargetAlgorithmEvaluator aTAE, ParamConfiguration aParamConfiguration, String aCNFDir)
	{
		fTAE = aTAE;
		fParamConfiguration = aParamConfiguration;
		fCNFDir = aCNFDir;
		
	}
	
	
	@Override
	public SATSolverResult solve(CNF aCNF, double aCutoff, long aSeed) {
		
		CNFCompressor aCompressor = new CNFCompressor();
		
		//Setup the CNF file and filename.
		String aCNFFilename = fCNFDir + File.separator + RandomStringUtils.randomAlphabetic(15)+".cnf"; 
		File aCNFFile = new File(aCNFFilename);

		String aCNFString = aCompressor.compress(aCNF).toDIMACS(new String[]{"FCC Station packing instance."});
		
		//Write the CNF to disk.
		try 
		{
			FileUtils.writeStringToFile(aCNFFile, aCNFString);
		} 
		catch (IOException e) 
		{
			throw new IllegalStateException("Could not write CNF to file ("+e.getMessage()+").");
		}
		
		//Create the run config.
		ProblemInstance aProblemInstance = new ProblemInstance(aCNFFilename);
		ProblemInstanceSeedPair aProblemInstanceSeedPair = new ProblemInstanceSeedPair(aProblemInstance,aSeed);
		RunConfig aRunConfig = new RunConfig(aProblemInstanceSeedPair, aCutoff, fParamConfiguration);
		
		//Execute it.
		List<AlgorithmRun> aRuns = fTAE.evaluateRun(aRunConfig);
		if(aRuns.size()!=1)
		{
			throw new IllegalStateException("Got multiple runs back from the TAE when solving a single CNF.");
		}
		AlgorithmRun aRun = aRuns.iterator().next();
		double aRuntime = aRun.getRuntime();				
		SATResult aResult;
		HashSet<Litteral> aAssignment = new HashSet<Litteral>();
		
		//Post process the result from the TAE.
		switch (aRun.getRunResult()){
			case KILLED:
				aResult = SATResult.KILLED;
				break;
			case SAT:
				aResult = SATResult.SAT;
				
				//Grab assignment
				String aAdditionalRunData = aRun.getAdditionalRunData();
				
				//The TAE wrapper is assumed to return a ';'-separated string of literals, one literal for each variable of the SAT problem.
				for(String aLiteral : aAdditionalRunData.split(";"))
				{
					boolean aSign = !aLiteral.contains("-"); 
					long aVariable = Long.valueOf(aLiteral.replace("-", ""));
					
					aAssignment.add(new Litteral(aCompressor.decompress(aVariable),aSign));
				}
				break;
			case UNSAT:
				aResult = SATResult.UNSAT;
				break;
			case TIMEOUT:
				aResult = SATResult.TIMEOUT;
				break;
			default:
				aResult = SATResult.CRASHED;
				throw new IllegalStateException("Run "+aRun+" crashed!");
				
		}
		
		//Clean up
		aCNFFile.delete();
		
		return new SATSolverResult(aResult,aRuntime,aAssignment);
		
	}


	@Override
	public void notifyShutdown() {
		fTAE.notifyShutdown();
	}

}
