package ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.incremental;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATSolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.OldClause;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.Litteral;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.AbstractSATSolver;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.helpers.IncrementalClaspSAT.IHDoNothing;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.helpers.IncrementalClaspSAT.InterruptHandler;
import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.IncrementalClaspLibrary;
import ca.ubc.cs.beta.stationpacking.utils.Holder;

/**
 * The same solver should not be accessed by multiple threads at the same time!
 * 
 * @author gsauln
 *
 */
public class IncrementalClaspSATSolver extends AbstractSATSolver 
{
	// C++
	// create the parser for the problem format
	
	private static Logger log = LoggerFactory.getLogger(IncrementalClaspSATSolver.class);
	
	private IncrementalClaspLibrary fLib = null;
	private String fParameters;
	private int fMaxArgs;
	private volatile boolean fInterrupt;
	private final LockManager fLocks = new LockManager();
	private final Holder<Boolean> fContinueCallbackHolder = new Holder<Boolean>();
	private Long fSeed;
	private IncrementalRunner fIncrementalRunner;
	private Pointer fJNAFacade = null;
	private Pointer fJNAResult = null;
	private boolean fTerminated;
	private Compressor fCompressor;
	private final Holder<Boolean> fFirstCall = new Holder<Boolean>();
	private volatile boolean fSolveCalled = false;
	
	// callback functions to be used by clasp 
	private final IncrementalClaspLibrary.jnaIncRead fReadCallback = new IncrementalClaspLibrary.jnaIncRead()
	{
		@Override
		public Pointer read() {
			// the first run is launched before any call to solve, so it must be waiting on the problem.
			if (!fFirstCall.get())
			{
				fLocks.lock.lock();
				try
				{
					fLocks.resultObtained.setAndSignal(true);
					try {
						fLocks.resultRead.waitUntilEqual(true, new IHDoNothing());
					} catch (InterruptedException e) {
					// 	do nothing if we were interrupted, we should wait for the solve command to handle this.
					}
				}
				finally
				{
					fLocks.lock.unlock();
				}
				// result were read reset the flag
				fLocks.resultRead.setAndSignal(false);
			}
			else
			{
				fFirstCall.set(false);
			}

			// wait until we get a new problem
			try {
				fLocks.problemEncoding.waitUntilNotNull(new IHDoNothing());
			} catch (InterruptedException e) {
				// do nothing as the thread isn't solving anything
			}

			Pointer problem = fLocks.problemEncoding.getHolderValue();
		
			fLocks.problemEncoding.setAndSignal(null);
			// set the interrupt flag to false as we start a new computation
			fInterrupt = false;
			return problem;
		}
	};
	
	private final IncrementalClaspLibrary.jnaIncContinue fContinueCallback = new IncrementalClaspLibrary.jnaIncContinue() {
		@Override
		public boolean doContinue() {
			return fContinueCallbackHolder.get();
		}
	};
	

	
	public IncrementalClaspSATSolver(String libraryPath, String parameters, long seed)
	{
		init(libraryPath, parameters, 128, seed);
	}
	
	public IncrementalClaspSATSolver(String libraryPath, String parameters, int maxArgs, long seed)
	{
		init(libraryPath, parameters, maxArgs, seed);
	}

	private void init(String libraryPath, String parameters, int maxArgs, long seed)
	{
		// load the library
		fLib = (IncrementalClaspLibrary) Native.loadLibrary(libraryPath, IncrementalClaspLibrary.class);
		fJNAFacade = fLib.createFacade();
		fJNAResult = fLib.createResult();
		
		// set holders values
		fContinueCallbackHolder.set(true);
		fFirstCall.set(true);
		
		// initialize the variables
		fMaxArgs = maxArgs;
		fParameters = parameters;
		fSeed = seed;
		fInterrupt = false;
		fTerminated = false;
		fCompressor = new Compressor();
		
		// test code
		// set the info about parameters, throw an exception if seed is contained.
		if (parameters.contains("--seed"))
		{
			throw new IllegalArgumentException("The parameter string cannot contain a seed as it is set upon the first call to solve!");
		}
		
		// check if the configuration is valid.
		String params = fParameters+" --seed=1";
		Pointer config = fLib.createConfig(params, params.length(), fMaxArgs);
		try {
			int status = fLib.getConfigStatus(config);
			if (status == 2)
			{
				String configError = fLib.getConfigErrorMessage(config);
				String claspError = fLib.getConfigClaspErrorMessage(config);
				String error = configError + "\n" + claspError;
				throw new IllegalArgumentException(error);
			}
		}
		finally 
		{
			fLib.destroyConfig(config);
		}
		
		// create and launch the thread
		fIncrementalRunner = new IncrementalRunner(fJNAFacade, fJNAResult);
		(new Thread(fIncrementalRunner)).start();
	}
	
	@Override
	public SATSolverResult solve(CNF aCNF, double aCutoff, long aSeed) 
	{
		fLib.resetResult(fJNAResult);
		
		fInterrupt = false;
		
		long time1 = System.currentTimeMillis();
		if (fTerminated)
		{
			throw new IllegalStateException("Cannot call solve(...) once notifyShutdown() has been called!");
		}
		// seed handling
		if (aSeed != fSeed)
		{
			log.warn("Incremental SAT solving does not support multiple seed. Only the seed on initialization is used.");
		}
		
		Message message = fCompressor.compress(aCNF);
		Pointer encoding = message.encode();
		
		long compTime1 = System.currentTimeMillis();
		
		fLocks.problemEncoding.setAndSignal(encoding);
				
		// Launches a timer that will set the interrupt flag of the result object to true after aCutOff seconds.
		final Holder<Boolean> timeout = new Holder<Boolean>();
		timeout.set(false);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				fLib.interrupt(fJNAFacade);
				timeout.set(true);
			}
		}, (long)(aCutoff*1000));
		// listens for thread interruption every 0.1 seconds, if the thread is interrupted, interrupt the library and return gracefully
		// while returning null (free library memory, etc.)
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (fInterrupt)
				{
					fLib.interrupt(fJNAFacade);
				}
			}
		}, 0, 100);
		
		fSolveCalled = true;
		try
		{
			fLocks.resultObtained.waitUntilEqual(true, new InterruptHandler() 
			{
				@Override
				public void handle(InterruptedException e) throws InterruptedException {
					interrupt();
				}
			});
		} catch (InterruptedException e)
		{
			// interrupts are already handled inside but we are waiting for a smooth exit to make sure the state of the system is stable.
		}
		timer.cancel();
		fSolveCalled = false;
		
		long compTime2 = System.currentTimeMillis();
		System.err.println("Incremental Clasp running time:"+(compTime2-compTime1));
		
		SATResult satResult = null;
		HashSet<Litteral> assignment = new HashSet<Litteral>();
		int state = fLib.getResultState(fJNAResult);
		if (fInterrupt)
		{
			satResult = SATResult.INTERRUPTED;
			fInterrupt = false;
		}
		else if (timeout.get())
		{
			satResult = SATResult.TIMEOUT;
		}
		else 
		{
			if (state == 0)
			{
				satResult = SATResult.UNSAT;
			}
			else if (state == 1)
			{
				satResult = SATResult.SAT;
				assignment = parseAssignment(fLib.getResultAssignment(fJNAResult), aCNF.getVariables());
			}
			else 
			{
				satResult = SATResult.CRASHED;
				log.error("Clasp crashed!");
			}
		}

		long time2 = System.currentTimeMillis();

		message.clear();
		
		// set the result holder to false
		fLocks.resultObtained.setAndSignal(false);
		// we have read the signal so signal the read thread that it can continue
		fLocks.resultRead.setAndSignal(true);
		
		// should be null only if thread was interrupted.
		SATSolverResult answer = null;
		if (satResult != null)
		{
			answer = new SATSolverResult(satResult, (time2-time1)/1000.0, assignment);
		}

		return answer;
	}

	private HashSet<Litteral> parseAssignment(String assignment, Collection<Long> CNFVars) {
		HashSet<Litteral> set = new HashSet<Litteral>();
		StringTokenizer strtok = new StringTokenizer(assignment, ";");

		while (strtok.hasMoreTokens())
		{
			int intLit = Integer.valueOf(strtok.nextToken());
			int var = Math.abs(intLit);
			boolean sign = intLit > 0;
			long decompressValue = fCompressor.decompressVar(var);
			if (CNFVars.contains(decompressValue))
			{
				Litteral aLit = new Litteral(decompressValue, sign);
				set.add(aLit);
			}
		}

		return set;
	}

	@Override
	public void interrupt() throws UnsupportedOperationException {
		if (fSolveCalled) fInterrupt = true;
	}

	@Override
	public void notifyShutdown() {
		// send interrupts and stop all computations
		
		fContinueCallbackHolder.set(false);
		interrupt();
		
		// termniation message is [0] = -1;
		Pointer message= new Memory(1 * Native.getNativeSize(Integer.TYPE));
		message.setInt(0 * Native.getNativeSize(Integer.TYPE), -1);
		fLocks.problemEncoding.setAndSignal(message);
		
		try {
			fLocks.notifyShutdown.waitUntilEqual(true, new IHDoNothing());
		} catch (InterruptedException e) {
			// will never happen as the interrupt handler ignores interrupts
		}
		
		// check the boolean set at the end of the run() of the runnable object and wait on it.  If true proceed with termination. 
		if (fLib != null)
		{
			if (fJNAFacade != null)
			{
				fLib.destroyFacade(fJNAFacade);
			}
			if (fJNAResult != null)
			{
				fLib.destroyResult(fJNAResult);
			}
		}
		fTerminated = true;
	}

	
	/**
	 * Manages the locks required for the synchronisation of the incremental solver.
	 */
	protected class LockManager
	{
		public Lock lock = new ReentrantLock();
		
		public ConditionHolder<Pointer> problemEncoding = new ConditionHolder<Pointer>(lock.newCondition(), new Holder<Pointer>(null));
		public ConditionHolder<Boolean> resultRead = new ConditionHolder<Boolean>(lock.newCondition(), new Holder<Boolean>(false));
		public ConditionHolder<Boolean> resultObtained = new ConditionHolder<Boolean>(lock.newCondition(), new Holder<Boolean>(false));
		public ConditionHolder<Boolean> notifyShutdown = new ConditionHolder<Boolean>(lock.newCondition(), new Holder<Boolean>(false));
		
		protected class ConditionHolder<T>
		{
			private Condition condition;
			private Holder<T> holder;
			
			public ConditionHolder(Condition aCondition, Holder<T> aHolder)
			{
				condition = aCondition;
				holder = aHolder;
			}
			
			public void setAndSignal(T value)
			{
				lock.lock();
				try
				{
					holder.set(value);
					condition.signalAll();
				}
				finally
				{
					lock.unlock();
				}
			}
			
			/**
			 * 
			 * @param value must not be null.
			 * @param interruptHandler handles the interruptedExceptions.
			 */
			public void waitUntilEqual(T value, InterruptHandler interruptHandler) throws InterruptedException
			{
				if (value == null)
				{
					throw new IllegalArgumentException("The value given to waitUntilEqual must not be null!");
				}
				lock.lock();
				try
				{
					while (!value.equals(holder.get()))
					{
						try
						{
							condition.await();
						}
						catch (InterruptedException e)
						{
							interruptHandler.handle(e);
						}
					}
				}
				finally
				{
					lock.unlock();
				}
			}
			
			/**
			 * 
			 * @param value must not be null.
			 * @param interruptHandler
			 * @throws InterruptedException
			 */
			public void waitUntilNotEqual(T value, InterruptHandler interruptHandler) throws InterruptedException
			{
				if (value == null)
				{
					throw new IllegalArgumentException("The value given to waitUntilEqual must not be null!");
				}
				lock.lock();
				try
				{
					while (value.equals(holder.get()))
					{
						try
						{
							condition.await();
						}
						catch (InterruptedException e)
						{
							interruptHandler.handle(e);
						}
					}
				}
				finally
				{
					lock.unlock();
				}
			}
			
			public void waitUntilNotNull(InterruptHandler interruptHandler) throws InterruptedException
			{
				lock.lock();
				try
				{
					while (holder.get() == null)
					{
						try
						{
							condition.await();
						}
						catch (InterruptedException e)
						{
							interruptHandler.handle(e);
						}
					}
				}
				finally
				{
					lock.unlock();
				}
			}
			
			public T getHolderValue()
			{
				lock.lock();
				try
				{
					return holder.get();
				}
				finally
				{
					lock.unlock();
				}
			}
		}
	}
	
	protected class IncrementalRunner implements Runnable
	{
		private Pointer fFacade; // facade object to be used by the incremental solving
		private Pointer fResult; // result object to be used by the incremental solving
		
		public IncrementalRunner(Pointer facade, Pointer result)
		{
			fFacade = facade;
			fResult = result;
		}
		
		@Override
		public void run() {
			// set the notify shutdown to false as we are running
			fLocks.notifyShutdown.setAndSignal(false);
			
			// Create the configuration object
			// the construction of the config should always work as it as been checked in the constructor.
			int seed = (new Random(fSeed)).nextInt();
			String params = fParameters+" --seed="+seed;
			Pointer config = fLib.createConfig(params, params.length(), fMaxArgs);
			
			// create Problem object and give the callback function.
			Pointer problem = fLib.createIncProblem(fReadCallback);
			
			// Create incremental controller
			Pointer control = fLib.createIncControl(fContinueCallback, fJNAResult);
			
			// call solve incremental, the thread will stop here until the call is terminated by incremental control returning false.
			fLib.jnasolveIncremental(fFacade, problem, config, control, fResult);
			
			// destroy all objects created in this call
			fLib.destroyConfig(config);
			fLib.destroyProblem(problem);
			fLib.destroyIncControl(control);
			
			// set the notifyShutdown boolean to true, in order for it to be able to destroy everything if needed.
			fLocks.notifyShutdown.setAndSignal(true);
		}
		
	}
	
	protected class Message
	{
		private int fNumVars;
		private TreeSet<Litteral> fAssumptions;
		private ArrayDeque<Long> fNewControlVariables;
		private HashSet<OldClause> fClauses;
		Pointer fEncoding = null;
		
		public Message(int numVars, TreeSet<Litteral> assumptions, HashSet<OldClause> clauses, ArrayDeque<Long> newControlVariables)
		{
			fNumVars = numVars;
			fAssumptions = assumptions;
			fClauses = clauses;
			fNewControlVariables = newControlVariables;
		}
		
		public Pointer encode()
		{
			if (fEncoding == null)
			{
				createEncoding();
			}
			return fEncoding;
		}
		
		public void clear()
		{
			fEncoding = null;
		}
		
		/**
		 * Format
		 * [0]: size of array
 		 * [1]: total number of variables
		 * [2]: number of new clauses
		 * [3]: number of control literals of clauses that need to be solved // MUST BE INCREASING
		 * [4..(4+[2]-1)]: new control variables (1 per new clause)
		 * [(4+[2])..(4+[2]+[3]-1)]: control literals that are true // MUST BE SORTED, MUST ALL BE FALSE i.e. negated
		 * [(4+[2]+[3])..end]: new clauses separated by 0s.
		 * 
		 * or
		 * 
		 * [0]: -1 = terminate the solver.
		 * @return
		 */
		protected void createEncoding()
		{
			int size = getIntSize();
			Pointer message= new Memory(size * Native.getNativeSize(Integer.TYPE));
			message.setInt(0 * Native.getNativeSize(Integer.TYPE), size);
			message.setInt(1 * Native.getNativeSize(Integer.TYPE), fNumVars);
			message.setInt(2 * Native.getNativeSize(Integer.TYPE), fClauses.size());
			message.setInt(3 * Native.getNativeSize(Integer.TYPE), fAssumptions.size());

			int i = 4;
			for (long newControl : fNewControlVariables)
			{
				message.setInt(i * Native.getNativeSize(Integer.TYPE), (int) newControl);
				i++;
			}
			for (Litteral trueControl : fAssumptions)
			{
				message.setInt(i * Native.getNativeSize(Integer.TYPE), (trueControl.getSign()?1:-1) * (int) trueControl.getVariable());
				i++;
			}
			for (OldClause clause : fClauses)
			{
				for (Litteral lit : clause)
				{
					message.setInt(i * Native.getNativeSize(Integer.TYPE), (lit.getSign()?1:-1) * (int) lit.getVariable());
					i++;
				}
				message.setInt(i * Native.getNativeSize(Integer.TYPE), 0);
				i++;
			}
			fEncoding = message;
		}
		
		protected int getIntSize()	
		{
			int size = 4;//size of array
			size += fClauses.size();
			size += fAssumptions.size();
			for (OldClause clause : fClauses)
			{
				size += clause.size()+1; // +1 because the clause needs to be terminated by a 0.
			}
			return size;
		}
	}
	
	protected class Compressor
	{
		private final HashBiMap<Long,Long> fCompressionMap = HashBiMap.create();
		private long fCompressionMapMax = 1;
		private long fNextControlLiteral = -1;
		
		private final HashMap<OldClause, Long> fControlVariables = new HashMap<OldClause, Long>();
		private final TreeSet<Litteral> fActivatedCompressedControls = new TreeSet<Litteral>(new Comparator<Litteral>() {

			@Override
			public int compare(Litteral o1, Litteral o2) {
				return Long.compare(o1.getVariable(), o2.getVariable());
			}
		});
		
		public Message compress(CNF cnf)
		{
			// set all control variables to true (so that clauses are not analysed).
			fActivatedCompressedControls.clear();

			HashSet<OldClause> newClauses = new HashSet<OldClause>();
			ArrayDeque<Long> newControlVariables = new ArrayDeque<Long>();
			for (OldClause clause : cnf)
			{
				Long control = fControlVariables.get(clause);
				if (control == null)
				{
					// add the clause
					fControlVariables.put(clause, fNextControlLiteral);
					control = fNextControlLiteral;
					fNextControlLiteral--;
					// add the clause to new clauses
					OldClause compressedClause = compressClause(clause, control);
					newClauses.add(compressedClause);
					newControlVariables.add(compressVar(control));
				}
				fActivatedCompressedControls.add(new Litteral(compressVar(control), false));
				
			}
			Message message = new Message(fCompressionMap.size(), fActivatedCompressedControls, newClauses, newControlVariables);
			return message;
		}
		
		protected OldClause compressClause(OldClause clause, long control)
		{
			OldClause newClause = new OldClause();
			// add the compressed lits to the clause
			for (Litteral lit : clause)
			{
				Long compressedVar = compressVar(lit.getVariable());
				newClause.add(new Litteral(compressedVar, lit.getSign()));
			}
			// add the control variable 
			Long compressedVar = compressVar(control);
			Litteral controlLit = new Litteral(compressedVar, true);
			newClause.add(controlLit);
			return newClause;
		}
		
		/**
		 * Compresses the var to its given value or creates a new compression and returns it.
		 * @param var variable to compress.
		 * @return the compressed value of the variable.
		 */
		protected long compressVar(long var)
		{
			Long val = fCompressionMap.get(var);
			if (val == null)
			{
				val = fCompressionMapMax;
				fCompressionMap.put(var, val);
				fCompressionMapMax++;
			}
			return val;
		}
		
		/**
		 * Decompresses the given compressed var to its original value.
		 * @param cvar compressed var for which to obtain the decompressed value.
		 * @return the decompressed value of the compressed var.
		 */
		public long decompressVar(long cvar)
		{
			BiMap<Long,Long> aInverseCompressionMap = fCompressionMap.inverse();
			if(aInverseCompressionMap.containsKey(cvar))
			{
				return aInverseCompressionMap.get(cvar);
			}
			else
			{
				throw new IllegalArgumentException("Cannot uncompress variable "+cvar+", not in the compression map.");
			}
		}
	}
}
