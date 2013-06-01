package ca.ubc.cs.beta.stationpacking.execution.incremental.SATLibraries;

import java.util.HashSet;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;


public class GlueMiniSatLibrary implements IIncrementalSATLibrary{
	
	GMSLibrary GMSsolver = (GMSLibrary) Native.loadLibrary("/Users/narnosti/Documents/fcc-station-packing/glueminisat-2.2.5/core/libglueminisat.so", GMSLibrary.class);

	
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
		 
		public SATResult solve(Set<Integer> aTrueVars,Set<Integer> aFalseVars){
			Pointer solver = GMSsolver.createSolver();
			Pointer vecAssumptions = GMSsolver.createVecLit();
			for(Integer aVar : aTrueVars){
				GMSsolver.addLitToVec(vecAssumptions,aVar,true);
			}
			for(Integer aVar : aFalseVars){
				GMSsolver.addLitToVec(vecAssumptions,aVar,false);
			}
			SATResult aResult;
			if(GMSsolver.solve(solver,vecAssumptions)){
				aResult = SATResult.SAT;
			} else {
				aResult =  SATResult.UNSAT;
			}
			GMSsolver.destroyVecLit();
			GMSsolver.destroySolver();
			return aResult;
			//Pointer lits = GMSsolver.createVecLit();
			//GMSsolver.addLitToVec(lits, 3, true);
			//GMSsolve.solve(solver, lits);
		}
		
		public SATResult solve(){
			return solve(new HashSet<Integer>(),new HashSet<Integer>());
		}
		
		public boolean addClause(Set<Integer> aTrueVars,Set<Integer> aFalseVars){
			Pointer solver = GMSsolver.createSolver();
			Pointer vecAssumptions = GMSsolver.createVecLit();
			for(Integer aVar : aTrueVars){
				GMSsolver.addLitToVec(vecAssumptions,aVar,true);
			}
			for(Integer aVar : aFalseVars){
				GMSsolver.addLitToVec(vecAssumptions,aVar,false);
			}
			boolean added = GMSsolver.addClause(solver,vecAssumptions);
			GMSsolver.destroyVecLit();
			GMSsolver.destroySolver();
			return added;
		}

}
