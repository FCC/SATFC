package ca.ubc.cs.beta.stationpacking.solver.IncrementalSolver;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.IncrementalSolver.SATLibraries.GlueMiniSatLibrary;
import ca.ubc.cs.beta.stationpacking.solver.IncrementalSolver.SATLibraries.IIncrementalSATLibrary;

public class IncrementalSolver implements ISolver{
	
	IConstraintManager fConstraintManager;
	ICNFEncoder fEncoder;
	IIncrementalSATLibrary fIncrementalSATLibrary;
	double fSeed;
	
	Instance fCurrentInstance;
	
	Clause fAssumptions = new Clause(new HashSet<Integer>(),new HashSet<Integer>());
	Set<Integer> fIncluded = new HashSet<Integer>();
	Set<Integer> fNotIncluded = new HashSet<Integer>();
	Integer curCount = 1;
	
	final Integer fNumDummyVariables;

	
	private static Logger log = LoggerFactory.getLogger(IncrementalSolver.class);
	

	//pass other parameters (such as incremental technique, how many dummy variables to store) as one final arg to constructor
	public IncrementalSolver(	IConstraintManager aConstraintManager, ICNFEncoder aCNFEncoder, 
								IIncrementalSATLibrary aIncrementalSATLibrary, Integer aNumDummyVariables, double aSeed){
		fConstraintManager = aConstraintManager;
		fIncrementalSATLibrary = aIncrementalSATLibrary;
		fEncoder = aCNFEncoder;
		fSeed = aSeed;
		fNumDummyVariables = aNumDummyVariables;
		reset(); //TODO - currently initializes fIncrementalSAT with GLueMiniSatLibrary
		//Only reason that it can't be completely agnostic is that I don't see a "clear" function in GlueMiniSat
		//so it has to "know" what type to re-initialize with when solving from scratch
	}
	
	/*
	 * NA - think about when to set numVarsPerStation, how to initialize set of stations 
	 * Do we need all variables up front, or not?
	 * Think about decoding instances - how to get a consistent map from station to variable?
	 * Probably want a little method that goes from (station,channel) -> variable.
	 * Also should be told whether to save state (never do if you get UNSAT, but if you get SAT...)
	 */
	
	@Override
	//If stations are superset and channels are subset (or if we had no instance previously), go incremental
	public SolverResult solve(Instance aInstance, double aCutoff) {
		if(	(aInstance.getStations().containsAll(fCurrentInstance.getStations()) &&
				fCurrentInstance.getChannels().containsAll(aInstance.getChannels())) ||
				fCurrentInstance.getChannels().isEmpty()){
			//add clauses as needed - remember to include curCount
				
		} else {
			reset();
			//add clauses as needed - remember to include curCount
		}
		//Include clauses involving dummy variable curCount
		
		//Use fAssumptions
		
		
		fIncluded.add(curCount);
		fNotIncluded.remove(curCount);
		long startTime = System.currentTimeMillis();
		SATResult aResult = fIncrementalSATLibrary.solve(fNotIncluded,fIncluded);
		long elapsedTime = (System.currentTimeMillis()-startTime)/1000;
		//if SAT and it's not a price check, update current model
		if(aResult == SATResult.SAT){
			fCurrentInstance = aInstance;
		} else {
			//Rollback - "remove" the most recent clauses
			fNotIncluded.add(curCount);
			fIncluded.remove(curCount);
		}
		if(curCount==fNumDummyVariables) reset();
		else curCount++; //next time, use a new dummy variable
		return new SolverResult(aResult,elapsedTime);
	}
	
	
	private void reset(){
		fIncrementalSATLibrary = new GlueMiniSatLibrary();
		fCurrentInstance = new Instance(new HashSet<Station>(),new HashSet<Integer>());
		curCount = 1;
		fIncluded.clear();
		for(int i = 1; i <= fNumDummyVariables; i++) fNotIncluded.add(i);
	}

}
