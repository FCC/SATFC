import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.listener.ServerListener;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.responder.ServerResponse;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolver;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolver.ProblemInitializingOverheadTimeoutException;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolver.SolverServerStateInterruptedException;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolver.SolvingInterrupedException;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.ServerSolverInterrupter;
import ca.ubc.cs.beta.stationpacking.daemon.server.threadedserver.solver.SolvingJob;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.daemon.ThreadedSolverServerParameters;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.utils.RunnableUtils;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.jna.Platform;

/*
 * Poll the Job Caster server (Redis) and solve problems as they appear
 * 
 * Made SATFCJobClient implement Runnable (it already had a run method) so that
 * both it and SATFC Solver Server can be executed at the same time.
 */
public class SATFCJobClient implements Runnable {
	
	public enum Answer { YES, NO, UNKNOWN, ERROR }
	
	/*
	 * A SolverServer for doing the hard work of unpacking and solving problems.
	 * @author wtaysom
	 */
	private final ServerSolver fServerSolver;
	
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
	public SATFCJobClient(Options options, ServerSolver aServerSolver) {
		
		/*
		 * We call ServerSolver.solve directly in order to avoid the complexity of threads and job queues.
		 * @wtaysom 
		 */
		fServerSolver = aServerSolver;
		
		_constraint_sets_directory = options.constraint_sets_directory;
		report("Using constraints in "+_constraint_sets_directory+".");
		_statistics = new HashMap<String, Object>();
		
		_redis_url = options.redis_url;
		for (;;) {
			try {
				reconnect();
				_client_id = _caster.get_new_client_id();
				break;
			} catch (JedisConnectionException e) {
				// Try again.
			}
		}
	}
	
	static final long RETRY_DELAY = 3000;
	
	void reconnect() {
		_caster = null;
		for (;;) {
			try {
				_caster = new JobCaster(_redis_url);
				_caster.is_alive(); // throws JedisConnectionException if there's a connection problem.
				break;
			} catch (JedisConnectionException e) {
				report("Cannot contact Redis at "+_redis_url+".  Retry in "+RETRY_DELAY+"ms.  "+e.getMessage());
				
				try {
					Thread.sleep(RETRY_DELAY);
				} catch (InterruptedException e1) {
					// Go ahead and continue.
				}
			}
		}		
	}
	
	Gson _gson = new Gson();
	String _constraint_sets_directory;
	String _redis_url;
	JobCaster _caster;
	String _client_id;
	long _created_at = (long)now();
	Map<String, Object> _statistics;
	
	double _last_status_report_time;
	
	double now() {
		return System.currentTimeMillis() / 1000;
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
	
	String get_ip() {
		try {
			Process process = Runtime.getRuntime().exec("curl -s http://ipecho.net/plain");
			return IOUtils.toString(process.getInputStream());
		} catch (IOException e) {
			return "";
		}
	}
	
	String _location;
	String location() {
		if (_location == null) {
			_location = System.getenv().get("WAN_IP");
			if (_location == null) {
				report("Getting IP from http://ipecho.net/plain.  Set WAN_IP environment variable to make this go faster.");
				_location = get_ip();
				report("IP is "+_location);
			}
			if (_location == null || _location.isEmpty()) {
				_location = "N/A";
			}
		}
		return _location;
	}
	
	JsonObject status() {
		JsonObject json = new JsonObject();
		json.addProperty("id", _client_id);
		json.addProperty("report_time", now());
		json.addProperty("location", location());
		json.addProperty("start_time", _created_at);
		json.add("statistics", _gson.toJsonTree(_statistics));
		
		JsonArray solver_status = new JsonArray();
		solver_status.add(new JsonPrimitive(_solver_status));
		solver_status.add(new JsonPrimitive(_solver_status_updated));
		solver_status.add(new JsonPrimitive(_solutions_since_satfc_reset));
		json.add("solver_status", solver_status);
		
		return json;	
	}
	
	String config_to_s() {
		return "JavaJobClient";
	}
	
	void report(String msg) {
		System.out.println(config_to_s() + ": " + msg);
	}
	
	void report_status() {
		if (now() - _last_status_report_time < JobCaster.CLIENT_STATUS_REPORT_INTERVAL) {
			return;
		}
		
		String location = _redis_url == null ? "localhost" : _redis_url;
		report("Reporting status to server at "+location+".");
		_caster.report_status(_client_id, status());
		_last_status_report_time = now();
	}
	
	void increment_statistic(String key, double increment) {
		Double wrapper = (Double)_statistics.get(key);
		double aggregate = wrapper == null ? 0 : wrapper.doubleValue();
		_statistics.put(key, aggregate + increment);
	}
	
	void record_fc(String new_station, String answer, double satfc_time, double working_time) {
		increment_statistic(answer, 1);
		increment_statistic("satfc_time", satfc_time);
		increment_statistic("working_time", working_time);		
	}

	void record_poll_time(double period) {
		increment_statistic("poll_time", period);
	}
	
	String _solver_status;
	double _solver_status_updated;
	void set_solver_status(String message) {
		_solver_status = message;
		_solver_status_updated = now();
	}
	
	long _solutions_since_satfc_reset = 0;
	void reset_successful_solution_count() {
		_solutions_since_satfc_reset = 0;
	}
	void increment_successful_solution_count() {
		++_solutions_since_satfc_reset;
	}	
	
	//////////////////////////
	// Solve a problem.
	//
	FeasibilityResult run_feasibility_check(ProblemSet problem_set, int new_station) {
		boolean use_stub = false;
		//use_stub = true;
		return use_stub ?  stub_feasibility_check(problem_set, new_station) : run_SATFC(problem_set, new_station);
	}
	
	FeasibilityResult stub_feasibility_check(ProblemSet problem_set, int new_station) {
		long sleep_time = (long)(Math.random() * problem_set.get_timeout_ms());
		sleep_for(sleep_time);
		
		return new FeasibilityResult(new_station, Answer.NO, "Immediately answered NO", sleep_time / 1000.0, sleep_time / 1000.0, null);
	}
	
	private long problem_id = 0;
	String next_problem_id() {
		++problem_id;
		return Long.toString(problem_id);
	}
	
	// Indexed by band.
	private static final int[][] CHANNELS_FOR_BAND = {
		{},
		{2, 3, 4, 5, 6},
		{7, 8, 9, 10, 11, 12, 13},
		{14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51}
	};
	
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
		String problem_id = next_problem_id();
		
		/*
		 * Since constraint_set() is the name of the folder containing constraint interference
		 * and domain constraints, we use the argument -c (--constraint_sets_directory) to provide the location of
		 * the directory containing individual constraint sets.
		 */
		String constraint_set = problem_set.get_constraint_set();
		String datafoldername = new File(_constraint_sets_directory, constraint_set).getPath();
		report("Using constraint set at "+datafoldername+".");
		
		/*
		 * Create an instance string as outlined in the SATFC [readme](
		 * https://docs.google.com/document/d/1TuuFr6lxOjv7QMPZztIFzS_34TaE6-qeNJjaHufOrAE/edit):
		 * 
		 * "A formatted instance string is simply a [dash] "-"-separated list of channels, a [dash] "-"-separated list
		 * of stations and an optional previously valid partial channel assignment (in the form of a [dash] "-"-separated
		 * list of station-channel assignments joined by [commas] ","), all [three parts] joined by a "_" [underscore].
		 * For example, the feasibility checking problem of packing stations 100,231 and 597 into channels 14,15,16,17
		 * and 18 with previous assignment 231 to 16 and 597 to 18 is represented by the following formatted instance
		 * string:
		 * 
		 *   14-15-16-17-18_100-231-597_231,16-597,18
		 * 
		 */
		// channels
		StringBuilder instance = new StringBuilder();
		for (int channel : CHANNELS_FOR_BAND[problem_set._band]) {
			if (channel > problem_set._highest) {
				break;
			}
			instance.append(channel);
			instance.append('-');
		}
		instance.setLength(instance.length() - 1);
		instance.append('_');
		
		// stations
		if (problem_set._tentative_assignment != null) {
			for (String station : problem_set._tentative_assignment.keySet()) {
				instance.append(station);
				instance.append('-');
			}
		}
		if (problem_set._tentative_assignment == null ||
				!problem_set._tentative_assignment.keySet().contains(Integer.toString(new_station))) {
			instance.append(new_station);
		}
		
		// optional previously valid partial channel assignment
		 if (problem_set._tentative_assignment != null && !problem_set._tentative_assignment.isEmpty()) {
		 	instance.append('_');
		 	for (Map.Entry<String, Integer> entry : problem_set._tentative_assignment.entrySet()) {
		 		if (entry.getValue().equals(-1)) { // Skip -1 assignments.
		 			continue;
		 		}
		 		instance.append(entry.getKey());
		 		instance.append(',');
		 		instance.append(entry.getValue());
		 		instance.append('-');
		 	}
		 	instance.setLength(instance.length() - 1);
		 }
		
		String instance_string = instance.toString();
		report("Solve instance "+instance_string);
		SolvingJob solvingJob = new SolvingJob(problem_id, datafoldername, instance_string, cutoff, seed, dummyAddress, dummyPort);
		
		ServerResponse solverResponse;
		try {
			solverResponse = fServerSolver.solve(solvingJob);
		} catch (ProblemInitializingOverheadTimeoutException e) {
			solverResponse = e.answerResponse;
		}
		
		double time = watch.stop() / 1000.0;
		
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
			
			//double runtime = Double.valueOf(resultParts[1]);
			
			
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
		
		} catch (UnknownHostException | SolverServerStateInterruptedException | SolvingInterrupedException e) {
			throw new RuntimeException(e);
		}
	}
	
	// Poll the server for work, and do work when it appears.
	@Override
	public void run() {
		set_solver_status("SATFC server starting normally");
		
		for (;;) {
			try {
				report_status();
				
				double start_poll = now(); 
				String job = _caster.block_for_job();
				record_poll_time(now() - start_poll);
				

				if (job != null) {
					report("Found job "+job);
					set_solver_status("SATFC server found job "+job);
									
					// We have a problem to work on.
					String[] parts = job.split(":");
					String new_station = parts[0];
					String problem_set_id = parts[1];
					
					double start_time = now();
					_statistics.put("latest_problem", start_time);
					
					Gson gson = new Gson();
					FeasibilityResult result;
					
					String problem_set_json = _caster.get_problem_set(problem_set_id);
					if (problem_set_json == null) {
						String missing_problem_set = "missing problem set "+problem_set_id;
						set_solver_status(missing_problem_set);
						report_status();
						report(missing_problem_set);
						
						result = new FeasibilityResult(Integer.parseInt(new_station), Answer.ERROR, missing_problem_set, 0.0, 0.0, null);
					} else {
						ProblemSet problem_set = new ProblemSet(problem_set_json);
						result = run_feasibility_check(problem_set, Integer.parseInt(new_station));
						
						report("Result from checker was " + result.get_answer());
						report("Json version of result is " + gson.toJson(result));
					}
					
					Map<String, Double> time_data = new HashMap<String, Double>();
					time_data.put("satfc_time", result.get_time());
					time_data.put("satfc_wall_clock", result.get_wall_clock());
					time_data.put("total_job_client_time", (double)now() - start_time);
					
					String answer = result.get_answer().toString().toLowerCase();
					Object[] answer_raw = new Object[] {
						location(),
						new_station,
						answer,
						result.get_message(),
						time_data
					};
					
					String answer_json = gson.toJson(answer_raw);
					
					report("Answer to return is "+answer_json+"\n\n");
					set_solver_status("SATFC server has answer for job "+job);
	        
					_caster.send_assignment(problem_set_id, new_station, gson.toJson(result.get_witness_assignment()));
					_caster.send_answer(problem_set_id, answer_json);
					
					record_fc(new_station, answer, result.get_time(), now() - start_time);
					increment_successful_solution_count();
				} else if (_caster.is_alive()) {
					report("No work at the moment. Trying again.");
				} else {
					report("Experienced timeout, connection lost? Trying again.");
				}
			} catch (JedisConnectionException e) {
				reconnect();
			} catch (Exception e) {
				report("Unusual exception:");
				e.printStackTrace();
				set_solver_status("Encountered an unusual exception: "+e.getMessage());
				report_status();
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
		
		@Option(name="-c", aliases={"--constraint_sets_directory"}, metaVar="path",
			usage="Path to a directory of constraint sets.  Contents should be of the form $constraint_set_name/{domains,interferences}.csv.")
		String constraint_sets_directory;
    	
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
    				System.exit(0);
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
		 * 
		 * For now, it's easiest to find the SATsolvers directory from walking up from the current directory
		 * looking for fcc-station-packing.
		 * 
		 * @wtaysom
		 */
		ThreadedSolverServerParameters aParameters = new ThreadedSolverServerParameters();
		try {
			File current_working_directory = new File(".");
			File fcc_station_packing_root = current_working_directory.getCanonicalFile();
			while (fcc_station_packing_root != null && !fcc_station_packing_root.getName().equals("fcc-station-packing")) {
				fcc_station_packing_root = fcc_station_packing_root.getParentFile(); 
			}
			if (fcc_station_packing_root == null) {
				throw new FileNotFoundException("Unable to find fcc-station-packing as a parent of "+current_working_directory.getCanonicalPath());
			}
			String file_name = Platform.isMac() ? "libjnaclasp.dylib" : "libjnaclasp.so";			
			aParameters.SolverParameters.Library = new File(fcc_station_packing_root, "SATsolvers/clasp/jna/"+file_name).getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
				
		SolverManager aSolverManager = aParameters.getSolverManager();
		ServerSolverInterrupter aSolverState = new ServerSolverInterrupter();
		ServerSolver aServerSolver = new ServerSolver(aSolverManager, aSolverState, null, null);

		SATFCJobClient aSATFCJobClient = new SATFCJobClient(options, aServerSolver);
		
		/*
		 * Must run both SATFCJobClient and SATFC's ServerSolver.
		 * @author afrechet 
		 */
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
	Map<String, Integer> _tentative_assignment;
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

		_fc_config = null;
		// NOTE: getAsString() sometimes complains java.lang.UnsupportedOperationException: JsonNull, even though
		//       we have checked for it.  We never consume _fc_config, so let's ignore it for now.
		// if (data.get(3).isJsonNull()) {
		// 	_fc_config = null;
		// } else {
		// 	_fc_config = data.get(3).getAsString();
		// }

		if (data.get(4).isJsonNull()) {
			_fc_approach = null;
		} else {
			_fc_approach = data.get(4).getAsString();
		}

		_timeout_ms = data.get(5).getAsInt();

		Gson gson=new Gson();
		_tentative_assignment = gson.fromJson(data.get(6), new TypeToken<HashMap<String, Integer>>(){
			static final long serialVersionUID = 1007; // to avoid warning.
		}.getType());
		
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

	Map<String, Integer> get_tentative_assignment() {
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
			", tentative_assignment: " + _tentative_assignment +
			", testing_flag: " + _testing_flag + "]";
	}
}


// Talk to the Redis server
class JobCaster {
	static final int CLIENT_STATUS_REPORT_INTERVAL = 5;
	static final int CLIENT_STATUS_REPORT_EXPIRATION = 5 * 60;
	
	static final String DEFAULT_JOB_CASTER_URL = "localhost";
	
	// Taken from Tokens::TOKEN in fcctv/trunk/tokens.rb.
	static final String TOKEN = "a052a4001fddb5f13686cfce7325e2b94a93061328c4abb1ac60d6463df1b377";
		
	Jedis _jedis;
	Gson _gson = new Gson();
	
	JobCaster(String url) {
		if (url == null) {
			url = DEFAULT_JOB_CASTER_URL;
		}
		_jedis = new Jedis(url);
		try {
			_jedis.auth(TOKEN);
		} catch (JedisDataException e) {
			if (e.getMessage().equals("ERR Client sent AUTH, but no password is set")) {
				String specifics = url == "localhost" ?
						"If you are running a test, be sure start Redis using ./redis-server-with-auth from fcctv/trunk." :
						"If you are running against an EC2 broker, be sure that /etc/redis/redis.conf has requirepass set to #{TOKEN}.";
				throw new JedisDataException(e.getMessage()+".  "+specifics);
			} else if (e.getMessage().equals("ERR invalid password")) {
				throw new JedisDataException(e.getMessage()+
					".  TOKEN \""+TOKEN+"\" should match Tokens::TOKEN in fcctv/trunk/tokens.rb.");
			} else {
				throw e;
			}
		}
	}
	
	boolean is_alive() {
	  return _jedis.ping().equals("PONG");
	}
	
	void report_error(String error_msg) {
		_jedis.lpush(CLIENT_ERROR_KEY, error_msg);
	}
	
	String get_new_client_id() {
		return get_new_seq_val(CLIENT_ID_SEQ);
	}
	
	void report_status(String client_id, JsonObject status) {
		Transaction tx = _jedis.multi();
		tx.sadd(CLIENT_IDS_SET, client_id);
		// Set the status and give it an expiration time.
		tx.set(client_status_key_for(client_id), _gson.toJson(status));
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
	static final String CLIENT_ID_SEQ = "client_id_seq";
	static final String CLIENT_IDS_SET = "client_ids";
	static final String CLIENT_STATUS_PREFIX = "client.status";
	static final String CLIENT_ERROR_KEY = "client_errors";
	static final String ANSWER_PREFIX = "answer";
	static final String PROBLEM_SET_PREFIX = "problem_set";
	
	String get_new_seq_val(String key) {
		Transaction tx = _jedis.multi();
		tx.incr(key);
		Response<String> response = tx.get(key);
		tx.exec();
		return response.get();
	}
	
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
