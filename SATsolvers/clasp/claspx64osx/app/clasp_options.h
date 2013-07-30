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
#ifndef CLASP_CLASP_OPTIONS_H_INCLUDED
#define CLASP_CLASP_OPTIONS_H_INCLUDED

#ifdef _MSC_VER
#pragma warning (disable : 4200) // nonstandard extension used : zero-sized array
#pragma once
#endif

#include <string>
#include <utility>
#include <program_opts/app_options.h>
#include <clasp/clasp_facade.h>
#include <clasp/solver.h>
#include <iosfwd>

namespace ProgramOptions {
struct StringSlice;
// Specialized function for mapping unsigned integers
// Parses numbers >= 0, -1, and the string umax
StringSlice parseValue(const StringSlice& in, uint32& x, int extra);
}
namespace Clasp {

/////////////////////////////////////////////////////////////////////////////////////////
// Option groups - Mapping between command-line options and libclasp objects
/////////////////////////////////////////////////////////////////////////////////////////
// Function for mapping positional options
bool parsePositional(const std::string& s, std::string& out);
struct StringToEnum {
	const char* str; // string value
	int         ev;  // corresponding enum value
};
typedef StringToEnum EnumMap;

// Group "Clasp - General Options"
// Options of this group are mapped to ClaspConfig::api
// and ClaspConfig::enumerate
struct GeneralOptions {
	explicit GeneralOptions(ClaspConfig* c = 0) : config(c), defConfig(0) {}
	static bool mapOptVal(GeneralOptions*, const std::string&, const std::string&);
	static bool mapSatPre(GeneralOptions*, const std::string&, const std::string&);
	static bool mapLearnExplicit(GeneralOptions*, const std::string&, const std::string&);
	static bool mapDefaultConfig(GeneralOptions*, const std::string&, const std::string&);
	static bool parseSolveLimit(const std::string& s, SolveLimits& limit);
	void initOptions(ProgramOptions::OptionContext& root);
	bool validateOptions(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& parsed, ProgramOptions::Messages&);
	ClaspConfig*      config;
	const char*       defConfig;
	static const EnumMap enumModes[];
	static const EnumMap extRules[];
};

// Groups "Clasp - Search Options" and "Clasp - Lookback Options"
// Options of these groups are mapped to ClaspConfig::solve 
// and ClaspConfig::solver
struct SearchOptions {
	explicit SearchOptions(SolverConfig* l);
	void initOptions(ProgramOptions::OptionContext& root);
	void initOptions(const ProgramOptions::OptionContext& root, ProgramOptions::OptionContext& local, SolverConfig* x, uint32 delOpts = 1);
	bool validateOptions(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& parsed, ProgramOptions::Messages&);
	// value parsing 
	static bool parseSchedule(const std::string& s, ScheduleStrategy& sched, bool allowNo);
	// value mapping
	static bool mapSolverOpts(SearchOptions*, const std::string& opt, const std::string& v);
	static bool mapHeuOpts(SearchOptions*, const std::string& opt, const std::string& v);
	static bool mapRestart(SearchOptions*   , const std::string& opt, const std::string& v);
	static bool mapRandOpts(SearchOptions*  , const std::string& opt, const std::string& v);
	static bool mapReduceOpts(SearchOptions*, const std::string& opt, const std::string& v);
	
	SolverConfig*      local;
	SolverStrategies*  solverOpts() const;
	uint32             enabledDel;
	static const EnumMap heuTypes[];
	static const EnumMap lookTypes[];
	static const EnumMap loopTypes[];
	static const EnumMap delAlgos[];
};
	
#define CLASP_DEFAULT_PORTFOLIO_SIZE 13
extern const char* portfolio_g;
extern const char* defConfigs_g;
struct OptionConfig {
	explicit OptionConfig(const char* raw) { initFromRaw(raw); }
	void initFromRaw(const char* r);
	std::string name;
	std::string cmdLine;
};
// Combines all groups and drives initialization/validation 
// of command-line options.
class ClaspOptions {
public:
	ClaspOptions() : genTemplate(false), satPreDefault(true), config(0) {}
	std::string  portfolio;
	bool         genTemplate;
	bool         satPreDefault;
	void         initOptions(ProgramOptions::OptionContext& root, ClaspConfig& c);
	bool         validateOptions(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& parsed, ProgramOptions::Messages&);
	void         applyDefaults(Input::Format f);
	const char*  getInputDefaults(Input::Format f) const;
	static bool  mapSolveOpts(ClaspOptions*, const std::string& k, const std::string& v);
private:
	const char*  getPortfolio(std::string& mem) const;
	bool applyDefaultConfig(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& parsed, ProgramOptions::Messages&);
	bool applySearchConfig(const ProgramOptions::OptionContext& ctx, const OptionConfig& c, const ProgramOptions::ParsedOptions&, ProgramOptions::Messages&);
	bool populateThreadConfigs(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& parsed, ProgramOptions::Messages&);
	void initThreadOptions(ProgramOptions::OptionContext& root);
	void removeConfig(OptionConfig& cfg, const char* opt) const;
	ClaspConfig*                   config;
	std::auto_ptr<GeneralOptions>  mode;
	std::auto_ptr<SearchOptions>   search;
};
}
#endif

