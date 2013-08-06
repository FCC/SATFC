package ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATSolver;


import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;


/** This class provides an implementation of the IncrementalSATLibrary interface.
 * Its backbone is the GMSLibrary interface (provided below). For each function listed in 
 * this interface, there is a corresponding function (i.e. same signature) in the Solver.cc
 * file in the glueminisat-2.2.5/core/ folder. 
 * 
 * Author: narnosti, gsauln
 */

public class GlueMiniSatSolver implements IIncrementalSATSolver{
	
	
	/* The path to the library to be loaded.
	 */
	String fLibraryPath;
	
	/* A set of function calls provided in Solver.cc (interface listed below)
	 */
	GMSLibrary fGMSsolver;
	
	/* A call to createSolver() returns a pointer to a location in memory where the
	 * GlueMiniSat Solver object is stored. Any time we want to use any methods in the
	 * GlueMiniSat library, we must pass this pointer as an argument.
	 * 
	 * To be careful, we should provide some sort of "shut down library" call
	 * so that this pointer can be freed, but there is only one pointer, and 
	 * such a call would be made only if the program was about to exit.
	 */
	Pointer fSolverPointer;
	
	
	/* GlueMiniSAT seems intent on using [0 ... n-1] as its variable names, so rather than
	 * creating a number of variables equal to the maximum value of any external variable,
	 * we have a map from external variables to the corresponding variables in GlueMiniSAT
	 * (and the corresponding inverse). These grow any time clauses with previously unseen
	 * variables are added.
	 */
	private Map<Integer,Integer> fExternalToInternal = new HashMap<Integer,Integer>();
	private Map<Integer,Integer> fInternalToExternal = new HashMap<Integer,Integer>();

	
	private interface GMSLibrary extends Library {

			//Provides a pointer to a glueminisat Solver object
			public Pointer createSolver();
			
			//Frees the memory used for the Solver pointer
			public void destroySolver(Pointer solver);	
			
		   	//Creates a pointer to a vector of literals
		  	public Pointer createVecLit();
		  	
		  	//Frees the memory used for the vector of literals
		  	public void destroyVecLit(Pointer vecLiterals);
		  	
		  	//Adds the literal (num, state) to the vector pointed to by vec
		  	public void addLitToVec(Pointer vec, int num, boolean state);
		  	
			//Creates a new variable, returning the value of the new variable
		   	public int newVar(Pointer solver);

			//Returns the number of variables used by the Solver
		   	public int nVars(Pointer solver);

		  	//Adds the clause associated with the vector of literals vecLiterals
		  	public boolean addClause(Pointer Solver, Pointer vecLiterals);
		   	
		  	//Returns true if the current problem is satisfiable given the assumptions in vecAssumptions
		  	public boolean solveWithAssumptions(Pointer solver, Pointer vecAssumptions);
		  	
		  	//Returns true if the current problem is satisfiable.
		   	public boolean solve(Pointer solver);
		   	
		   	/* Returns the value of the variable var in the last model.
		   	 * Meaningless output if the last call to solve() returned false.
		   	 */
		   	public boolean value(Pointer solver, int var);
		   	
		   	// Returns true if the solver is not in a conflicting state
		   	public boolean okay(Pointer solver);

		    // Other function that exists in Solver.cc but is not needed.
		   	public boolean solveWithOneAssumption(Pointer solver, int var, boolean state);
		   	
		   	/**
		   	 * Set the interrupt flag to true in the GlueMiniSAT solver.  It will then exit at the next check.
		   	 * @param solver solver to set the flag for.
		   	 */
		   	public void interrupt(Pointer solver);
		   	
		   	/**
		   	 * Clears the interrupt flag (set to false) of the GlueMiniSAT solver.
		   	 * @param solver solver to clear the interrupt flag for.
		   	 */
		   	public void clearInterrupt(Pointer solver);
		   	
		   	/**
		   	 * Return the interrupt flag state.
		   	 * @param solver solver to return the interrupt flag state for.
		   	 */
		   	public boolean getInterruptState(Pointer solver);
		}
	
    	public GlueMiniSatSolver(String aLibraryPath){
    		try
    		{
	    		fLibraryPath = aLibraryPath;
	    		fGMSsolver = (GMSLibrary) Native.loadLibrary(fLibraryPath, GMSLibrary.class);
	    		fSolverPointer = fGMSsolver.createSolver();
	    		runBasicTests(); //Prints the output of basic tests to stdout; used for debugging
    		}
    		catch(Exception e)
    		{
    			throw new IllegalArgumentException("Could not create a glueminisat library from "+aLibraryPath+" ("+e.getMessage()+").");
    		}
    	}
		 
		public SATResult solve(Clause aAssumptions, double aCutOff){
			
			//Create a pointer to a vector of assumptions corresponding to aAssumptions
			Pointer vecAssumptions = fGMSsolver.createVecLit();
			for(Integer aVar : aAssumptions.getVars()){
				fGMSsolver.addLitToVec(vecAssumptions,getInternalVariable(aVar),true);
			}
			for(Integer aVar : aAssumptions.getNegatedVars()){
				fGMSsolver.addLitToVec(vecAssumptions,getInternalVariable(aVar),false);
			}
			
			// Clears the interrupt state to make sure it wont stop at the beginning of the solve command
			fGMSsolver.clearInterrupt(fSolverPointer);

			// Launches a timer that will set the interrupt flag of the solve to true after aCutOff seconds.
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					fGMSsolver.interrupt(fSolverPointer);
				}
			}, (long)(aCutOff*1000));
			
			// Start solving
			boolean output = fGMSsolver.solveWithAssumptions(fSolverPointer,vecAssumptions);
			timer.cancel();

			// Set the correct SAT result
			SATResult aResult;
			if (fGMSsolver.getInterruptState(fSolverPointer))
			{
				aResult = SATResult.TIMEOUT;
			}
			else
			{
				if(output){
					aResult = SATResult.SAT;
				} else {
					aResult =  SATResult.UNSAT;
				}
			}
			
			fGMSsolver.destroyVecLit(vecAssumptions);
			return aResult;
		}
		
		public SATResult solve(double aCutOff){
			return solve(new Clause(), aCutOff);
		}
		
		public boolean addClause(Clause aClause){
			
			//Create a pointer to a vector of Literals corresponding to aClause
			Pointer vecLiterals = fGMSsolver.createVecLit();			
			for(Integer aVar : aClause.getVars()){
				fGMSsolver.addLitToVec(vecLiterals,getInternalVariable(aVar),true);
			}
			for(Integer aVar : aClause.getNegatedVars()){
				fGMSsolver.addLitToVec(vecLiterals,getInternalVariable(aVar),false);
			}
			
			boolean added = fGMSsolver.addClause(fSolverPointer,vecLiterals);
			fGMSsolver.destroyVecLit(vecLiterals);			
			return added;
		}
		
		public Clause getAssignment(){
			Clause aAssignment = new Clause();
			if(fGMSsolver.okay(fSolverPointer)){
				for(int i = 0; i < fGMSsolver.nVars(fSolverPointer); i++){
					/* For some reason, when passing booleans from c to java there were problems.
					 * By returning an int and then testing (b>0), these problems were resolved.
					 */	
					//int b = fGMSsolver.value(fSolverPointer,i);
					//aAssignment.addLiteral(fInternalToExternal.get(i),(b>0));
					boolean b = fGMSsolver.value(fSolverPointer,i);
					aAssignment.addLiteral(fInternalToExternal.get(i),b);
				}
			}
			return aAssignment;
		}
		
		public void clear(){
			fGMSsolver.destroySolver(fSolverPointer);
			fExternalToInternal.clear();
			fInternalToExternal.clear();
			fGMSsolver = (GMSLibrary) Native.loadLibrary(fLibraryPath, GMSLibrary.class);
			fSolverPointer = fGMSsolver.createSolver();
		}
		
		/* Used to map external variables (passed to this class)
		 * to internal variables (used by the GMSLibrary solver)
		 * If given a previously unseen variable, it creates a new one.
		 */
		private Integer getInternalVariable(Integer aVar){
			Integer aInternalVar = fExternalToInternal.get(aVar);
			if(aInternalVar == null){
				aInternalVar = fGMSsolver.newVar(fSolverPointer);
				fExternalToInternal.put(aVar,aInternalVar);
				fInternalToExternal.put(aInternalVar,aVar);
			}
			return aInternalVar;
		}
		
		/* 
		 * getMap(), printResult() and runBasicTests() were used for debugging.
		 */
		
		/* 
		public Map<Integer,Integer> getMap(){
			return fExternalToInternal;
		}
		*/
		
		private void printResult(Clause aAssumptions, double aCutOff){
	        if(solve(aAssumptions, aCutOff)!=SATResult.SAT){
	            System.out.println("Solver result is UNSAT.");
	        } else{
	        	System.out.println("Solver result is SAT, with assignment: "+getAssignment());
	        }
		}

		private void runBasicTests(){
			Clause aClause;
	        
	        System.out.println("Testing trivial SAT instance...");
	        aClause = new Clause();
	        aClause.addLiteral(1,false);
	        aClause.addLiteral(2, true);
	        addClause(aClause);
	        aClause = new Clause();
	        aClause.addLiteral(2,false);
	        addClause(aClause);
	        aClause = new Clause();
	        printResult(aClause, 1); 
	        
	        System.out.println("Testing trivial UNSAT instance...");
	        aClause.addLiteral(1,true);
	        printResult(aClause, 1);
	        	        
	        System.out.println("Testing trivial UNSAT instance...");
	        addClause(aClause);
	        printResult(new Clause(), 1);
	        clear();
	        
			System.out.println("Testing slightly more complicated SAT instance...");
	        aClause = new Clause();
	        aClause.addLiteral(1,true);
	        aClause.addLiteral(2,true);
	        aClause.addLiteral(3,true);
	        aClause.addLiteral(4,false);
	        addClause(aClause);
	        aClause = new Clause();
	        aClause.addLiteral(1,false);
	        aClause.addLiteral(2,false);
	        aClause.addLiteral(4,false);
	        addClause(aClause);
	        aClause = new Clause();
	        aClause.addLiteral(1,false);
	        aClause.addLiteral(3,false);
	        aClause.addLiteral(4,false);
	        addClause(aClause);
	        aClause = new Clause();
	        aClause.addLiteral(2,false);
	        aClause.addLiteral(3,false);
	        aClause.addLiteral(4,false);
	        addClause(aClause);
	        aClause = new Clause();
	        aClause.addLiteral(2,false);
	        addClause(aClause);
	        printResult(new Clause(), 1);
	        
	        System.out.println("Now variable 4 should be true.");
	        aClause = new Clause();
	        aClause.addLiteral(4,true);
	        printResult(aClause, 1);
	        
	        System.out.println("Now the problem should be UNSAT...");
	        aClause.addLiteral(2,true);
	        printResult(aClause, 1);
	        
	        System.out.println("Now the problem should really, really be UNSAT.");
	        aClause = new Clause();
	        aClause.addLiteral(2,true);
	        addClause(aClause);
	        printResult(new Clause(), 1);
	        
	        clear();	//Important to clear; otherwise the clauses inserted above remain in the problem.
		}
		
}
