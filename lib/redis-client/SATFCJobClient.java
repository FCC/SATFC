import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ca.ubc.cs.beta.aclib.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.listener.ServerListener;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder.ServerResponse;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolver;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolverInterrupter;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.SolvingJob;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.daemon.ThreadedSolverServerParameters;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.utils.RunnableUtils;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.gson.*;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;

// Poll the Job Caster server (Redis) and solve problems as they appear
//
// TODO: see run_feasibility_check().
/*
 * Made SATFCJobClient implement Runnable (it already had a run method) so that
 * both it and SATFC Solver Server can be executed at the same time.
 */
public class SATFCJobClient implements Runnable {
	
	public enum Answer { YES, NO, UNKNOWN, ERROR }
	
	/*
	 * Queues to submit SATFC jobs and get answers.
	 * @author afrechet
	 */
	private final BlockingQueue<SolvingJob> fSATFCJobQueue;
	private final BlockingQueue<ServerResponse> fSATFCAnswerQueue;
	
	////////////////////////////////////////////////
	// Simple class to hold hetrogenous data
	static class FeasibilityResult {
		int _new_station;
		Answer _answer;     // "yes", "no", "unknown", "error"
		String _message;    // any message that needs to be returned
		Double _time;       // how long did SATFC take
		Double _wall_clock; // how much wall clock time did SATFC actually take?
		Map<Integer, Integer> _witness_assignment;
		
		public FeasibilityResult(int new_station, Answer answer, String message,  Double time, Double wall_clock, Map<Integer, Integer> witness) {
			_new_station        = new_station;
			_answer             = answer;
			_time               = time;
			_message            = message;
			_wall_clock         = wall_clock;
			_witness_assignment = witness;
		}
		
		int get_new_station()   { return _new_station; }
		Answer get_answer()     { return _answer; }
		Double get_time()       { return _time; }
		String get_message()    { return _message; }
		Double get_wall_clock() { return _wall_clock; }
		Map<Integer, Integer> get_witness_assignment() { return _witness_assignment; }
	}

	
	/*
	 * Made SATFCJobClient implement runnable so that both it and SATFC's solver server
	 * can be executed in parallel with a thread pool.
	 * @author afrechet
	 */
	public SATFCJobClient(Options options, 
			BlockingQueue<SolvingJob> aSATFCJobQueue,
			BlockingQueue<ServerResponse> aSATFCAnswerQueue) {
		
		/*
		 * The only connection points this object needs from SATFC is the ServerSolver job queue to
		 * submit jobs to, and server response queue to get answers from. 
		 * @afrechet 
		 */
		fSATFCJobQueue = aSATFCJobQueue;
		fSATFCAnswerQueue = aSATFCAnswerQueue;
		

		//?? use options
		_caster = new JobCaster(options.redis_url);
		_statistics = new HashMap<String, Object>();
	}
	
	JobCaster _caster;
	Map<String, Object> _statistics;
	
	double _last_status_report_time;
	
	double now() {
		return System.currentTimeMillis() / 1000.0;
	}
	
	void sleep_for(long milliseconds) {
		try {
		  Thread.sleep(milliseconds);
		} catch(InterruptedException ex) {
		  Thread.currentThread().interrupt();
		}	
	}
	
	////////////////////////////
	// Parts of status reports
	String client_id() {
		return "java_framework_dude";
	}
	
	String status() {
		// Hard coded value grabbed from a JSONing of some ruby data in the right format.  Clients should be able to read this OK.
		// The real value should be a JSONed hash.  See the old #status in satfc_job_client_logic.rb
	    return "{\"id\":\"client_id\",\"report_time\":1387468010,\"location\":\"my_location\",\"start_time\":1387468010,\"satfc_port\":49149,\"statistics\":{},\"solver_status\":[\"status OK\",1387468010,17]}";		
	}
	
	String config_to_s() {
		return "DummyJavaClient";
	}
	
	void report(String msg) {
		System.out.println(config_to_s() + ": " + msg);
	}
	
	void report_status() {
		if (now() - _last_status_report_time < JobCaster.CLIENT_STATUS_REPORT_INTERVAL) {
			return;
		}
		
		report("Reporting status to server");
		_caster.report_status(client_id(), status());
		_last_status_report_time = now();
	} 
	
	// TODO: fill me in with magic of some sort
	String location() {
		return "127.0.0.1";
	}
	
	// TODO: remember the best way to do this in the face of static typing
	//	
	// private void increment_statistic(String stat, Number delta) {
	// 	if (!_statistics.containsKey(stat)) {
	// 		_statistics.set(stat, 0);
	// 	}
	// 	_statistics.set(stat, (Number)(_statistics.get(stat)) + delta);
	// }
	// 
	// void record_fc(FeasibilityResult result, double working_time) {
	// 	Answer answer = result.get_answer().to_s().toLowerCase();
	//     double satfc_time = result.get_time();
	//     
	// 	increment_statistic(answer, 1);
	// 	increment_statistic("satfc_time", satfc_time);
	// 	increment_statistic("working_time", working_time);
	//   }
	
	
	
	//////////////////////////
	// Solve a problem.
	//
	// For now we pause for a random time up to timeout, and then return NO.
	FeasibilityResult run_feasibility_check(ProblemSet problem_set, int new_station) {
		long sleep_time = (long)(Math.random() * problem_set.get_timeout_ms());
		sleep_for(sleep_time);
		
		return new FeasibilityResult(new_station, Answer.NO, "Immediately answered NO", sleep_time / 1000.0, sleep_time / 1000.0, null);
	}
	
	/**
	 * Converts a given problem to a SATFC format, submit it to SATFC then wait for an answer.
	 * @param problem_set - station packing problem.
	 * @param new_station - new station added to the problem.
	 * @return a feasibility result of packing new station in problem set.
	 * @author afrechet
	 */
	FeasibilityResult run_SATFC(ProblemSet problem_set, int new_station)
	{
		Watch watch = new Watch();
		watch.start();
		
		//Static problem parameters that are not provided.
		try {
		InetAddress dummyAddress = InetAddress.getLocalHost();
		int dummyPort = 111111;
		long seed = 1;
		
		/*
		 * Parse problem into solving job for solver server.
		 */
		double cutoff = problem_set.get_timeout_ms()/1000.0;
		String id = "SATFCJobClientID";
		
		/*
		 * TODO It seems constraint_set() is the name of the folder containing constraint interference
		 * and domain constraints. Make sure datafoldername is a valid path to the actual problem set, because SATFC
		 * may need to actually read the constraints. So either add a static path before get_constraint_set() or change
		 * the constraint_set's to actually be folder paths.
		 */
		String datafoldername = problem_set.get_constraint_set();
		
		/*
		 * TODO This should be an instance string as outlined in the SATFC readme:
		 * 
		 * "A formatted instance string is simply a O-O-separated list of channels, a O-O-separated list
		 * of stations and an optional previously valid partial channel assignment (in the form of a O-O-separated
		 * list of station-channel assignments joined by O,O), all joined by a O_O. For example, the feasibility 
		 * checking problem of packing stations 100,231 and 597 into channels 14,15,16,17 and 18 with previous 
		 * assignment 231 to 16 and 597 to 18 is represented by the following formatted instance string:
		 * 
		 * 14-15-16-17-18_100-231-597_231,16-597,18"
		 * 
		 * I am not sure the right information is provided through problem_set and new_station, so I am letting
		 * you guys figure out how to construct this string from the given objects.
		 */
		String instance = "REPLACE ME";
		
		SolvingJob solvingJob = new SolvingJob(id, datafoldername, instance, cutoff, seed, dummyAddress, dummyPort);
		
		
		/*
		 * Enqueue solving job.
		 */
		fSATFCJobQueue.put(solvingJob);
		
		/*
		 * Wait for answer from solver server.
		 */
		ServerResponse solverResponse = fSATFCAnswerQueue.take();
		
		double time = watch.stop();
		
		/*
		 * Parse answer from solver server.
		 */
		String answerMessage = solverResponse.getMessage();
		String[] answerMessageParts = answerMessage.split(ServerListener.COMMANDSEP);
		
		Answer answer = Answer.UNKNOWN;
		Map<Integer,Integer> witness = new HashMap<Integer,Integer>();
		if(answerMessageParts[0].equals("ERROR"))
		{
			answer = Answer.ERROR;	
		}
		else if(answerMessageParts[0].equals("ANSWER"))
		{
			String resultString = answerMessageParts[2];
			
			String[] resultParts = resultString.split(",");
			
			SATResult result = SATResult.valueOf(resultParts[0]);
			switch(result)
			{
				case SAT:
					answer = Answer.YES;
					break;
				case UNSAT:
					answer = Answer.NO;
					break;
				default:
					break;
			}
			
			double runtime = Double.valueOf(resultParts[1]);
			
			
			if(resultParts.length==3)
			{
				String assignmentString = resultParts[2];
				String[] assignmentStringParts = assignmentString.split(";");
				
				for(String channelAssignment : assignmentStringParts)
				{
					int channel = Integer.valueOf(channelAssignment.split("-")[0]);
					for(String stationString : channelAssignment.split("-")[1].split("_"))
					{
						int station = Integer.valueOf(stationString);
						
						witness.put(station, channel);
						
					}
				}
			}
		}
		
		/*
		 * Also piping in as message the full answerMessage (which might be pretty long), but may be useful for debugging purpose.
		 * Might want to remove it.
		 */
		FeasibilityResult result = new FeasibilityResult(new_station, answer, answerMessage, time, time, witness);
		
		return result;
		
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	// Poll the server for work, and do work when it appears.
	@Override
	public void run() {		
		for (;;) {
			report_status();
			
			String job = _caster.block_for_job();
			
			if (job != null) {
				report("Found job " + job);
				
				// We have a problem to work on
				String[] parts = job.split(":");
				String new_station = parts[0];
				String problem_set_id = parts[1];
				
				double start_time = now();
				
				_statistics.put("latest_problem", start_time);

				String problem_set_json = _caster.get_problem_set(problem_set_id);
				
				ProblemSet problem_set = new ProblemSet(problem_set_json);
				
				//!! This is the integration point with their software.
				FeasibilityResult result = run_feasibility_check(problem_set, Integer.parseInt(new_station));

				report("Result from checker was " + result.get_answer());
				
				Gson gson = new Gson();
				report("Json version of result is " + gson.toJson(result));

				//!! return the answer
				Map<String, Double> time_data = new HashMap<String, Double>();
				time_data.put("satfc_time", result.get_time());
				time_data.put("satfc_wall_clock", result.get_wall_clock());
				time_data.put("total_job_client_time", now() - start_time);
				
				Object[] answer_raw = new Object[] {
					location(),
					new_station,
					result.get_answer().toString().toLowerCase(),
					result.get_message(),
					time_data
				};
				
				String answer_json = gson.toJson(answer_raw);
				
				report("Answer to return is " + answer_json);
        
				_caster.send_assignment(problem_set_id, new_station, gson.toJson(result.get_witness_assignment()));
				_caster.send_answer(problem_set_id, answer_json);
 
				// 
				// record_fc answer, Time.now - start_time
			} else if (_caster.is_alive()) {
	      report("No work at the moment. Trying again.");
	    } else {
	      report("Experienced timeout, connection lost? Trying again.");
	    }
		}
	}
	
	
	/*
	 * Threadpool execution service for SATFC's solver server and SATFC job client.
	 * @author afrechet
	 */
	private final static AtomicInteger TERMINATION_STATUS = new AtomicInteger(0);
	private final static UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER;
	
	static
	{
		/*
		 * Statically define the uncaught exception handler.
		 */
	
		//Any uncaught exception should terminate current process.
		UNCAUGHT_EXCEPTION_HANDLER = new UncaughtExceptionHandler() 
		{
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				
				e.printStackTrace();
				
				System.err.println("Thread "+t.getName()+" died with an exception ("+e.getMessage()+").");
				
				System.err.println("Stopping service :( .");
				EXECUTOR_SERVICE.shutdownNow();
				
				TERMINATION_STATUS.set(1);
				
			}
		};
	}
	
	private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

	static class Options {
		// Look here <https://github.com/kohsuke/args4j/blob/master/rgs4j/examples/SampleMain.java>, here <http://www.whenbrainsfly.om/2009/05/args4j-is-magic/>, and here <http://args4j.kohsuke.org/> or examples.
		
		@Option(name="-r", aliases={"--redis_url"}, metaVar="url",
			usage="The url of the JobCaster (redis) server")
    	String redis_url;
    	
    	@Option(name="-h", aliases={"--help"},
    		usage="Show this message")
    	boolean should_show_help;
    	
    	static Options parse(String[] args) {
    		Options options = new Options();
    		CmdLineParser parser = new CmdLineParser(options);
    		try {
    			parser.parseArgument(args);
    			if (options.should_show_help) {
    				parser.printUsage(System.err);
    				System.exit(2);
    			}
    		} catch (CmdLineException e) {
				System.err.println(e.getMessage());
				parser.printUsage(System.err);
				System.exit(2);
			}
    		return options;
    	}
    }
	
	public static void main(String[] args) {
		
		Options options = Options.parse(args);
		
		/*
		 * Initialize SATFC's ServerSolver.
		 * This first requires option parameters to be set up, and the necessary options to be (manually)
		 * set. In this case it is only the path to the clasp library.
		 * 
		 * These options are usually read from args with jCommander; if this would be interesting, just ask me.
		 * 
		 * @afrechet
		 */
		ThreadedSolverServerParameters aParameters = new ThreadedSolverServerParameters();			
		//TODO: Put your library path here, or grab it from the args. (replace string)
		aParameters.SolverParameters.Library = "/Users/MightyByte/paxos-downloads/satfc/SATsolvers/clasp/jna/libjnaclasp.so";
				
		BlockingQueue<SolvingJob> aSolvingJobQueue = new LinkedBlockingQueue<SolvingJob>();
		BlockingQueue<ServerResponse> aServerResponseQueue = new LinkedBlockingQueue<ServerResponse>();
		
		SolverManager aSolverManager = aParameters.getSolverManager();
		ServerSolverInterrupter aSolverState = new ServerSolverInterrupter();
		ServerSolver aServerSolver = new ServerSolver(aSolverManager, aSolverState, aSolvingJobQueue, aServerResponseQueue);

		SATFCJobClient aSATFCJobClient = new SATFCJobClient(options,aSolvingJobQueue,aServerResponseQueue);
		
		/*
		 * Must run both SATFCJobClient and SATFC's ServerSolver.
		 * @author afrechet 
		 */
		RunnableUtils.submitRunnable(EXECUTOR_SERVICE, UNCAUGHT_EXCEPTION_HANDLER, aServerSolver);
		RunnableUtils.submitRunnable(EXECUTOR_SERVICE, UNCAUGHT_EXCEPTION_HANDLER, aSATFCJobClient);
		
		try {
			EXECUTOR_SERVICE.awaitTermination(365*10, TimeUnit.DAYS);
		} catch (InterruptedException e1) {
			System.err.println("We are really amazed that we're seeing this right now ("+e1+").");
			return;
		}
	
		System.exit(TERMINATION_STATUS.get());
	}
}




// Thin wrapper to unpack the json version and provide accessors
// Wow.  I do not miss Java at times like this. :(
class ProblemSet {
	
	int _band;
	int _highest;
	String _constraint_set;
	String _fc_config; // ignored
	String _fc_approach; // ignored
	int _timeout_ms;
	Map<Integer, Integer> _tentative_assignment;
	String _testing_flag;
	
	// Parse from the json encoding.
	public ProblemSet(String json_details) {
		JsonParser parser = new JsonParser();
		JsonObject problem_set_details = parser.parse(json_details).getAsJsonObject();
	
	  JsonArray data = problem_set_details.get("data").getAsJsonArray();
	
		// See JobCaster::ProblemSet for format.
		// We should perhaps simplify the encoding now that we have to unpack it by hand.
		_band = data.get(0).getAsInt();
		_highest = data.get(1).getAsInt();
		_constraint_set = data.get(2).getAsString();
		_fc_config = data.get(3).getAsString();
		_fc_approach = data.get(4).getAsString();
		_timeout_ms = data.get(5).getAsInt();

		// TODO: how to decode the tentative assignment, which is a map of integers (station ids) to integers (channels)?
		// Gson gson=new Gson(); 
		// String assignment_json = data.get(6).getAsString();
		// 
		// _tentative_assignment = new HashMap<Integer, Integer>();
		// _tentative_assignment = (Map<Integer, Integer>) gson.fromJson(assignment_json, _tentative_assignment.getClass());
		
		if (data.get(7).isJsonNull()) {
			_testing_flag = null;
		} else {
		  _testing_flag = data.get(7).getAsString();
		}
	}
	
	//////////////////////
	// Acceessors
	int get_band() {
		return _band;
	}
	
	int get_highest() {
		return _highest;
	}
	
	String get_constraint_set() {
		return _constraint_set;
	}
	
	String get_fc_config() {
		return _fc_config;
	}
	
	String get_fc_approach() {
		return _fc_approach;
	}
	
	int get_timeout_ms() {
		return _timeout_ms;
	}
	
	Map<Integer, Integer> get_tentative_assignment() {
		return _tentative_assignment;
	}
	
	String get_testing_flag() {
		return _testing_flag;
	}
	
	public String toString() {
		return "[band: " + _band +
		       ", highest: " + _highest +
		       ", constraint set: " + _constraint_set +
					 ", fc_config: " + _fc_config +
					 ", fc_approach: " + _fc_approach +
					 ", timeout_ms: " + _timeout_ms +
					 ", testing_flag: " + _testing_flag + "]";
	}
}


// Talk to the Redis server
class JobCaster {
	static final int CLIENT_STATUS_REPORT_INTERVAL = 5;
	static final int CLIENT_STATUS_REPORT_EXPIRATION = 5 * 60;
	
	static final String REDIS_SERVER_URL = "localhost";
		
	Jedis _jedis;
	
	JobCaster(String url) {
		//?? Use the url.
		_jedis = new Jedis(REDIS_SERVER_URL);
	}
	
	boolean is_alive() {
	  return _jedis.ping().equals("PONG");
	}
	
	void report_error(String error_msg) {
		_jedis.lpush(CLIENT_ERROR_KEY, error_msg);
	}
	
	void report_status(String client_id, String status) {
		Transaction tx = _jedis.multi();
		tx.sadd(CLIENT_IDS_SET, client_id);
		// Set the status and give it an expriation time.
		tx.set(client_status_key_for(client_id), status); //?? status.to_json
		tx.expire(client_status_key_for(client_id), CLIENT_STATUS_REPORT_EXPIRATION);
		tx.exec();
	}
	
	void send_answer(String problem_set_id, String answer_json) {
		_jedis.lpush(answer_list_key_for(problem_set_id), answer_json);
	}
	
	void send_assignment(String problem_set_id, String new_station_id, String witness_assignment_json) {
		_jedis.set(assignment_key_for(problem_set_id, new_station_id), witness_assignment_json);
	}
	
	// Wait for a job to appear in the appropriate list, returning it.  We return nil
	// if no such job appears by the timeout.
	String block_for_job() {
		List<String> result = _jedis.brpop(JOB_BLOCK_TIMEOUT, JOB_LIST_KEY);
		
		if (result == null) {
			return null;
		} else {
    		return result.get(1);
		}
	}

	String get_problem_set(String problem_set_id) {
		return _jedis.get(problem_set_key_for(problem_set_id));
	}

	
	static final int    JOB_BLOCK_TIMEOUT = 5; // seconds
	static final String JOB_LIST_KEY = "jobs";
	static final String CLIENT_IDS_SET = "client_ids";
	static final String CLIENT_STATUS_PREFIX = "client.status";
	static final String CLIENT_ERROR_KEY = "client_errors";
	static final String ANSWER_PREFIX = "answer";
	static final String PROBLEM_SET_PREFIX = "problem_set";
	
	String client_status_key_for(String client_id) {
		return CLIENT_STATUS_PREFIX+"."+client_id;
	}
	
	String answer_list_key_for(String problem_set_id) {
		return ANSWER_PREFIX+"."+problem_set_id+".answers";
	}
	
	String assignment_key_for(String problem_set_id, String station_id) {
		return ANSWER_PREFIX + "." + problem_set_id + "." + station_id;
	}
	
	String problem_set_key_for(String problem_set_id) {
		return PROBLEM_SET_PREFIX + "." + problem_set_id;
	}
}