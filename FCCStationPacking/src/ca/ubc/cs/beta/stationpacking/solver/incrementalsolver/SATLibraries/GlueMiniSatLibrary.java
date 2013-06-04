package ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries;

import ca.ubc.cs.beta.stationpacking.datastructures.Clause;
import ca.ubc.cs.beta.stationpacking.datastructures.SATResult;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;


public class GlueMiniSatLibrary implements IIncrementalSATLibrary{
	
	
	String fLibraryPath;
	GMSLibrary fGMSsolver;

    public GlueMiniSatLibrary(String aLibraryPath){
        fLibraryPath = aLibraryPath;
        fGMSsolver = (GMSLibrary) Native.loadLibrary(fLibraryPath, GMSLibrary.class);
    }
	
	private interface GMSLibrary extends Library {
		   //public boolean addEmptyClause(Pointer solver);
		   //public boolean simplify(Pointer solver);
		   //public boolean okay(Pointer solver);
		   //public void printLit(Pointer solver, Integer variable, Boolean value);
		   public Pointer createSolver();
		   public void destroySolver();
		   

		   public Pointer createVecLit();
		   public void destroyVecLit();
		   public void addLitToVec(Pointer vec, int num, boolean state);
		   
		   public boolean addClause(Pointer Solver, Pointer vecAssumptions);
		   public boolean solve(Pointer solver, Pointer vecAssumptions);
		   //public Integer testing(Pointer solver, Integer i);
		   //public boolean solve(Pointer solver);

		}
		 
		public SATResult solve(Clause aAssumptions){
			Pointer solver = fGMSsolver.createSolver();
			Pointer vecAssumptions = fGMSsolver.createVecLit();
			for(Integer aVar : aAssumptions.getVars()){
				fGMSsolver.addLitToVec(vecAssumptions,aVar,true);
			}
			for(Integer aVar : aAssumptions.getNegatedVars()){
				fGMSsolver.addLitToVec(vecAssumptions,aVar,false);
			}
			SATResult aResult;
			if(fGMSsolver.solve(solver,vecAssumptions)){
				aResult = SATResult.SAT;
			} else {
				aResult =  SATResult.UNSAT;
			}
			fGMSsolver.destroyVecLit();
			fGMSsolver.destroySolver();
			return aResult;
			//Pointer lits = GMSsolver.createVecLit();
			//GMSsolver.addLitToVec(lits, 3, true);
			//GMSsolve.solve(solver, lits);
		}
		
		public SATResult solve(){
			return solve(new Clause());
		}
		
		public boolean addClause(Clause aClause){
			Pointer solver = fGMSsolver.createSolver();
			Pointer vecAssumptions = fGMSsolver.createVecLit();
			for(Integer aVar : aClause.getVars()){
				fGMSsolver.addLitToVec(vecAssumptions,aVar,true);
			}
			for(Integer aVar : aClause.getNegatedVars()){
				fGMSsolver.addLitToVec(vecAssumptions,aVar,false);
			}
			boolean added = fGMSsolver.addClause(solver,vecAssumptions);
			fGMSsolver.destroyVecLit();
			fGMSsolver.destroySolver();
			return added;
		}
		
		public void clear(){
			fGMSsolver = (GMSLibrary) Native.loadLibrary(fLibraryPath, GMSLibrary.class);
		}

}
