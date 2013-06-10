package ca.ubc.cs.beta.stationpacking.solver.incrementalsolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.Instance;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder2;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.IIncrementalSATLibrary;

public class IncrementalSolver implements ISolver{
	
	//Used to encode the Instance
	IConstraintManager fConstraintManager;
	ICNFEncoder2 fEncoder;


	//Each added clause contains the literal !curCount 
	//(when we solve with assumptions, we "activate" clauses for which the corresponding variable is true)
	Clause fAssumptions;
	Integer curCount;
	//final Integer fNumDummyVariables;
	
	//Clauses in fCurrentClauses DO NOT include the dummy variables
	Instance fCurrentInstance;
	Set<Clause> fCurrentClauses;
	
	//Used to solve the Instance
	IIncrementalSATLibrary fIncrementalSATLibrary;
	double fSeed;
	
	private static Logger log = LoggerFactory.getLogger(IncrementalSolver.class);

		

	//pass other parameters (such as incremental technique, how many dummy variables to store) as one final arg to constructor
	public IncrementalSolver(	IConstraintManager aConstraintManager, ICNFEncoder2 aCNFEncoder, 
								IIncrementalSATLibrary aIncrementalSATLibrary, double aSeed){
		fConstraintManager = aConstraintManager;
		fEncoder = aCNFEncoder;

		//fNumDummyVariables = aNumDummyVariables;
		
		fIncrementalSATLibrary = aIncrementalSATLibrary;
		fSeed = aSeed;
		reset();
	}
	
	/*
	 * Do we need all variables up front, or not?
	 * Also should be told whether to save state (never do if you get UNSAT, but if you get SAT...)
	 */
	
	@Override
	//If stations are superset and channels are subset (or if we had no instance previously), go incremental
	public SolverResult solve(Instance aInstance, double aCutoff) throws Exception {
		Set<Clause> aNewClauses = fEncoder.encode(aInstance, fConstraintManager);
		aNewClauses = fEncoder.encode(aInstance, fConstraintManager);
		if(	(!aInstance.getStations().containsAll(fCurrentInstance.getStations())) ||
			(!fCurrentInstance.getChannels().containsAll(aInstance.getChannels()))){
			reset(); //Can only use incremental if new station set is larger, new channel set smaller
			log.info("Cannot use incremental capability, re-setting...");
		}
		
		//log.info("Adding clauses to library...");
		
		//Add all new clauses to our clause set (including a dummy variable to "toggle" them)
		Set<Clause> aNewlyAddedClauses = new HashSet<Clause>();
		for(Clause aClause : aNewClauses){
			if(!fCurrentClauses.contains(aClause)){
				aNewlyAddedClauses.add(aClause);
				Clause aCopyClause = new Clause(aClause.getVars(),aClause.getNegatedVars());
				aCopyClause.addLiteral(-curCount, false);
				fIncrementalSATLibrary.addClause(aCopyClause);
			}
		}
		
		log.info("Solving instance: "+aInstance.getStations()+","+aInstance.getChannels());
		
		//Set fAssumptions to "activate" the newly added clauses and solve
		fAssumptions.addLiteral(-curCount,true);
		fAssumptions.removeLiteral(-curCount,false);
		long startTime = System.currentTimeMillis();
		SATResult aResult = fIncrementalSATLibrary.solve(fAssumptions);
		long elapsedTime = (System.currentTimeMillis()-startTime)/1000;
		
		Map<Integer,Set<Station>> aStationAssignment = new HashMap<Integer,Set<Station>>();
		
		//if SAT and it's not a price check, update current instance
		if(aResult == SATResult.SAT){

			Clause aAssignment = fIncrementalSATLibrary.getAssignment();
			//System.out.println(aAssignment);
			//aAssignment = fIncrementalSATLibrary.getAssignment();				

			try{
				for(Integer aVar : fAssumptions.getVars()){
					if(!aAssignment.removeLiteral(aVar, true)) throw new Exception("Assumption Not Satisfied: tried to remove "+aVar);
				}
				for(Integer aNegatedVar : fAssumptions.getNegatedVars()){
					if(!aAssignment.removeLiteral(aNegatedVar, false)){
						throw new Exception("Assumption Not Satisfied: tried to remove -"+aNegatedVar);
					}
				}
				fEncoder.translate(aAssignment, fIncrementalSATLibrary.getMap());
				aStationAssignment = fEncoder.decode(aInstance, aAssignment);
				fCurrentInstance = aInstance;
				fCurrentClauses.addAll(aNewlyAddedClauses);
			} catch(Exception e){
				fAssumptions.addLiteral(-curCount,false);
				fAssumptions.removeLiteral(-curCount,true);
				e.printStackTrace();
			}
			
			//log.info("Instance was SAT, with assignment: "+aStationAssignment);
			//get the model
		} else { //Rollback - "remove" the most recent clauses
			fAssumptions.addLiteral(-curCount,false);
			fAssumptions.removeLiteral(-curCount,true);
			
		}
		
		//if(curCount==fNumDummyVariables) reset();
		//else 
		curCount++; //next time, use a new dummy variable
		return new SolverResult(aResult,elapsedTime,aStationAssignment);
	}
	
	
	private void reset(){
		fIncrementalSATLibrary.clear();
		fCurrentInstance = new Instance();
		fCurrentClauses = new HashSet<Clause>();
		fAssumptions = new Clause();
		curCount = 1;
	}

}
