package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental.queued;

import com.sun.jna.Pointer;

public class IncrementalProblem {

	private final double fCutoffTime;
	private final Pointer fProblemPointer;
	
	public IncrementalProblem(double cutoffTime, Pointer problemPointer)
	{
		fCutoffTime = cutoffTime;
		fProblemPointer = problemPointer;
	}
	
	public double getCutoffTime() {
		return fCutoffTime;
	}

	public Pointer getProblemPointer() {
		return fProblemPointer;
	}


//	private final CNF fCNF;
//	private final int fNumVars;
//	private final TreeSet<Literal> fAssumptions;
//	private final ArrayDeque<Long> fNewControlVariables;
//	private final HashSet<Clause> fClauses;
//	Pointer fEncoding = null;

//	public IncrementalProblem(CNF aCNF, int numVars, TreeSet<Literal> assumptions, HashSet<Clause> clauses, ArrayDeque<Long> newControlVariables)
//	{
//		fCNF = aCNF;
//		fNumVars = numVars;
//		fAssumptions = assumptions;
//		fClauses = clauses;
//		fNewControlVariables = newControlVariables;
//	}
	
//	public void clear()
//	{
//		fEncoding = null;
//	}
//	
//	/**
//	 * Format
//	 * [0]: size of array
//		 * [1]: total number of variables
//	 * [2]: number of new clauses
//	 * [3]: number of control literals of clauses that need to be solved // MUST BE INCREASING
//	 * [4..(4+[2]-1)]: new control variables (1 per new clause)
//	 * [(4+[2])..(4+[2]+[3]-1)]: control literals that are true // MUST BE SORTED, MUST ALL BE FALSE i.e. negated
//	 * [(4+[2]+[3])..end]: new clauses separated by 0s.
//	 * 
//	 * or
//	 * 
//	 * [0]: -1 = terminate the solver.
//	 * @return
//	 */
//	protected void createEncoding()
//	{
//		int size = getIntSize();
//		Pointer message= new Memory(size * Native.getNativeSize(Integer.TYPE));
//		message.setInt(0 * Native.getNativeSize(Integer.TYPE), size);
//		message.setInt(1 * Native.getNativeSize(Integer.TYPE), fNumVars);
//		message.setInt(2 * Native.getNativeSize(Integer.TYPE), fClauses.size());
//		message.setInt(3 * Native.getNativeSize(Integer.TYPE), fAssumptions.size());
//
//		int i = 4;
//		for (long newControl : fNewControlVariables)
//		{
//			message.setInt(i * Native.getNativeSize(Integer.TYPE), (int) newControl);
//			i++;
//		}
//		for (Literal trueControl : fAssumptions)
//		{
//			message.setInt(i * Native.getNativeSize(Integer.TYPE), (trueControl.getSign()?1:-1) * (int) trueControl.getVariable());
//			i++;
//		}
//		for (Clause clause : fClauses)
//		{
//			for (Literal lit : clause)
//			{
//				message.setInt(i * Native.getNativeSize(Integer.TYPE), (lit.getSign()?1:-1) * (int) lit.getVariable());
//				i++;
//			}
//			message.setInt(i * Native.getNativeSize(Integer.TYPE), 0);
//			i++;
//		}
//		fEncoding = message;
//	}
//	
//	protected int getIntSize()	
//	{
//		int size = 4;//size of array
//		size += fClauses.size();
//		size += fAssumptions.size();
//		for (Clause clause : fClauses)
//		{
//			size += clause.size()+1; // +1 because the clause needs to be terminated by a 0.
//		}
//		return size;
//	}
	
//	public Pointer getProblemPointer()
//	{
//		if (fEncoding == null)
//		{
//			createEncoding();
//		}
//		return fEncoding;
//	}
	
}
