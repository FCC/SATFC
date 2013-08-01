package ca.ubc.cs.beta.stationpacking.solver.incrementalsolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.IIncrementalSATLibrary;

public class IncrementalSolver implements ISolver{
	
	/*
	 * Used to encode the Instance
	 */
	IConstraintManager fConstraintManager;
	ICNFEncoder fEncoder;


	/*
	 * curCount is the next dummy variable to use. Each added clause contains the literal !(-curCount),
	 * so this effectively counts the number of calls to solve() since the last reset.
	 */
	Integer curCount;
	
	/*
	 * The most recently solved instance - helps us decide whether we can use incremental capability.
	 */
	StationPackingInstance fCurrentInstance;
	/*
	 * We keep fCurrentClauses around because it makes it easy to figure out which clauses 
	 * are new and should be added to the IncrementalSATLibrary. Without fCurrentClauses, we would
	 * either have to reason about this ourselves, or add ALL clauses from each instance to the library.
	 * 
	 * Note that Clauses in fCurrentClauses DO NOT include the dummy variables 
	 * (so that we can compare them to clauses produced by the CNFEncoder).
	 */
	Set<Clause> fCurrentClauses;
	
	/*
	 * When we solve with assumptions, we "activate" clauses for which the corresponding variable is true
	 */
	Clause fAssumptions;
	
	/* 
	 * For resetting to a flagged state (Not yet implemented)
	 */
	
	/*
	Instance fFlaggedInstance = new Instance();
	Set<Clause> fFlaggedClauses = new HashSet<Clause>(); //can't recover these from fFlaggedInstance because we may have added "side" clauses
	Clause fFlaggedAssumptions = new Clause();
	boolean fResetFlag = false; //'true' indicates that we have called reset() more recently than flagState()
	*/

	
	/*
	 * Used to solve the Instance
	 */
	IIncrementalSATLibrary fIncrementalSATLibrary;
	
	private static Logger log = LoggerFactory.getLogger(IncrementalSolver.class);

		

	/* 
	 * The constructor. We may want to add other parameters indicating what "mode" to run the solver in.
	 */
	public IncrementalSolver(	IConstraintManager aConstraintManager, ICNFEncoder aCNFEncoder, 
								IIncrementalSATLibrary aIncrementalSATLibrary){
		fConstraintManager = aConstraintManager;
		fEncoder = aCNFEncoder;		
		fIncrementalSATLibrary = aIncrementalSATLibrary;
		reset();
	}
	
	/* 
	 * This method adds clauses as needed to the IncrementalSATLibrary. 
	 * 
	 * If the resulting problem is UNSAT, we "remove" these clauses by 
	 * toggling the dummy variable and do not update fCurrentInstance, fCurrentClauses.
	 * If the resulting problem is SAT, we update our state. 
	 * 
	 * In the future, we may want to not always save our state when finding SAT.
	 * One way to do this would be to include another parameter to the solve() method.
	 * Another way would be to implement the "flagState" and "resetToFlaggedState" methods.
	 * 
	 * Also, we currently reset only when absolutely necessary; if we get many UNSAT instances
	 * in a row, there will be many superfluous clauses in the IncrementalSATLibrary (each of which
	 * we deactivate by passing appropriate assumptions about the dummy variables). I believe that
	 * this should be fine, but it's possible that too many of these will slow the solver down.
	 */
	
	@Override
	public SolverResult solve(StationPackingInstance aInstance, double aCutoff, long aSeed) throws Exception {

		//We can only go incremental if stations are superset and channels are subset; else reset
		if(	(!aInstance.getStations().containsAll(fCurrentInstance.getStations())) ||
			(!fCurrentInstance.getChannels().containsAll(aInstance.getChannels()))){
			reset();
		}
		
		//Get clauses associated with the new instance
		Set<Clause> aNewClauses = fEncoder.encode(aInstance, fConstraintManager);
				
		/*
		 * Add all NEW clauses to our clause set (including a dummy variable to "toggle" them).
		 * Create aCopyClause so that we have two versions of each clause: 
		 * the original (no dummy variable, possibly added to fCurrentClauses), and
		 * the copy (including the dummy variable, added to the solver)
		 */
		Set<Clause> aNewlyAddedClauses = new HashSet<Clause>();
		for(Clause aClause : aNewClauses){
			if(!fCurrentClauses.contains(aClause)){
				aNewlyAddedClauses.add(aClause);
				Clause aCopyClause = new Clause(aClause.getVars(),aClause.getNegatedVars());
				aCopyClause.addLiteral(-curCount, false);
				fIncrementalSATLibrary.addClause(aCopyClause);
			}
		}
		
		//log.info("Solving instance: "+aInstance.getStations()+","+aInstance.getChannels());
		
		//Set fAssumptions to "activate" the newly added clauses and solve
		fAssumptions.addLiteral(-curCount,true);
		fAssumptions.removeLiteral(-curCount,false);
		

		long startTime = System.currentTimeMillis();
		SATResult aResult = fIncrementalSATLibrary.solve(fAssumptions, aCutoff);
		double elapsedTime = new Double(System.currentTimeMillis()-startTime)/1000;
		
		Map<Integer,Set<Station>> aStationAssignment = new HashMap<Integer,Set<Station>>();
		
		//if(SAT), get the assignment and update current instance
		if(aResult == SATResult.SAT){

			//Get the assignment from the IncrementalSATLibrary
			Clause aAssignment = fIncrementalSATLibrary.getAssignment();	

			try{
				//Check to make sure that all assumptions are satisfied
				for(Integer aVar : fAssumptions.getVars()){
					if(!aAssignment.removeLiteral(aVar, true)) throw new IllegalStateException("Assumption Not Satisfied: tried to remove "+aVar);
				}
				for(Integer aNegatedVar : fAssumptions.getNegatedVars()){
					if(!aAssignment.removeLiteral(aNegatedVar, false)){
						throw new IllegalStateException("Assumption Not Satisfied: tried to remove -"+aNegatedVar);
					}
				}
				
				//Decode the assignment
				aStationAssignment = fEncoder.decode(aInstance, aAssignment);
				if(!fConstraintManager.isSatisfyingAssignment(aStationAssignment)){
					throw new IllegalStateException("When decoding station assignment, violated pairwise interference constraints found.");
				} else {
					log.info("Successfully verified feasibility of assignment.");
				}
				//Update current instance and current clauses
				fCurrentInstance = aInstance;
				fCurrentClauses.addAll(aNewlyAddedClauses);
			} catch(Exception e){
				//Error decoding or unsatisfied assumption - something is wrong!
				//Rollback - "remove" the most recent clauses by deactivating them
				fAssumptions.addLiteral(-curCount,false);
				fAssumptions.removeLiteral(-curCount,true);
				e.printStackTrace();
			}
			
		} else { 
			//Rollback - "remove" the most recent clauses by deactivating them
			fAssumptions.addLiteral(-curCount,false);
			fAssumptions.removeLiteral(-curCount,true);
		}
		
		curCount++; //next time, use a new dummy variable
		return new SolverResult(aResult,elapsedTime,aStationAssignment);
	}
	
	
	private void reset(){
		log.info("Cannot use incremental capability, re-setting...");
		fIncrementalSATLibrary.clear();
		fCurrentInstance = new StationPackingInstance();
		fCurrentClauses = new HashSet<Clause>();
		fAssumptions = new Clause();
		curCount = 1;
		//fResetFlag = true; //Will be used when solver interface is extended
	}

	@Override
	public void notifyShutdown() {
		
	}
	
	/* The following three methods are for an extension of the solver capability.
	 * Currently unimplemented.
	 */
	
	
	/*
	public void flagState(){
		fFlaggedInstance = new Instance(fCurrentInstance.getStations(),fCurrentInstance.getChannels());
		fFlaggedClauses = new HashSet<Clause>(fCurrentClauses);
		fFlaggedAssumptions = new Clause(fAssumptions.getVars(),fAssumptions.getNegatedVars());
		fResetFlag = false;
	}
	*/

	/*
	public void resetToFlaggedState(){
		if(!fResetFlag){ //We haven't reset since our last flagged state
			fCurrentInstance = fFlaggedInstance;
			fCurrentClauses = fFlaggedClauses;
			for(int i = 1; i < curCount; i++){ //Make sure to "turn off" clauses that have been added since the Flag
				if(!fFlaggedAssumptions.getVars().contains(i)) fFlaggedAssumptions.addLiteral(i, false);
			}
			fAssumptions = fFlaggedAssumptions;
		} else { //We have reset, so we need to do so again, re-populate fIncrementalSATLibrary with clauses
			reset();
			fCurrentInstance = fFlaggedInstance;
			fCurrentClauses = fFlaggedClauses;
			for(Clause aClause : fCurrentClauses){
				Clause aCopyClause = new Clause(aClause.getVars(),aClause.getNegatedVars());
				aCopyClause.addLiteral(-curCount, false);
				fIncrementalSATLibrary.addClause(aCopyClause);
			}
			curCount++;
		}

		flagState(); //Called to create "new" FlaggedInstance and Assumptions (so that modifications to fCurrent don't change fFlagged)
	}
	*/

	//public void addClauseAndSolve(/*some arguments*/){}
}
