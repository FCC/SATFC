package ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries;


import java.util.HashMap;
import java.util.Map;

import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;


public class GlueMiniSatLibrary implements IIncrementalSATLibrary{
	
	
	String fLibraryPath;
	GMSLibrary fGMSsolver;
	Pointer fSolverPointer; //need to provide a way to free this memory (a shutdown method)
	private Map<Integer,Integer> fExternalToInternal = new HashMap<Integer,Integer>();
	private Map<Integer,Integer> fInternalToExternal = new HashMap<Integer,Integer>();

	
    public GlueMiniSatLibrary(String aLibraryPath){
        fLibraryPath = aLibraryPath;
        fGMSsolver = (GMSLibrary) Native.loadLibrary(fLibraryPath, GMSLibrary.class);
		fSolverPointer = fGMSsolver.createSolver();
		
		Clause aClause;
		/*
		System.out.println("Testing trivially UNSAT instance...");
        aClause = new Clause();
        aClause.addLiteral(1,false);
        addClause(aClause);
        aClause = new Clause();
        aClause.addLiteral(1,true);
        addClause(aClause);
        printResult(new Clause());
        clear();
        */
        
        /*
		System.out.println("Testing trivially SAT instance...");
        aClause = new Clause();
        aClause.addLiteral(1,true);
        addClause(aClause);
        aClause = new Clause();
        aClause.addLiteral(1,false);
        aClause.addLiteral(2,true);
        addClause(aClause);
        printResult(new Clause());
       
        System.out.println("Now it should be UNSAT...");
        aClause = new Clause();
        aClause.addLiteral(2,false);
        printResult(aClause);
        clear();
        */
		
		//fGMSsolver.printNick(fSolverPointer);
        
        System.out.println("Should give SAT.");
        aClause = new Clause();
        aClause.addLiteral(1,false);
        aClause.addLiteral(2, true);
        addClause(aClause);
        aClause = new Clause();
        aClause.addLiteral(2,false);
        addClause(aClause);
        aClause = new Clause();
        printResult(aClause); 
        
        System.out.println("Should give UNSAT.");
        aClause.addLiteral(1,true);
        printResult(aClause);
        
        //System.out.println("Should give UNSAT.");
        //System.out.println("Got "+fGMSsolver.solveWithOneAssumption(fSolverPointer, 0, true));
        
        System.out.println("Should give UNSAT.");
        addClause(aClause);
        printResult(new Clause());
        
        /*
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
        printResult(new Clause());
        
        System.out.println("Now variable 4 should be true.");
        aClause = new Clause();
        aClause.addLiteral(4,true);
        printResult(aClause);
        
        System.out.println("Now it should be UNSAT.");
        aClause.addLiteral(2,false);
        printResult(aClause);
        
        System.out.println("Now it should really, really be UNSAT.");
        aClause = new Clause();
        aClause.addLiteral(2,true);
        addClause(aClause);
        printResult(new Clause());
        clear();
        */
		/*
        for(int i = 0; i < 10; i++){
        	System.out.println("Created variable "+fGMSsolver.newVar(fSolverPointer));
        }
        System.out.println("Number of variables is: "+fGMSsolver.nVars(fSolverPointer));
        Clause aClause = new Clause();
        aClause.addLiteral(12,false);
        aClause.addLiteral(13, false);
        addClause(aClause);
        System.out.println("Solver result is : "+ fGMSsolver.solve(fSolverPointer));
        System.out.println("WOOHOO!!!");
        */
    }
	
	private interface GMSLibrary extends Library {
		 	//public boolean addEmptyClause(Pointer solver);
		   	//public boolean simplify(Pointer solver);
		   	//public boolean okay(Pointer solver);
		   
		
			public void printLit(Pointer solver, Integer variable, Boolean value);
		   
			public Pointer createSolver();
		   
			public void destroySolver(Pointer solver);

		  	public Pointer createVecLit();
		  	public void destroyVecLit(Pointer vecAssumptions);
		  	public void addLitToVec(Pointer vec, int num, boolean state);
		   
		  	public boolean addClause(Pointer Solver, Pointer vecAssumptions);
		   	public boolean solveWithAssumptions(Pointer solver, Pointer vecAssumptions);
		   	
		   	public int nVars(Pointer solver);
		   	public int newVar(Pointer solver);
		   	//public Integer testing(Pointer solver, Integer i);
		   	public boolean solve(Pointer solver);
		   	public int value(Pointer solver, int var);
		   	public boolean okay(Pointer solver);
		   	public boolean solveWithOneAssumption(Pointer solver, int var, boolean state);
		   	public boolean litValue(Pointer solver, int var, boolean state);
		   	//public void printNick(Pointer solver);
		}
		 
		public SATResult solve(Clause aAssumptions){
			//System.out.println("\n Assumptions are "+aAssumptions+"\n");
			Pointer vecAssumptions = fGMSsolver.createVecLit();
			for(Integer aVar : aAssumptions.getVars()){
				fGMSsolver.addLitToVec(vecAssumptions,getInternalVariable(aVar),true);
			}
			for(Integer aVar : aAssumptions.getNegatedVars()){
				fGMSsolver.addLitToVec(vecAssumptions,getInternalVariable(aVar),false);
			}
			SATResult aResult;
			if(fGMSsolver.solveWithAssumptions(fSolverPointer,vecAssumptions)){
				aResult = SATResult.SAT;
			} else {
				aResult =  SATResult.UNSAT;
			}
						
			fGMSsolver.destroyVecLit(vecAssumptions);
			return aResult;
		}
		
		public SATResult solve(){
			return solve(new Clause());
		}
		
		public boolean addClause(Clause aClause){
			
			//System.out.print("Clause to add is: <");

			Pointer vecAssumptions = fGMSsolver.createVecLit();			
			for(Integer aVar : aClause.getVars()){
				fGMSsolver.addLitToVec(vecAssumptions,getInternalVariable(aVar),true);
				//System.out.print(getInternalVariable(aVar)+"("+aVar+"),");
			}
			//System.out.print("_");
			for(Integer aVar : aClause.getNegatedVars()){
				fGMSsolver.addLitToVec(vecAssumptions,getInternalVariable(aVar),false);
				//System.out.print(getInternalVariable(aVar)+"("+aVar+"),");
			}
			//System.out.println(">");
			boolean added = fGMSsolver.addClause(fSolverPointer,vecAssumptions);
			fGMSsolver.destroyVecLit(vecAssumptions);
			
			return added;
		}
		
		public void clear(){
			fGMSsolver.destroySolver(fSolverPointer);
			fExternalToInternal.clear();
			fInternalToExternal.clear();
			fGMSsolver = (GMSLibrary) Native.loadLibrary(fLibraryPath, GMSLibrary.class);
			fSolverPointer = fGMSsolver.createSolver();
		}
		
		private Integer getInternalVariable(Integer aVar){
			Integer aInternalVar = fExternalToInternal.get(aVar);
			if(aInternalVar == null){
				aInternalVar = fGMSsolver.newVar(fSolverPointer);
				fExternalToInternal.put(aVar,aInternalVar);
				fInternalToExternal.put(aInternalVar,aVar);
			}
			return aInternalVar;
		}

		//Insert some checks to see that the last state is okay
		public Clause getAssignment(){
			//fGMSsolver.solve(fSolverPointer);
			Clause aAssignment = new Clause();
			for(int i = 0; i < fGMSsolver.nVars(fSolverPointer); i++){
				int b = fGMSsolver.value(fSolverPointer,i);
				//if(b>0) System.out.println("In JAVA we got: var "+i+" is "+b);
				//if(b) System.out.println("REVERSE");
				//b = (fGMSsolver.litValue(fSolverPointer,i,false)!=fGMSsolver.value(fSolverPointer,i));
				//if(b) System.out.println("I'm tired");
				aAssignment.addLiteral(fInternalToExternal.get(i),(b>0));

				//System.out.println(fInternalToExternal.get(i)+","+fGMSsolver.value(fSolverPointer,i));
				//System.out.println(fInternalToExternal.get(i)+","+fGMSsolver.litValue(fSolverPointer,i,false));

				//if(fGMSsolver.litValue(fSolverPointer,fInternalToExternal.get(i),fGMSsolver.value(fSolverPointer,i))) System.out.println("\n\n\nWTF???\n\n\n");
			}
			return aAssignment;
		}
		
		private void printResult(Clause aAssumptions){
	        if(solve(aAssumptions)!=SATResult.SAT){
	            System.out.println("Solver result is UNSAT.");
	        } else{
	        	if(fGMSsolver.okay(fSolverPointer))
	        		System.out.println("Solver result is SAT, with assignment: "+getAssignment());
	        }
		}
		
		
		public Map<Integer,Integer> getMap(){
			return fExternalToInternal;
		}
}
