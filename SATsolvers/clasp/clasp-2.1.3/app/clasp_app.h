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
#ifndef CLASP_CLASP_APP_H_INCLUDED
#define CLASP_CLASP_APP_H_INCLUDED

#ifdef _MSC_VER
#pragma warning (disable : 4200) // nonstandard extension used : zero-sized array
#pragma once
#endif
#include "clasp_options.h"
#include "clasp_output.h"
#include <program_opts/typed_value.h>
#include <clasp/util/timer.h>
#include <string>
#include <vector>
#include <iosfwd>
#include <memory>
#include <stdio.h>
#include <signal.h>
/////////////////////////////////////////////////////////////////////////////////////////
// Output macros and app exit codes
/////////////////////////////////////////////////////////////////////////////////////////
#define WRITE_STDERR(type,sys,msg) ( fflush(stdout), fprintf(stderr, "*** %-5s: (%s): %s\n", (type),(sys),(msg)), fflush(stderr) )
#define ERROR_OUT(sys,msg)   WRITE_STDERR("ERROR", (sys), (msg))
#define INFO_OUT(sys,msg)    WRITE_STDERR("Info", (sys), (msg))
#define WARNING_OUT(sys,msg) WRITE_STDERR("Warn", (sys), (msg))
/////////////////////////////////////////////////////////////////////////////////////////
// Clasp::Application
/////////////////////////////////////////////////////////////////////////////////////////
namespace Clasp {
class WriteLemmas {
public:
	WriteLemmas(std::ostream& os);
	~WriteLemmas();
	void attach(SharedContext& ctx);
	void detach();
	void flush(Constraint_t::Set types, uint32 maxLbd);
	bool unary(Literal, Literal) const;
	bool binary(Literal, Literal, Literal) const;
private:
	SharedContext* ctx_;
	std::ostream&  os_;
	mutable uint32 outShort_;
};
/////////////////////////////////////////////////////////////////////////////////////////
// Clasp specific app options
/////////////////////////////////////////////////////////////////////////////////////////
struct ClaspAppOptions {
	ClaspAppOptions();
	typedef std::pair<uint32, uint32> QPair;
	typedef ProgramOptions::StringSeq StringSeq;
	static bool mappedOpts(ClaspAppOptions*, const std::string&, const std::string&);
	void initOptions(ProgramOptions::OptionContext& root);
	bool validateOptions(const ProgramOptions::ParsedOptions& parsed, ProgramOptions::Messages&);
	StringSeq   input;     // list of input files - only first used!
	std::string lemmaOut;  // optional file name for writing learnt lemmas
	std::string lemmaIn;   // optional file name for reading learnt lemmas
	QPair       quiet;     // configure printing of models and optimization values
	uint32      verbose;   // verbosity level
	uint32      timeout;   // timeout in seconds (default: none=-1)
	uint32      stats;     // print statistics
	uint32      outf;      // output format
	char        ifs;       // output field separator
	uint8       outLbd;    // optional lbd limit for lemma out
	uint8       inLbd;     // optional lbd for lemma in
	bool        fastExit;  // force fast exit (no dtors)
	enum OutputFormat { out_def = 0, out_comp = 1, out_json = 2 };
};
class Application : public ProgramOptions::AppOptions, public ClaspFacade::Callback {
public:
	static Application& instance();    // returns the singleton instance
	static void sigHandler(int sig);   // signal/timeout handler
	void   installSigHandlers();       // adds handlers for SIGINT, SIGTERM, SIGALRM
	int    run(int argc, char** argv); // "entry-point"
	void   printTemplate()const;
	void   printWarnings()const;
private:
	Application();
	Application(const Application&);
	const Application& operator=(const Application&);
	// -------------------------------------------------------------------------------------------
	// AppOptions interface
	void    printHelp(const ProgramOptions::OptionContext& root)    ;
	void    printVersion(const ProgramOptions::OptionContext& root) ;
	HelpOpt initHelpOption() const;
	void    initOptions(ProgramOptions::OptionContext& root) {
		claspre_.initOptions(root);
		clasp_.initOptions(root, config_);
		app_.initOptions(root);
	}
	bool    validateOptions(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& vm, ProgramOptions::Messages& m) {
		return app_.validateOptions(vm, m) && claspre_.validateOptions(vm) && clasp_.validateOptions(root, vm, m);
	}
	// -------------------------------------------------------------------------------------------
	// ClaspFacade::Callback interface
	void state(ClaspFacade::Event e, ClaspFacade& f);
	void event(const Solver& s, ClaspFacade::Event e, ClaspFacade& f);
	void warning(const char* msg) { messages.warning.push_back(msg); }
	void reportRestart(const Solver&, uint64, uint32);
	// -------------------------------------------------------------------------------------------
	std::istream& getStream();
	void killAlarm();
	void kill(int sig);
	int  blockSignals();
	void unblockSignals(bool deliverPending);
	void readLemmas();
	int  exception(int status, const char* what);
	void appTerminate(int exitCode) const;
	void printDefaultConfigs() const;
	// -------------------------------------------------------------------------------------------  
	// Status information & output
	void configureOutput(Input::Format f);
	void model(const Solver& s, const Enumerator& e, bool cons);
	int  printResult(int sig);
	// -------------------------------------------------------------------------------------------  
	typedef PodVector<const SolveStats*>::type StatsVec;
	ClaspConfig                   config_;
	ClaspAppOptions               app_;
	ClaspOptions                  clasp_;
	Claspre::Options              claspre_;
	Timer<ProcessTime>            cpuTotalTime_;
	Timer<RealTime>               timer_[ClaspFacade::num_states]; // one for each state
	double                        timeToFirst_;                    // time to first model
	double                        timeToLast_;                     // time to last model
	StatsVec                      stats_;
	ClaspFacade*                  facade_;
	std::auto_ptr<WriteLemmas>    writeLemmas_;
	std::auto_ptr<OutputFormat>   out_;
	volatile sig_atomic_t         blocked_;
	volatile sig_atomic_t         pending_;
};

const char* const clasp_app_banner = "clasp version " CLASP_VERSION;

}
#endif
