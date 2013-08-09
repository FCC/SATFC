package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Clause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental.SATSolver.IIncrementalSATSolver;

public class IncrementalSolver implements ISolver{
	
	/*
	 * Used to encode the Instance
	 */
	IConstraintManager fConstraintManager;
	ISATEncoder fEncoder;


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
	IIncrementalSATSolver fIncrementalSATLibrary;
	
	private static Logger log = LoggerFactory.getLogger(IncrementalSolver.class);

		

	/* 
	 * The constructor. We may want to add other parameters indicating what "mode" to run the solver in.
	 */
	public IncrementalSolver(	IConstraintManager aConstraintManager, ISATEncoder aCNFEncoder, 
								IIncrementalSATSolver aIncrementalSATLibrary){
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
	public SolverResult solve(StationPackingInstance aInstance, double aCutoff, long aSeed) {

		//We can only go incremental if stations are superset and channels are subset; else reset
		if(	(!aInstance.getStations().containsAll(fCurrentInstance.getStations())) ||
			(!fCurrentInstance.getChannels().containsAll(aInstance.getChannels()))){
			reset();
		}
		
		//Get clauses associated with the new instance
		Set<Clause> aNewClauses = fEncoder.encode(aInstance);
				
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
				Clause aCopyClause = new Clause();
				aCopyClause.addAll(aClause);
				aCopyClause.add(new Litteral(-curCount, false));
				fIncrementalSATLibrary.addClause(aCopyClause);
			}
		}
		
		//log.info("Solving instance: "+aInstance.getStations()+","+aInstance.getChannels());
		
		//Set fAssumptions to "activate" the newly added clauses and solve
		fAssumptions.add(new Litteral(-curCount,true));
		fAssumptions.remove(new Litteral(-curCount,false));
		

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
				for(Litteral aLitteral : fAssumptions)
				{
					if(aLitteral.getSign())
					{
						if(!aAssignment.remove(aLitteral)) throw new IllegalStateException("Assumption Not Satisfied: tried to remove "+aLitteral);
					}
					else
					{
						if(!aAssignment.remove(aLitteral)) throw new IllegalStateException("Assumption Not Satisfied: tried to remove -"+aLitteral);
					}
				}
				
				
				//Decode the assignment
				HashMap<Long,Boolean> aLitteralChecker = new HashMap<Long,Boolean>();
				for(Litteral aLitteral : aAssignment)
				{
					boolean aSign = aLitteral.getSign(); 
					long aVariable = aLitteral.getVariable();
					
					if(aLitteralChecker.containsKey(aVariable))
					{
						log.warn("A variable was present twice in a SAT assignment.");
						if(!aLitteralChecker.get(aVariable).equals(aSign))
						{
							throw new IllegalStateException("SAT assignment from TAE wrapper assigns a variable to true AND false.");
						}
					}
					else
					{
						aLitteralChecker.put(aVariable, aSign);
					}
					
					//If the litteral is positive, then we keep it as it is an assigned station to a channel.
					if(aSign)
					{
						Pair<Station,Integer> aStationChannelPair = fEncoder.decode(aVariable);
						Station aStation = aStationChannelPair.getKey();
						Integer aChannel = aStationChannelPair.getValue();
						
						if(!aInstance.getStations().contains(aStation))
						{
							throw new IllegalStateException("A decoded station "+aStation+" from a SAT assignment is not in that problem instance.");
						}
						if(!aInstance.getChannels().contains(aChannel))
						{
							throw new IllegalStateException("A decoded channel "+aChannel+" from a SAT assignment is not in that problem instance.");
						}
						if(!aStationAssignment.containsKey(aChannel))
						{
							aStationAssignment.put(aChannel, new HashSet<Station>());
						}
						aStationAssignment.get(aChannel).add(aStation);
					}
				}
				
				
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
				fAssumptions.add(new Litteral(-curCount,false));
				fAssumptions.remove(new Litteral(-curCount,true));
				e.printStackTrace();
			}
			
		} else { 
			//Rollback - "remove" the most recent clauses by deactivating them
			fAssumptions.add(new Litteral(-curCount,false));
			fAssumptions.remove(new Litteral(-curCount,true));
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
