// 
// Copyright (c) 2006-2012, Benjamin Kaufmann
// 
// This file is part of Clasp. See http://www.cs.uni-potsdam.de/clasp/ 
// 
// Clasp is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// Clasp is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with Clasp; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
//
#include "clasp_app.h"
#include "alarm.h"
#include "program_opts/composite_value_parser.h"  // pair and vector
#include <iostream>
#include <fstream>
#include <cstdlib>
#include <climits>
#if !defined(_WIN32)
#include <unistd.h> // for _exit
#endif
#include <clasp/clause.h>
/////////////////////////////////////////////////////////////////////////////////////////
// Application
/////////////////////////////////////////////////////////////////////////////////////////
namespace Clasp {
#if !defined(CLASP_USAGE)
#define CLASP_USAGE   "clasp [number] [options] [file]"
#endif
#if !defined (SIGUSR1)
#define SIGUSR1 SIGTERM
#endif
#if !defined(SIGUSR2)
#define SIGUSR2 SIGTERM
#endif
inline std::ostream& operator << (std::ostream& os, Literal l) {
	if (l.sign()) os << '-';
	os << l.var();
	return os;
}
inline std::istream& operator >> (std::istream& in, Literal& l) {
	int i;
	if (in >> i) {
		l = Literal(i >= 0 ? Var(i) : Var(-i), i < 0);
	}
	return in;
}
inline bool isStdIn(const std::string& in)  { return in == "-" || in == "stdin"; }
inline bool isStdOut(const std::string& out){ return out == "-" || out == "stdout"; }
/////////////////////////////////////////////////////////////////////////////////////////
// Clasp specific app options
/////////////////////////////////////////////////////////////////////////////////////////
ClaspAppOptions::ClaspAppOptions() : quiet(-1, -1), verbose(1), timeout(0), stats(0), outf(0), ifs(' '), outLbd(Activity::MAX_LBD), inLbd(Activity::MAX_LBD), fastExit(false)  {}
void ClaspAppOptions::initOptions(ProgramOptions::OptionContext& root) {
	using namespace ProgramOptions;
	OptionGroup basic("Basic Options");
	basic.addOptions()
		("verbose,V"  , storeTo(verbose)->implicit("3")->defaultsTo("1")->arg("<n>"), "Set verbosity level to %A")
		("stats,s"   , storeTo(stats)->implicit("1")->arg("{0..2}"), "Print {0=no|1=basic|2=extended} statistics")
		("quiet,q"    , storeTo(quiet)->implicit("2,2")->arg("<m>,<o>"), 
		 "Configure printing of models and optimize values\n"
		 "      <m>: print {0=all|1=last|2=no} models\n"
		 "      <o>: print {0=all|1=last|2=no} optimize values [<m>]")
		("time-limit", storeTo(timeout)->arg("<n>"), "Set time limit to %A seconds (0=no limit)")
		("outf,@1", storeTo(outf)->arg("<n>"), "Use {0=default|1=competition|2=JSON} output")
		("lemma-out,@1" , storeTo(lemmaOut)->arg("<file>"), "Write learnt lemmas to %A on exit")
		("lemma-out-lbd,@1",notify(this, &ClaspAppOptions::mappedOpts)->arg("<n>"), "Only write lemmas with lbd <= %A")
		("lemma-in,@1"  , storeTo(lemmaIn)->arg("<file>"), "Read additional lemmas from %A")
		("lemma-in-lbd,@1", notify(this, &ClaspAppOptions::mappedOpts)->arg("<n>"), "Initialize lbd of additional lemmas to <n>")
		("fast-exit,@1",  flag(fastExit), "Force fast exit (do not call dtors)")
		("ifs,@1", notify(this, &ClaspAppOptions::mappedOpts), "Internal field separator")
		("file,f,@2", storeTo(input)->composing(), "Input files")
	;
	root.add(basic);
}
bool ClaspAppOptions::mappedOpts(ClaspAppOptions* this_, const std::string& name, const std::string& value) {
	uint32 x;
	if (name == "ifs") {
		if (value.empty() || value.size() > 2) { return false;}
		if (value.size() == 1) { this_->ifs = value[0]; return true; }
		if (value[1] == 't')   { this_->ifs = '\t'; return true; }
		if (value[1] == 'n')   { this_->ifs = '\n'; return true; }
		if (value[1] == 'v')   { this_->ifs = '\v'; return true; }
		if (value[1] == '\\')  { this_->ifs = '\\'; return true; }
	}
	else if (name.find("-lbd") && ProgramOptions::parseValue(value,x,0) && x < Activity::MAX_LBD) {
		if      (name == "lemma-out-lbd") { this_->outLbd = (uint8)x; return true; }
		else if (name == "lemma-in-lbd")  { this_->inLbd  = (uint8)x; return true; }
	}
	return false; 
}
bool ClaspAppOptions::validateOptions(const ProgramOptions::ParsedOptions&, ProgramOptions::Messages&) {
	if (quiet.second == UINT32_MAX) { quiet.second = quiet.first; }
	return true;
}
/////////////////////////////////////////////////////////////////////////////////////////
// public functions & basic helpers
/////////////////////////////////////////////////////////////////////////////////////////
Application::Application() : timeToFirst_(-1.0), timeToLast_(-1.0), facade_(0), blocked_(0), pending_(0)  {}
Application& Application::instance() {
	static Application inst;
	return inst;
}
void Application::sigHandler(int sig) {
	Application::instance().kill(sig);
}

// Kills any pending alarm
void Application::killAlarm() {
	if (app_.timeout>0) {
		setAlarm(0); 
	}
}

// Called on timeout or signal.
// Prints summary and then kills the application.
void Application::kill(int sig) {
	if (blocked_ == 0) {
		blockSignals();         // ignore further signals
		SCOPE_ALARM_LOCK();
		INFO_OUT("clasp", "INTERRUPTED by signal!");
		if (!facade_ || !facade_->terminate()) {
			if (facade_ && facade_->state() != ClaspFacade::num_states) {
				if (facade_->state() != ClaspFacade::state_start) { timer_[facade_->state()].stop(); }
				timer_[ClaspFacade::state_start].stop();
				cpuTotalTime_.stop();
				printResult(sig);
			}
			appTerminate(config_.ctx.enumerator()->enumerated > 0 ?  S_SATISFIABLE : S_UNKNOWN);
		}
		else {                  // multiple threads are active - shutdown was initiated
			INFO_OUT("clasp", "Shutting down threads...");
		}
	}
	else if (pending_ == 0) { // signals are currently blocked because output is active
		INFO_OUT("clasp", "Queueing signal...");
		pending_ = sig;
	}
}

// temporarily disable delivery of signals
int Application::blockSignals() {
	return blocked_++;
}

// re-enable signal handling and deliver any pending signal
void Application::unblockSignals(bool deliverPending) {
	if (--blocked_ == 0) {
		int pend = pending_;
		pending_ = 0;
		// directly deliver any pending signal to our sig handler
		if (pend && deliverPending) { kill(pend); }
	}
}

void Application::installSigHandlers() {
	if (signal(SIGINT, &Application::sigHandler) == SIG_IGN) {
		signal(SIGINT, SIG_IGN);
	}
	if (signal(SIGTERM, &Application::sigHandler) == SIG_IGN) {
		signal(SIGTERM, SIG_IGN);
	}
	if (SIGUSR1 != SIGTERM && (signal(SIGUSR1, &Application::sigHandler) == SIG_IGN)) {
		signal(SIGUSR1, SIG_IGN);
	}
	if (SIGUSR2 != SIGTERM && (signal(SIGUSR2, &Application::sigHandler) == SIG_IGN)) {
		signal(SIGUSR2, SIG_IGN);
	}
	if (app_.timeout > 0) {
		setAlarmHandler(&Application::sigHandler);
		if (setAlarm(app_.timeout) == 0) {
			messages.warning.push_back("Could not set time limit!");
		}
	}
}
std::istream& Application::getStream() {
	ProgramOptions::StringSeq& input = app_.input;
	if (input.empty() || isStdIn(input[0])) {
		input.resize(1, "stdin");
		return std::cin;
	}
	else {
		static std::ifstream file;
		if (file.is_open()) return file;
		file.open(input[0].c_str());
		if (!file) { throw std::runtime_error("Can not read from '"+input[0]+"'");  }
		return file;
	}
}

Application::HelpOpt Application::initHelpOption() const {
	return HelpOpt(ProgramOptions::desc_level_e2, "Print {1=basic|2=more|3=full} help and exit");
}

void Application::printDefaultConfigs() const {
	uint32 minW = 2, maxW = 80;
	OptionConfig c("[empty]:");
	for (const char* cfg = defConfigs_g; *cfg; cfg += strlen(cfg) + 1) {
		c.initFromRaw(cfg);
		printf("[%s]:\n%*c", c.name.c_str(), minW-1, ' ');
		// split options into formatted lines
		uint32 sz = c.cmdLine.size(), off = 0, n = maxW - minW;
		while (n < sz) {
			while (n != off  && c.cmdLine[n] != ' ') { --n; }
			if (n != off) { c.cmdLine[n] = 0; printf("%s\n%*c", &c.cmdLine[off], minW-1, ' '); }
			else          { break; }
			off = n+1;
			n   = (maxW - minW) + off;
		}
		printf("%s\n", c.cmdLine.c_str()+off);
	}
}
void Application::printHelp(const ProgramOptions::OptionContext& root) {
	printf("%s\n", clasp_app_banner);
	printf("usage: %s\n", CLASP_USAGE);
	ProgramOptions::FileOut out(stdout);
	root.description(out);
	printf("\n\nusage: %s\n", CLASP_USAGE);
	printf("Default command-line:\nclasp %s\n", root.defaults(strlen("clasp ")).c_str());
	if (root.getActiveDescLevel() >= ProgramOptions::desc_level_e1) {
		printf("[asp] %s\n", clasp_.getInputDefaults(Input::SMODELS));
		printf("[cnf] %s\n", clasp_.getInputDefaults(Input::DIMACS));
		printf("[opb] %s\n", clasp_.getInputDefaults(Input::OPB));
	}
	if (root.getActiveDescLevel() >= ProgramOptions::desc_level_e2) {
		printf("\nDefault configurations:\n");
		printDefaultConfigs();
	}
	printf("\nclasp is part of Potassco: %s\n", "http://potassco.sourceforge.net/#clasp");
	printf("Get help/report bugs via : http://sourceforge.net/projects/potassco/support\n");
	fflush(stdout);
}

void Application::printVersion(const ProgramOptions::OptionContext&) {
	printf("%s\n", clasp_app_banner);
	printf("Address model: %d-bit\n", (int)(sizeof(void*)*CHAR_BIT));
	printf("Configuration: WITH_CLASPRE=%d WITH_THREADS=%d", WITH_CLASPRE, WITH_THREADS);
#if WITH_THREADS
	printf(" (Intel TBB version %d.%d)", TBB_VERSION_MAJOR, TBB_VERSION_MINOR);
#endif
	printf("\n%s\n", CLASP_LEGAL);
	fflush(stdout);
}
void Application::printTemplate() const {
#if WITH_THREADS
	printf("# clasp %s portfolio file\n", CLASP_VERSION);
	printf("# A portfolio file contains a (possibly empty) list of configurations.\n"
	       "# Each of which must have the following format:\n"
	       "#   [<name>]: <cmd>\n"
	       "# where <name> is a string that must not contain ']'\n"
	       "# and   <cmd>  is a command-line style list of options\n"
	       "# from \"Search Options\" and/or \"Lookback Options\".\n"
	       "#\n"
	       "# SEE: clasp --help\n"
	       "#\n"
	       "# NOTE: Options given on the command-line are added to all configurations in a\n"
	       "#       portfolio file. If an option is given both on the command-line and in a\n"
	       "#       portfolio configuration, the one from the command-line is preferred.\n"
	       "#\n"
	       "# NOTE: If, after adding command-line options, a portfolio configuration\n"
	       "#       contains mutually exclusive options an error is raised.\n"
	       "#\n"
	       "# EXAMPLE for up to %u threads:\n", CLASP_DEFAULT_PORTFOLIO_SIZE);
	for (const char* p = portfolio_g; *p;) {
		printf("%s\n", p);
		p += strlen(p) + 1;
	}
#endif
}

void Application::printWarnings() const {
	for (ProgramOptions::StringSeq::const_iterator it = messages.warning.begin(); it != messages.warning.end(); ++it) {
		WARNING_OUT("clasp", it->c_str());
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// run - clasp's "main"-function
/////////////////////////////////////////////////////////////////////////////////////////
int Application::run(int argc, char** argv) {
	if (!parse(argc, argv, "clasp", parsePositional)) {
		// command-line error
		ERROR_OUT("clasp", messages.error.c_str());
		INFO_OUT("clasp", "Try '--help' for usage information");
		return S_ERROR;
	}
	if (help || version) {
		return EXIT_SUCCESS;
	}
	if (claspre_.listFeatures) {
		claspre_.printFeatures();
		return EXIT_SUCCESS;
	}
	if (clasp_.genTemplate) {
		printTemplate();
		return EXIT_SUCCESS;
	}
	if (!app_.lemmaIn.empty() && !isStdIn(app_.lemmaIn) && !std::ifstream(app_.lemmaIn.c_str()).is_open()) {
		ERROR_OUT("clasp", "'lemma-in': could not open file!");
		return EXIT_FAILURE;
	}
	std::ofstream outF;
	if (!app_.lemmaOut.empty()) {
		std::ostream* os = &std::cout;
		if (!isStdOut(app_.lemmaOut)) {
			if (std::find(app_.input.begin(), app_.input.end(), app_.lemmaOut) != app_.input.end() || app_.lemmaIn == app_.lemmaOut) {
				ERROR_OUT("clasp", "'lemma-out': cowardly refusing to overwrite input file!");
				return EXIT_FAILURE;
			}
			outF.open(app_.lemmaOut.c_str());
			if (!outF.is_open()) {
				ERROR_OUT("clasp", "'lemma-out': could not open file for writing!");
				return EXIT_FAILURE;
			}
			os = &outF;
		}
		writeLemmas_.reset( new WriteLemmas(*os) );
		writeLemmas_->attach(config_.ctx);
	}
	installSigHandlers();
	int retStatus = S_UNKNOWN;
	ClaspFacade clasp;
	stats_.clear();
	SolveStats agg;
	try {
		StreamInput input(getStream(), detectFormat(getStream()));
		clasp_.applyDefaults(input.format());
		configureOutput(input.format());
		if (app_.stats) {
			config_.master()->solver->stats.enableStats(app_.stats);
			stats_.push_back(&config_.master()->solver->stats);
			if (config_.numSolvers() > 1) { 
				stats_.reserve(config_.numSolvers()+1);
				agg.enableStats(app_.stats);
				agg.enableParallelStats(); 
				stats_[0] = &agg;
			}
		}
		facade_ = &clasp;
		cpuTotalTime_.start();
		clasp.solve(input, config_, this);
		cpuTotalTime_.stop();
		int sig = blockSignals();// disable signal handler
		killAlarm();             // kill any pending alarms;
		printResult(sig);
		if      (clasp.result() == ClaspFacade::result_unsat) retStatus = S_UNSATISFIABLE;
		else if (clasp.result() == ClaspFacade::result_sat)   retStatus = S_SATISFIABLE;
		else                                                  retStatus = S_UNKNOWN;
	}
	catch (const std::bad_alloc&  ) { retStatus = exception(S_MEMORY, "std::bad_alloc"); }
	catch (const std::exception& e) { retStatus = exception(S_ERROR, e.what()); }
	if    (app_.fastExit)           { appTerminate(retStatus);        }
	else                            { fflush(stdout); fflush(stderr); }
	return retStatus;
}

void Application::appTerminate(int status) const {
	fflush(stdout);
	fflush(stderr);
	_exit(status);
}
int Application::exception(int status, const char* what) {
	blockSignals();
	app_.fastExit = true;
	ERROR_OUT("clasp", what);
	if (facade_ && facade_->state() != ClaspFacade::num_states) {
		cpuTotalTime_.stop();
		printResult(status);
	}
	return status;
}
/////////////////////////////////////////////////////////////////////////////////////////
// State & Result functions
/////////////////////////////////////////////////////////////////////////////////////////
// Generates a summary after search has stopped or has been interrupted.
// The summary is then passed to the output object which is responsible
// for printing.
void Application::printResult(int sig) {
	OutputFormat::RunSummary sol(config_.ctx);
	sol.termId       = sig;
	sol.sig          = sig != 0;
	sol.comp         = (sig == 0 && !facade_->more());
	sol.consequences = config_.consequences() ? config_.cbType() : 0;
	sol.totalTime    = timer_[0].total();
	sol.solveTime    = timer_[ClaspFacade::state_solve].total();
	sol.modelTime    = timeToFirst_ != -1.0 ? timeToFirst_ : 0.0;
	double ttl       = timeToLast_ != -1.0 ? timeToLast_ : 0.0;
	sol.unsatTime    = sol.complete() && sol.solveTime-ttl >= 0.001 ? sol.solveTime-ttl : 0.0;
	sol.cpuTime      = std::max(cpuTotalTime_.total(), 0.0);
	if (config_.enumerate.onlyPre) {
		if (sig) return;
		if (facade_->api()) { // asp-mode
			facade_->result() == ClaspFacade::result_unsat
				? (void)(std::cout << "0\n0\nB+\n1\n0\nB-\n1\n0\n0\n")
				: facade_->api()->writeProgram(std::cout);
			delete facade_->releaseApi();
		}
		else {
			if (facade_->result() != ClaspFacade::result_unsat) {
				WARNING_OUT("clasp", "Search not started because of option '--pre'!");
			}
			out_->reportResult(sol, 0, 0);
		}
		return;
	}
	if (!stats_.empty() && config_.numSolvers() > 1) {
		// aggregate stats in stats_[0]
		SolveStats* agg = const_cast<SolveStats*>(stats_[0]);
		for (uint32 i = 0; i != config_.numSolvers(); ++i) {
			SolveStats* t = &config_.getSolver(i)->solver->stats;
			agg->accu(*t);
			if (app_.stats > 1) { stats_.push_back(t); }
		}
	}	
	if (!stats_.empty() && !sig) { sol.termId = sol.ctx.winner(); }
	out_->reportResult(sol, !stats_.empty() ? &stats_[0] : 0, (uint32)stats_.size());
	if (writeLemmas_.get()) {
		Constraint_t::Set x; x.addSet(Constraint_t::learnt_conflict);
		writeLemmas_->flush(x, app_.outLbd);
		writeLemmas_->detach();
		writeLemmas_.reset(0);
	}
}

// State-transition callback called by ClaspFacade.
// Handles timing and notifies output object
void Application::state(ClaspFacade::Event e, ClaspFacade& f) { 
	SCOPE_ALARM_LOCK();
	if (e == ClaspFacade::event_state_enter) {
		out_->reportState(f.state(), true, 0);
		timer_[f.state()].start();
		if (f.state() == ClaspFacade::state_solve && !app_.lemmaIn.empty()) {
			readLemmas();
		}
	}
	else if (e == ClaspFacade::event_state_exit) {
		timer_[f.state()].stop();
		out_->reportState(f.state(), false, timer_[f.state()].total());
	}
	printWarnings();
	messages.warning.clear();
}

// Event callback called by ClaspFacade.
// Notifies output object about models
void Application::event(const Solver& s, ClaspFacade::Event e, ClaspFacade& f) {
	if (e == ClaspFacade::event_model) {
		timer_[f.state()].lap();
		timeToLast_ = timer_[f.state()].total();
		if (timeToFirst_ == -1.0) {  timeToFirst_ = timeToLast_; }
		if (!out_->quiet()){
			model(s, *s.sharedContext()->enumerator(), config_.consequences());
		}
	}
	else if (e == ClaspFacade::event_p_prepared) {
		if (config_.enumerate.onlyPre) {
			if (f.api()) f.releaseApi(); // keep api so that we can later print the program
			return;
		}
		out_->initSolve(s, f.api());
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// status & output
/////////////////////////////////////////////////////////////////////////////////////////
// Creates output object suitable for given input format
void Application::configureOutput(Input::Format f) {
	if (config_.enumerate.onlyPre) {
		app_.verbose = 0;
	}
	if (claspre_.features == 0) {
		if (app_.outf != ClaspAppOptions::out_json) {
			DefaultOutput::Format outFormat = DefaultOutput::format_asp;
			if      (f == Input::DIMACS) { outFormat = DefaultOutput::format_sat09; }
			else if (f == Input::OPB)    { outFormat = DefaultOutput::format_pb09; }
			else if (f == Input::SMODELS && app_.outf == ClaspAppOptions::out_comp) {
				outFormat = DefaultOutput::format_aspcomp;
			}
			out_.reset(new DefaultOutput(app_.verbose, app_.quiet, outFormat, app_.ifs));
		}
		else {
			out_.reset(new JsonOutput(app_.verbose, app_.quiet));
		}
	}
	else if (f == Input::SMODELS) {
		// claspre output
		out_.reset(claspre_.createOutput(app_.verbose, app_.quiet, DefaultOutput::format_asp));
		config_.setMaxSolvers(1);
		if (!claspre_.hasLimit) {
			config_.solve.limit = SolveLimits(500,20);
		}
		app_.stats = 2;
		config_.ctx.enableProgressReport(out_.get());
	}
	else { throw std::runtime_error("Feature extraction not supported for current input format!"); }
	if (out_->verbosity() > 1) {
		config_.ctx.enableProgressReport(out_.get());
	}
	out_->init(clasp_app_banner, app_.input[0]);
}
void Application::model(const Solver& s, const Enumerator& e, bool cons) {
	SCOPE_ALARM_LOCK();
	blockSignals();
	if (!cons) { out_->reportModel(s, e); }
	else       { out_->reportConsequences(s, e,  config_.cbType()); }
	unblockSignals(true);
}
/////////////////////////////////////////////////////////////////////////////////////////
// internal helpers
/////////////////////////////////////////////////////////////////////////////////////////
void Application::readLemmas() {
	std::ifstream fileStream;	
	std::istream& file = isStdIn(app_.lemmaIn.c_str()) ? std::cin : (fileStream.open(app_.lemmaIn.c_str()), fileStream);
	Solver& s          = *config_.master()->solver;
	bool ok            = !s.hasConflict();
	uint32 numVars;
	for (ClauseCreator clause(&s); file && ok; ) {
		while (file.peek() == 'c' || file.peek() == 'p') { 
			const char* m = file.get() == 'p' ? " cnf" : " clasp";
			while (file.get() == *m) { ++m; }
			if (!*m && (!(file >> numVars) || numVars != config_.ctx.numVars()) ) {
				throw std::runtime_error("Wrong number of vars in file: "+app_.lemmaIn); 
			}
			while (file.get() != '\n' && file) {} 
		}
		Literal x; bool elim = false; 
		clause.start(Constraint_t::learnt_conflict);
		clause.setLbd(app_.inLbd);
		while ( (file >> x) ) {
			if (x.var() == 0)         { ok = elim || clause.end(); break; }			
			elim = elim || config_.ctx.eliminated(x.var());
			if (!s.validVar(x.var())) { throw std::runtime_error("Bad variable in file: "+app_.lemmaIn); }
			if (!elim)                { clause.add(x); }
		}
		if (x.var() != 0) { throw std::runtime_error("Unrecognized format: "+app_.lemmaIn); }
	}
	if (ok && !file.eof()){ throw std::runtime_error("Error reading file: "+app_.lemmaIn); }
	s.simplify();
}

WriteLemmas::WriteLemmas(std::ostream& os) : ctx_(0), os_(os) {}
WriteLemmas::~WriteLemmas()  { detach(); }
void WriteLemmas::detach() { if (ctx_) { ctx_ = 0; } }
void WriteLemmas::attach(SharedContext& ctx) {
	detach();
	ctx_ = &ctx;
}
bool WriteLemmas::unary(Literal p, Literal x) const {
	if (!isSentinel(x) && x.asUint() > p.asUint() && (p.watched() + x.watched()) != 0) {
		os_ << ~p << " " << x << " 0\n"; 
		++outShort_;
	}
	return true;
}
bool WriteLemmas::binary(Literal p, Literal x, Literal y) const {
	if (x.asUint() > p.asUint() && y.asUint() > p.asUint() && (p.watched() + x.watched() + y.watched()) != 0) {
		os_ << ~p << " " << x << " " << y << " 0\n"; 
		++outShort_;
	}
	return true;
}
// NOTE: ON WINDOWS this function is unsafe if called from time-out handler because
// it has potential races with the main thread
void WriteLemmas::flush(Constraint_t::Set x, uint32 maxLbd) {
	if (!ctx_ || !os_) { return; }
	// write problem description
	os_ << "c clasp " << ctx_->numVars() << "\n";
	// write learnt units
	Solver& s         = *ctx_->master();
	const LitVec& t   = s.trail();
	Antecedent trueAnte(posLit(0));
	for (uint32 i = ctx_->topLevelSize(), end = s.decisionLevel() ? s.levelStart(1) : t.size(); i != end; ++i) {
		const Antecedent& a = s.reason(t[i]);
		if (a.isNull() || a.asUint() == trueAnte.asUint()) {
			os_ << t[i] << " 0\n";
		}
	}
	// write implicit learnt constraints
	uint32 numLearnts = ctx_->shortImplications().numLearnt();
	outShort_         = 0;
	for (Var v = 1; v <= ctx_->numVars() && outShort_ < numLearnts; ++v) {
		ctx_->shortImplications().forEach(posLit(v), *this);
		ctx_->shortImplications().forEach(negLit(v), *this);
	}
	// write explicit learnt conflict constraints matching the current filter
	LitVec lits; ClauseHead* c;
	for (LitVec::size_type i = 0; i != s.numLearntConstraints() && os_; ++i) {
		if ((c = s.getLearnt(i).clause()) != 0 && c->lbd() <= maxLbd && x.inSet(c->ClauseHead::type())) {
			lits.clear();
			c->toLits(lits);
			std::copy(lits.begin(), lits.end(), std::ostream_iterator<Literal>(os_, " "));
			os_ << "0\n";
		}
	}
	os_.flush();
}

} // end of namespace clasp

