package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.nonincremental;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Literal;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractCompressedSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;

/**
 * Lean TAE based SAT solver.
 * @author afrechet
 */
public class TAESATSolver extends AbstractCompressedSATSolver{
	
	private final TargetAlgorithmEvaluator fTAE;
	private final AlgorithmExecutionConfiguration fExecConfig;
	private final ParameterConfiguration fParamConfiguration;
	private final String fCNFDir;
	
	/**
	 * Builds a TAE based SAT solver.
	 * @param aTargetAlgorithmEvaluator - target algorithm evaluator to use.
	 * @param aExecutionConfig - the execution config for the SAT solver we want to execute.
	 * @param aParamConfig - the parameter configuration for the SAT solver we want to execute.
	 * @param aCNFDir - a CNF directory in which to execute. 
	 */
	public TAESATSolver(TargetAlgorithmEvaluator aTargetAlgorithmEvaluator,
			ParameterConfiguration aParamConfig,
			AlgorithmExecutionConfiguration aExecConfig,
			String aCNFDir)
	{
		fTAE = aTargetAlgorithmEvaluator;
		fExecConfig = aExecConfig;
		fParamConfiguration = aParamConfig;
		fCNFDir = aCNFDir;
	}
	
	
	@Override
	public SATSolverResult solve(CNF aCNF, ITerminationCriterion aTerminationCriterion, long aSeed) {
		
		//Setup the CNF file and filename.
		String aCNFFilename = fCNFDir + File.separator + RandomStringUtils.randomAlphabetic(15)+".cnf";
		File aCNFFile = new File(aCNFFilename);
		while(aCNFFile.exists())
		{
			aCNFFilename = fCNFDir + File.separator + RandomStringUtils.randomAlphabetic(15)+".cnf";
			aCNFFile = new File(aCNFFilename);
		}

		String aCNFString = aCNF.toDIMACS(new String[]{"FCC Station packing instance."});
		
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
		AlgorithmRunConfiguration aRunConfig = new AlgorithmRunConfiguration(aProblemInstanceSeedPair, aTerminationCriterion.getRemainingTime(), fParamConfiguration,fExecConfig);
		
		//Execute it.
		List<AlgorithmRunResult> aRuns = fTAE.evaluateRun(aRunConfig);
		if(aRuns.size()!=1)
		{
			throw new IllegalStateException("Got multiple runs back from the TAE when solving a single CNF.");
		}
		AlgorithmRunResult aRun = aRuns.iterator().next();
		double aRuntime = aRun.getRuntime();
		
		SATResult aResult;
		HashSet<Literal> aAssignment = new HashSet<Literal>();
		
		//Post process the result from the TAE.
		switch (aRun.getRunStatus()){
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
					
					aAssignment.add(new Literal(aVariable,aSign));
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


	@Override
	public void interrupt() throws UnsupportedOperationException 
	{
		throw new UnsupportedOperationException("TAESATSolver does not support pre-emption. (interrupts)");
	}

}
