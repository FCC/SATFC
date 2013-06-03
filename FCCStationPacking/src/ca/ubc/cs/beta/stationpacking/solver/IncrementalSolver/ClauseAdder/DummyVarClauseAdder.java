package ca.ubc.cs.beta.stationpacking.solver.IncrementalSolver.ClauseAdder;

import java.util.HashSet;
import java.util.Set;


import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.solver.IncrementalSolver.SATLibraries.GlueMiniSatLibrary;
import ca.ubc.cs.beta.stationpacking.solver.IncrementalSolver.SATLibraries.IIncrementalSATLibrary;


public class DummyVarClauseAdder implements IClauseAdder {
	
	
	final IConstraintManager fConstraintManager;
	IIncrementalSATLibrary fIncrementalSAT;
	final Integer fNumDummyVariables;
	Instance fCurrentInstance;
	Set<Integer> fIncluded = new HashSet<Integer>();
	Set<Integer> fNotIncluded = new HashSet<Integer>();
	int numVarsPerStation;
	int curCount = 1;
	
	public DummyVarClauseAdder(IConstraintManager aConstraintManager, IIncrementalSATLibrary aIncrementalSAT, Integer aNumDummyVariables){
		fConstraintManager = aConstraintManager;
		//fIncrementalSAT = aIncrementalSAT;
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
	 */
	
	@Override
	//If stations are superset and channels are subset (or if we had no instance previously), go incremental
	public SolverResult updateAndSolve(Instance aInstance, Boolean priceCheck) {
		if(	(aInstance.getStations().containsAll(fCurrentInstance.getStations()) &&
				fCurrentInstance.getChannels().containsAll(aInstance.getChannels())) ||
				fCurrentInstance.getChannels().isEmpty()){
			//add clauses as needed - remember to include curCount
				
		} else {
			reset();
			//add clauses as needed - remember to include curCount
		}
		//Include clauses involving dummy variable curCount
		fIncluded.add(curCount);
		fNotIncluded.remove(curCount);
		long startTime = System.currentTimeMillis();
		SATResult aResult = fIncrementalSAT.solve(fNotIncluded,fIncluded);
		long elapsedTime = (System.currentTimeMillis()-startTime)/1000;
		//if SAT and it's not a price check, update current model
		if(aResult == SATResult.SAT && !priceCheck){
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
		fIncrementalSAT = new GlueMiniSatLibrary();
		fCurrentInstance = new Instance(new HashSet<Station>(),new HashSet<Integer>());
		curCount = 1;
		fIncluded.clear();
		for(int i = 1; i <= fNumDummyVariables; i++) fNotIncluded.add(i);
	}

}
