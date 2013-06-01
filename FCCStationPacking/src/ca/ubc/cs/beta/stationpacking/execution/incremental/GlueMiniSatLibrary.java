package ca.ubc.cs.beta.stationpacking.execution.incremental;

import java.util.Set;

import ca.ubc.cs.beta.stationpacking.experiment.solver.result.SATResult;

import com.sun.jna.Library;
import com.sun.jna.Native;


public class GlueMiniSatLibrary implements IncrementalSATLibrary{
	
	GMSLibrary GMSsolver = (GMSLibrary) Native.loadLibrary("/Users/narnosti/Documents/fcc-station-packing/glueminisat-2.2.5/core/libglueminisat_standard.so", GMSLibrary.class);

	
	private interface GMSLibrary extends Library {
		   /*
	        public int chmod(String filename, int mode);
	        public int chown(String filename, int user, int group);
	        public int rename(String oldpath, String newpath);
	        public int kill(int pid, int signal);
	        public int link(String oldpath, String newpath);
	        public int mkdir(String path, int mode);
	        public int rmdir(String path);
	        */
		   //int add(int x, int y);
		   public boolean addEmptyClause();
		   public boolean simplify();
		   public boolean solve();
		   public boolean okay();
		}
		 
		public SATResult solve(){
			try{
				throw new Exception("Method solve not implemented");
			} catch(Exception e) {
				e.printStackTrace();
			}
			return SATResult.CRASHED;
		}
		
		public boolean addClause(Set<Integer> aVars,Set<Integer> aNegatedVars){
			try{
				throw new Exception("Method addClause not implemented");
			} catch(Exception e) {
				e.printStackTrace();
			}
			return false;
		}

}
