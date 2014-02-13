import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import ca.ubc.cs.beta.aclib.logging.LogLevel;
import ca.ubc.cs.beta.stationpacking.base.Station;
import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.SolverManager;
import ca.ubc.cs.beta.stationpacking.daemon.datamanager.solver.bundles.ISolverBundle;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.SATFCParameters;
import ca.ubc.cs.beta.stationpacking.solvers.ISolver;
import ca.ubc.cs.beta.stationpacking.solvers.base.SATResult;
import ca.ubc.cs.beta.stationpacking.solvers.base.SolverResult;
import ca.ubc.cs.beta.stationpacking.solvers.termination.ITerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.composite.DisjunctiveCompositeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.cputime.CPUTimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.solvers.termination.walltime.WalltimeTerminationCriterion;
import ca.ubc.cs.beta.stationpacking.utils.RunnableUtils;
import ca.ubc.cs.beta.stationpacking.utils.Watch;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
	public static final String VERSION = "2014-01-24"; // Generally use the release date.
	
	private static Logger log;
	
	public enum Answer { YES, NO, UNKNOWN, ERROR }
	
	/*
	 * A SolverManager to get you the good solver for your problem.
	 * @author wtaysom
	 */
	private final SolverManager fSolverManager;
	
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
	public SATFCJobClient(Options options, SolverManager aSolverManager) {
		
		fSolverManager = aSolverManager;
		
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
		
		_name = options.name;
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
	String _name;
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
		String ip = "unkown";
		try {
			Process process = Runtime.getRuntime().exec("curl -s http://ipecho.net/plain");
			String result = IOUtils.toString(process.getInputStream());
			if (result.length() <= "000.000.000.000".length()) { // guard against HTTP 500 errors, etc.
				ip = result;
			}
		} catch (IOException e) {}
		return ip;
	}
	
	String _location;
	String location() {
		if (_location == null) {
			String ip = System.getenv().get("WAN_IP");
			if (ip == null) {
				report("Getting IP from http://ipecho.net/plain.  Set WAN_IP environment variable to make this go faster.");
				ip = get_ip();
				report("IP is "+ip);
			}
			
			_location = ip;
			if (_name != null) {
				_location += " #"+_name;
			}
			
			_location += " v"+VERSION;
			
			report("location set to: "+_location);
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
		
	void report(String msg) {
		log.info(msg);
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
		long seed = 1;
		
		/**
		 * Parse problem into solving job for solver server.
		 * 
		 * TODO It is not necessary to pass by a string to create a StationPackingInstance, but it is the easiest for now.
		 */
		
		/*
		 * Since constraint_set() is the name of the folder containing constraint interference
		 * and domain constraints, we use the argument -c (--constraint_sets_directory) to provide the location of
		 * the directory containing individual constraint sets.
		 */
		String constraint_set = problem_set.get_constraint_set();
		String datafoldername = new File(_constraint_sets_directory, constraint_set).getPath();
		log.debug("Using constraint set at "+datafoldername+".");
		
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
		StringBuilder instance_string_builder = new StringBuilder();
		for (int channel : CHANNELS_FOR_BAND[problem_set._band]) {
			if (channel > problem_set._highest) {
				break;
			}
			instance_string_builder.append(channel);
			instance_string_builder.append('-');
		}
		instance_string_builder.setLength(instance_string_builder.length() - 1);
		instance_string_builder.append('_');
		
		// stations
		if (problem_set._tentative_assignment != null) {
			for (String station : problem_set._tentative_assignment.keySet()) {
				instance_string_builder.append(station);
				instance_string_builder.append('-');
			}
		}
		if (problem_set._tentative_assignment == null ||
				!problem_set._tentative_assignment.keySet().contains(Integer.toString(new_station))) {
			instance_string_builder.append(new_station);
		}
		
		// optional previously valid partial channel assignment
		 if (problem_set._tentative_assignment != null && !problem_set._tentative_assignment.isEmpty()) {
		 	instance_string_builder.append('_');
		 	for (Map.Entry<String, Integer> entry : problem_set._tentative_assignment.entrySet()) {
		 		if (entry.getValue().equals(-1)) { // Skip -1 assignments.
		 			continue;
		 		}
		 		instance_string_builder.append(entry.getKey());
		 		instance_string_builder.append(',');
		 		instance_string_builder.append(entry.getValue());
		 		instance_string_builder.append('-');
		 	}
		 	instance_string_builder.setLength(instance_string_builder.length() - 1);
		 }
		
		String instance_string = instance_string_builder.toString();
		log.debug("Solve instance "+instance_string);
		
		/**
		 * Grab the solving components from solver manager.
		 */
		
		//Grab the solving bundle corresponding to the data.
		ISolverBundle bundle;
		try {
			bundle = fSolverManager.getData(datafoldername);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not find the necessary problem data to instantiate SATFC.");
		}
		
		//Grab the data managers from the bundle.
		IStationManager stationManager = bundle.getStationManager();
		
		//Create the instance we want to solve from the instance string and data managers.
		StationPackingInstance instance = StationPackingInstance.valueOf(instance_string, stationManager);
		
		//Do per instance solver selection with the bundle.
		ISolver solver = bundle.getSolver(instance);
		
		//Setup termination criteria (and start them).
		double cutoff = problem_set.get_timeout_ms()/1000.0;
		log.debug("Cutoff {} s",cutoff);
		ITerminationCriterion cputimeTermination = new CPUTimeTerminationCriterion(cutoff);
		ITerminationCriterion walltimeTermination = new WalltimeTerminationCriterion(cutoff*1.5);
		ITerminationCriterion terminationCriterion = new DisjunctiveCompositeTerminationCriterion(Arrays.asList(cputimeTermination,walltimeTermination));
		
		/**
		 * Solve the instance.
		 */
		
		//Solve the instance
		watch.stop();
		SolverResult result = solver.solve(instance, terminationCriterion, seed);
		watch.start();
		
		/**
		 * Convert result into a feasibility result.
		 */
		
		Answer answer = Answer.UNKNOWN;
		double runtime = result.getRuntime();
		Map<Integer,Integer> witness = new HashMap<Integer,Integer>();
	
		switch(result.getResult())
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
		
		//witness assignment maps station (ID's) to the assigned channel.
		if(result.getResult().equals(SATResult.SAT))
		{
			for(Entry<Integer,Set<Station>> entry : result.getAssignment().entrySet())
			{
				int channel = entry.getKey();
				for(Station station : entry.getValue())
				{
					witness.put(station.getID(), channel);
				}
			}
		}
		
		//Also piping in as message the full solver result to string (which might be pretty long), but may be useful for debugging purpose.
		
		watch.stop();
		double extraTime = watch.getEllapsedTime();
		double time = runtime + extraTime;
		
		FeasibilityResult feasibility_result = new FeasibilityResult(new_station, answer, result.toString(),runtime , time, witness);
		
		return feasibility_result;

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
					
					try {
						String problem_set_json = _caster.get_problem_set(problem_set_id);
						if (problem_set_json == null) {
							String missing_problem_set = "missing problem set "+problem_set_id;
							set_solver_status(missing_problem_set);
							report_status();
							report(missing_problem_set);
							
							double time = now() - start_time;
							result = new FeasibilityResult(Integer.parseInt(new_station), Answer.ERROR, missing_problem_set, time, time, null);
						} else {
							ProblemSet problem_set = new ProblemSet(problem_set_json);
							result = run_feasibility_check(problem_set, Integer.parseInt(new_station));
							
							report("Result from checker was " + result.get_answer());
							report("Json version of result is " + gson.toJson(result));
						}
					} catch (Exception e) {
						report("Unusual exception (with feasibility checking):");
						e.printStackTrace();
						set_solver_status("Encountered an unusual exception (with feasibility checking): "+e.getMessage());
						report_status();
						
						JsonObject error_json = new JsonObject();
						error_json.addProperty("job", job);
						error_json.addProperty("location", location());
						error_json.addProperty("error message", e.getMessage());
						_caster.report_error(error_json.toString());
						
						double time = now() - start_time;
						result = new FeasibilityResult(Integer.parseInt(new_station), Answer.ERROR, e.getMessage(), time, time, null);
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
				report("Unusual exception (with Redis communication):");
				e.printStackTrace();
				set_solver_status("Encountered an unusual exception (with Redis communication): "+e.getMessage());
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
		
		@Option(name="-n", aliases={"--name"}, metaVar="name",
			usage="Name for this SATFCJobClient instance.")
		String name;
		
		@Option(name="-l", aliases={"--clasp-lib"}, metaVar="libjnaclasp",
			usage="Path to Clasp library, libjnaclasp.dylib on OS X and libjnaclasp.so on Linux.  Defaults to searching fcc-station-packing/SATsolvers/clasp/jna/.")
		String claspLib;
    	
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
		 * TODO @afrechet Flesh out options to define only the necessary components.
		 * 
		 * @afrechet
		 * 
		 * For now, it's easiest to find the SATsolvers directory from walking up from the current directory
		 * looking for fcc-station-packing.
		 * 
		 * @wtaysom
		 */
		SATFCParameters parameters = new SATFCParameters();
		if (options.claspLib == null) {
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
				parameters.SolverManagerParameters.SolverParameters.Library = new File(fcc_station_packing_root, "SATsolvers/clasp/jna/"+file_name).getCanonicalPath();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			try {
				parameters.SolverManagerParameters.SolverParameters.Library = new File(options.claspLib).getCanonicalPath();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
			
		/*
		 * Set logging options.
		 * Note that conf/logback.xml needs to be packaged with executable for logging to work properly.
		 */
		parameters.LoggingOptions.logLevel = LogLevel.TRACE;
		parameters.LoggingOptions.initializeLogging();
		
		log = LoggerFactory.getLogger(SATFCJobClient.class);
		
		SolverManager aSolverManager = parameters.SolverManagerParameters.getSolverManager();

		SATFCJobClient aSATFCJobClient = new SATFCJobClient(options, aSolverManager);
		
		/*
		 * Must run both SATFCJobClient and SATFC's ServerSolver.
		 * @author afrechet 
		 */
		RunnableUtils.submitRunnable(EXECUTOR_SERVICE, UNCAUGHT_EXCEPTION_HANDLER, aSATFCJobClient);
		
		try {
			EXECUTOR_SERVICE.awaitTermination(365*10, TimeUnit.DAYS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			System.err.println("SATFC job client executor service interrupted ("+e1+").");
			return;
		}
	
		System.exit(TERMINATION_STATUS.get());
	}
}




// Thin wrapper to unpack the json version and provide accessors
// Wow.  I do not miss Java at times like this. :(
class ProblemSet {
	private static final int NO_HIGHEST_CHANNEL = Integer.MAX_VALUE;
	
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
		JsonElement highest_element = data.get(1);
		_highest = highest_element.isJsonNull() ? NO_HIGHEST_CHANNEL : highest_element.getAsInt();
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
