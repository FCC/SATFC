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
#include "clasp_options.h"
#include "program_opts/typed_value.h"  
#include "program_opts/composite_value_parser.h"  // pair and vector
#include <clasp/satelite.h>
#include <sstream>
#include <fstream>
#include <algorithm>
#include <cstring>
#include <cctype>
using namespace ProgramOptions;
using namespace std;
namespace ProgramOptions {
// Specialized function for mapping unsigned integers
// Parses numbers >= 0, -1, and the string umax
StringSlice parseValue(const StringSlice& in, uint32& x, int extra) {
	int t           = 0;
	StringSlice ret = parseValue(in, t, extra);
	if (ret.ok() && t >= -1) { x = static_cast<uint32>(t); return ret; }
	size_t p = 0;
	if      (in.size() >= 2 && std::strncmp(in.data(), "-1", 2) == 0)   { x = uint32(-1); p = 2; }
	else if (in.size() >= 4 && std::strncmp(in.data(), "umax", 4) == 0) { x = uint32(-1); p = 4; }
	ret = in.parsed(p > 0, p);
	return ret.ok () && (extra != 0 || ret.complete()) ? ret : in.parsed(false);
}
}
namespace Clasp { 
/////////////////////////////////////////////////////////////////////////////////////////
// Parseing & Mapping of options
/////////////////////////////////////////////////////////////////////////////////////////
#define SET(x, v)           ( ((x)=(v)) == (v) )
#define SET_M(x, v, m)      ( ((v)<=(m)) && ((x)=(v)) == (v) )
#define SET_R(x, v, lo, hi) ( ((lo)<=(v)) && ((v)<=(hi)) && ((x)=(v)) == (v) )
template <class T, class U>
inline T clamp_max(U v)     { return v <= static_cast<T>(-1) ? static_cast<T>(v) : static_cast<T>(-1); }
template <class T, class U>
inline void set_clamp_max(T& x, U v){ x = clamp_max<T>(v); }

// a little helper to make parsing more comfortable
template <class T>
inline bool parse(const std::string& str, T& out) {
	return ProgramOptions::DefaultParser<T>::parse(str, out);
}

bool isFlagNo(const std::string& x) {
	if (x == "no") { return true; }
	const ProgramOptions::FlagStr* t = ProgramOptions::FlagStr::find(x);
	return t && t->val == false;
}

// maps positional options to options number or file
bool parsePositional(const std::string& t, std::string& out) {
	int num;
	if   (parse(t, num)) { out = "number"; }
	else                 { out = "file";   }
	return true;
}

inline ProgramOptions::StringSlice skip(const ProgramOptions::StringSlice& in, char c) {
	if (!in.ok() || in.complete() || in.data()[0] != c) { return in; }
	return in.parsed(true, 1);
}

template <class T>
inline bool match(ProgramOptions::StringSlice& in, T& out, bool skipSep = true) {
	if (skipSep) { in = skip(in, ','); }
	return (in = parseValue(in, out, 1)).ok();
}

// overload for parsing a clasp schedule - needed
// to make parsing of schedule composable, e.g. in a pair 
ProgramOptions::StringSlice parseValue(const ProgramOptions::StringSlice& in, ScheduleStrategy& sched, int E) {
	typedef ScheduleStrategy::Type SchedType;
	std::string type;
	StringSlice next(in);
	if (match(next, type, false)) {
		uint32 base    = 0, add, limit = 1;
		double arg     = 0;
		bool ok        = false;
		type           = toLower(type);
		SchedType st   = ScheduleStrategy::geometric_schedule;
		if      (!match(next, base)|| base == 0){ return in.parsed(false); }
		else if (type == "f" || type == "fixed"){ st = ScheduleStrategy::arithmetic_schedule; ok = true; limit = 0; }
		else if (type == "l" || type == "luby") { st = ScheduleStrategy::luby_schedule;       ok = true; }
		else if (type == "+" || type == "add")  { st = ScheduleStrategy::arithmetic_schedule; ok = match(next, add); arg = add; }
		else if (type == "x" || type == "*")    { st = ScheduleStrategy::geometric_schedule;  ok = match(next, arg) && arg >= 1.0; }
		else if (type == "d" || type == "D")    { st = ScheduleStrategy::user_schedule;       ok = match(next, arg) && arg > 0.0; }
		if (!ok)        { return in.parsed(false); }
		if (limit == 1) { limit = 0; next = !next.complete() && next.data()[0] == ',' ? parseValue(next.parsed(true,1), limit, E) : next; }
		if (E == 0 && !next.complete()) { return in.parsed(false); }
		sched = ScheduleStrategy(st, base, arg, limit);
		return next;
	}
	return in.parsed(false);
}
// maps value to the corresponding enum-constant
// theMap must be null-terminated!
bool mapEnumImpl(const EnumMap* theMap, const std::string& value, int& out) {
	std::string temp(toLower(value));
	for (int i = 0; theMap[i].str; ++i) {
		if (temp == theMap[i].str) { 
			out = theMap[i].ev; 
			return true; 
		}
	}
	return false;
}
// statically binds theMap so that the resulting function can be
// used as parser in the ProgramOption-library
template <const EnumMap* theMap, class EnumT>
bool mapEnum(const std::string& value, EnumT& out) {
	int temp;
	return mapEnumImpl(theMap, value, temp) && (out = (EnumT)temp, true);
}
/////////////////////////////////////////////////////////////////////////////////////////
// Clasp specific mode options
/////////////////////////////////////////////////////////////////////////////////////////
void GeneralOptions::initOptions(ProgramOptions::OptionContext& root) {
	GlobalOptions* global = config;
	OptionGroup general("Clasp - General Options");
	general.addOptions()
		("configuration", notify(this, &GeneralOptions::mapDefaultConfig)->defaultsTo("frumpy"),
		 "Configure default configuration [%D]\n"
		 "      %A: {frumpy|jumpy|handy|crafty|trendy"
#if WITH_THREADS
		 "|chatty"
#endif
		 "}\n        frumpy: Use conservative defaults\n"
		 "        jumpy : Use aggressive defaults\n"
		 "        handy : Use defaults geared towards large problems\n"
		 "        crafty: Use defaults geared towards crafted problems\n"
		 "        trendy: Use defaults geared towards industrial problems\n"
#if WITH_THREADS
		 "        chatty: Use 4 competing threads initialized via the default portfolio\n"
#endif
		 )

		("solve-limit", storeTo(global->solve.limit, &GeneralOptions::parseSolveLimit)->arg("<n>[,<m>]"), "Stop search after <n> conflicts or <m> restarts\n")
		
		("enum-mode,e", storeTo(global->enumerate.mode, &mapEnum<enumModes>)->defaultsTo("auto")->state(Value::value_defaulted), 
		 "Configure enumeration algorithm [%D]\n"
		 "      %A: {bt|record|brave|cautious|auto}\n"
		 "        bt      : Backtrack decision literals from solutions\n"
		 "        record  : Add nogoods for computed solutions\n"
		 "        brave   : Compute brave consequences (union of models)\n"
		 "        cautious: Compute cautious consequences (intersection of models)\n"
		 "        auto    : Use bt for enumeration and record for optimization")
		("number,n", storeTo(global->enumerate.numModels)->arg("<n>"), "Compute at most %A models (0 for all)")
		("restart-on-model"  , flag(global->enumerate.restartOnModel), "Restart after each model")
		("project"           , flag(global->enumerate.project)       , "Project models to named atoms in enumeration mode\n")
		("project-opt,@2", storeTo(global->enumerate.projectOpts), "Additional options for projection as octal digit")
		
		("opt-ignore"        , flag(global->opt.no) ,                  "Ignore optimize statements")
		("opt-sat"           , flag(global->enumerate.maxSat), "Treat DIMACS input as MaxSAT optimization problem")
		("opt-hierarch"      , storeTo(global->opt.hierarch)->arg("{0..3}")->implicit("1"), 
		 "Process optimize statements in order of priority\n"
		 "    For each criterion use:\n"
		 "      1: fixed step size of one\n"
		 "      2: exponentially increasing step sizes\n"
		 "      3: exponentially decreasing step sizes")
		("opt-all"           , notify(this, &GeneralOptions::mapOptVal)->arg("<opt>..."), "Compute models <= %A")
		("opt-value"         , notify(this, &GeneralOptions::mapOptVal)->arg("<opt>..."), "Initialize objective function(s)\n")
		
		("sat-prepro,@1", notify(this, &GeneralOptions::mapSatPre)->implicit("-1"),
		 "Run SatELite-like preprocessing (Implicit: %I)\n"
		 "      %A: <n1>[,...][,<n5 {0..2}>] (-1=no limit)\n"
		 "        <n1>: Run for at most <n1> iterations\n"
		 "        <n2>: Run variable elimination with cutoff <n2>              [-1]\n"
		 "        <n3>: Run for at most <n3> seconds                           [-1]\n"
		 "        <n4>: Disable if <n4>%% of vars are frozen                    [-1]\n"
		 "        <n5>: Run blocked clause elimination  {0=no,1=limited,2=full} [1]")
		("learn-explicit,@1", notify(this, GeneralOptions::mapLearnExplicit)->flag(), "Do not use Short Implication Graph for learning")
	;
	OptionGroup asp("Clasp - ASP Options");
	asp.addOptions()
		("pre" , flag(global->enumerate.onlyPre), "Run ASP preprocessing and exit")
		("supp-models",flag(global->eq.noSCC), "Compute supported models (no unfounded set check)")
		("eq,@1", storeTo(global->eq.iters)->arg("<n>"), "Configure equivalence preprocessing\n"
		"      Run for at most %A iterations (-1=run to fixpoint)")
		("backprop!,@1",flag(global->eq.backprop), "Use backpropagation in ASP-preprocessing")
		("eq-dfs,@2"  , flag(global->eq.dfOrder)    , "Enable df-order in eq-preprocessing")
		("trans-ext!,@1", storeTo(global->eq.erMode, &mapEnum<extRules>),
		 "Configure handling of Lparse-like extended rules\n"
		 "      %A: {all|choice|card|weight|integ|dynamic}\n"
		 "        all    : Transform all extended rules to basic rules\n"
		 "        choice : Transform choice rules, but keep cardinality and weight rules\n"
		 "        card   : Transform cardinality rules, but keep choice and weight rules\n"
		 "        weight : Transform cardinality and weight rules, but keep choice rules\n"
		 "        integ  : Transform cardinality integrity constraints\n"
		 "        dynamic: Transform \"simple\" extended rules, but keep more complex ones")
	;
	root.add(general);
	root.add(asp);
}
const EnumMap GeneralOptions::extRules[] = {
	{"all", ProgramBuilder::mode_transform}, {"yes", ProgramBuilder::mode_transform},
	{"no", ProgramBuilder::mode_native}, {"0", ProgramBuilder::mode_native},
	{"choice", ProgramBuilder::mode_transform_choice}, {"card", ProgramBuilder::mode_transform_card},
	{"weight", ProgramBuilder::mode_transform_weight}, {"integ", ProgramBuilder::mode_transform_integ},
	{"dynamic", ProgramBuilder::mode_transform_dynamic}, {0, 0}
};
const EnumMap GeneralOptions::enumModes[]= {
	{"auto", GlobalOptions::enum_auto}, {"bt", GlobalOptions::enum_bt},
	{"record", GlobalOptions::enum_record}, {"brave", GlobalOptions::enum_brave},
	{"cautious", GlobalOptions::enum_cautious}, {0,0}
};

bool GeneralOptions::mapDefaultConfig(GeneralOptions* this_, const std::string&, const std::string& value) {
	const char* x = defConfigs_g;
	for (; *x && strncmp(x+1, value.c_str(), value.size()) != 0; x += strlen(x) + 1) { ; }
	return *(this_->defConfig = x) != 0;
}
bool GeneralOptions::mapLearnExplicit(GeneralOptions* this_, const std::string&, const std::string& value) {
	bool x, parsed;
	if ((parsed = FlagStr::store_true(value, x)) == true) { this_->config->ctx.learnImplicit(!x); }
	return parsed;
}
bool GeneralOptions::mapOptVal(GeneralOptions* this_, const std::string& n, const std::string& value) {
	GlobalOptions* global = this_->config;
	if (parseSequence<wsum_t>(StringSlice(value), std::back_inserter(global->opt.vals), -1, 0)) {
		if (n == "opt-all") { global->opt.all = true; }
		return true;
	}
	global->opt.vals.clear();
	return false;
}
bool GeneralOptions::mapSatPre(GeneralOptions* this_, const std::string&, const std::string& value) {
	GlobalOptions* global = this_->config;
	std::auto_ptr<SatElite::SatElite> pre(new SatElite::SatElite());
	uint32* pos = &pre->options.maxIters, *end = pos + (sizeof(pre->options)/sizeof(uint32)); 
	if (!parseSequence<uint32>(StringSlice(value), pos, uint32(end - pos), 0)) return false;
	if (pre->options.maxIters) { global->ctx.satPrepro.reset(pre.release()); }
	return true;
}

bool GeneralOptions::validateOptions(const ProgramOptions::OptionContext&, const ProgramOptions::ParsedOptions& vm, Messages& m) {
	if (config->eq.noSCC && vm.count("eq") == 0)  { config->eq.noEq(); }
	if (vm.count("opt-value") && config->opt.all) { m.error = "'opt-all' and 'opt-value' are mutually exclusive!"; }
	return m.error.empty();
}
bool GeneralOptions::parseSolveLimit(const std::string& s, SolveLimits& limit) {
	std::pair<uint32, uint32> p(-1, -1);
	if (s != "no" && !parse(s, p)) { return false; }
	limit = SolveLimits(p.first, p.second);
	if (limit.conflicts == UINT32_MAX) { limit.conflicts = UINT64_MAX; }
	if (limit.restarts  == UINT32_MAX) { limit.restarts  = UINT64_MAX; }
	return true;
}
/////////////////////////////////////////////////////////////////////////////////////////
// Clasp specific search options
/////////////////////////////////////////////////////////////////////////////////////////
const std::string searchGroup   = "Clasp - Search Options";
const std::string lookbackGroup = "Clasp - Lookback Options";
SearchOptions::SearchOptions(SolverConfig* o) : local(o), enabledDel(1) {}
SolverStrategies* SearchOptions::solverOpts() const { return &local->solver->strategies(); }

void SearchOptions::initOptions(ProgramOptions::OptionContext& root) {
	// NOTE: DO NOT bind locations to options (e.g. via. storeTo/flag) here
	// but instead only use indirect (notify) values. This way,
	// when parsing a portfolio, only the underlying solver configuration 
	// (one pointer) has to change, while the context holding the 
	// option-instances can remain the same.
	OptionGroup search(searchGroup, ProgramOptions::desc_level_e1);
	search.addOptions()
		("heuristic", notify(this, &SearchOptions::mapHeuOpts), 
		 "Configure decision heuristic\n"
		 "      %A: {Berkmin|Vmtf|Vsids|Unit|None}\n"
		 "        Berkmin: Apply BerkMin-like heuristic\n"
		 "        Vmtf   : Apply Siege-like heuristic\n"
		 "        Vsids  : Apply Chaff-like heuristic\n"
		 "        Unit   : Apply Smodels-like heuristic (Default if --no-lookback)\n"
		 "        None   : Select the first free variable")
		("init-moms!,@2", notify(this, &SearchOptions::mapHeuOpts)->flag(), "Initialize heuristic with MOMS-score")
		("score-other", notify(this, &SearchOptions::mapHeuOpts)->arg("<n>"), "Score {0=no|1=loop|2=all} other learnt nogoods")
		("sign-def", notify(this, &SearchOptions::mapHeuOpts)->arg("<n>"), "Default sign: {0=type|1=no|2=yes|3=rnd}")
		("sign-fix!", notify(this, &SearchOptions::mapHeuOpts)->flag(), "Disable sign heuristics and use default signs only")
		("berk-max,@2", notify(this, &SearchOptions::mapHeuOpts)->arg("<n>"), "Consider at most %A nogoods in Berkmin heuristic")
		("berk-huang!,@2",notify(this, &SearchOptions::mapHeuOpts)->flag(), "Enable/Disable Huang-scoring in Berkmin")
		("berk-once!,@2",notify(this, &SearchOptions::mapHeuOpts)->flag(), "Score sets (instead of multisets) in Berkmin")
		("vmtf-mtf,@2", notify(this, &SearchOptions::mapHeuOpts)->arg("<n>"), "In Vmtf move %A conflict-literals to the front")
		("vsids-decay,@2", notify(this, &SearchOptions::mapHeuOpts)->arg("<n>"), "In Vsids use 1.0/0.<n> as decay factor")
		("nant!,@2",notify(this, &SearchOptions::mapHeuOpts)->flag(), "In Unit count only atoms in NAnt(P)")
		("opt-heuristic" , notify(this, &SearchOptions::mapSolverOpts)->implicit("1")->arg("{0..3}"), 
		 "Use opt. in {1=sign|2=model|3=both} heuristics")
		("save-progress"   , notify(this, &SearchOptions::mapSolverOpts)->implicit("1")->arg("<n>"), "Use RSat-like progress saving on backjumps > %A")
		("rand-freq", notify(this, &SearchOptions::mapRandOpts)->arg("<p>"), "Make random decisions with probability %A")
		("init-watches", notify(this, &SearchOptions::mapSolverOpts)->arg("{0..2}")->defaultsTo("1"),
		 "Configure watched literal initialization [%D]\n"
		 "      Watch {0=first|1=random|2=least watched} literals in nogoods")
		("seed"    , notify(this, &SearchOptions::mapSolverOpts)->arg("<n>"),"Set random number generator's seed to %A\n")
		
		("lookahead!"     ,notify(this, &SearchOptions::mapSolverOpts)->implicit("atom"),
		 "Configure failed-literal detection (fld)\n"
		 "      %A: <type>[,<n {1..umax}>] / Implicit: %I\n"
		 "        <type>: Run fld via {atom|body|hybrid} lookahead\n"
		 "        <n>   : Disable fld after <n> applications ([-1]=no limit)\n"
		 "      --lookahead=atom is default if --no-lookback is used\n")

		("rand-prob!", notify(this, &SearchOptions::mapRandOpts)->implicit("10,100"),
		 "Configure random probing (Implicit: %I)\n"
		 "      %A: <n1>[,<n2>]\n"
		 "        Run <n1> random passes with at most <n2> conflicts each")
	;
	
	OptionGroup lookback(lookbackGroup, ProgramOptions::desc_level_e1);
	lookback.addOptions()
		("no-lookback"   , notify(this, &SearchOptions::mapSolverOpts)->flag(), "Disable all lookback strategies\n")
		
		("restarts!,r", notify(this, &SearchOptions::mapRestart)->arg("<sched>"),
		 "Configure restart policy\n"
		 "      %A: <type {D|F|L|x|+}>,<n {1..umax}>[,<args>][,<lim>]\n"
		 "        F,<n>    : Run fixed sequence of <n> conflicts\n"
		 "        L,<n>    : Run Luby et al.'s sequence with unit length <n>\n"
		 "        x,<n>,<f>: Run geometric seq. of <n>*(<f>^i) conflicts  (<f> >= 1.0)\n"
		 "        +,<n>,<m>: Run arithmetic seq. of <n>+(<m>*i) conflicts (<m {0..umax}>)\n"
		 "        ...,<lim>: Repeat seq. every <lim>+j restarts           (<type> != F)\n"
		 "        D,<n>,<f>: Restart based on moving LBD average over last <n> conflicts\n"
		 "                   Mavg(<n>,LBD)*<f> > avg(LBD)\n"
		 "                   use conflict level average if <lim> > 0 and avg(LBD) > <lim>\n"
		 "      no|0       : Disable restarts")
		("local-restarts"  , notify(this, &SearchOptions::mapRestart)->flag(), "Use Ryvchin et al.'s local restarts")
		("bounded-restarts", notify(this, &SearchOptions::mapRestart)->flag(), "Use (bounded) restarts during model enumeration")
		("counter-restarts", notify(this, &SearchOptions::mapRestart)->arg("<n>"), "Do a counter implication restart every <n> restarts")
		("counter-bump,@2" , notify(this, &SearchOptions::mapRestart)->arg("<n>"), "Set CIR bump factor to %A")
		("reset-restarts",   notify(this, &SearchOptions::mapRestart)->flag(), "Reset restart strategy during model enumeration")				
		("shuffle!", notify(this, &SearchOptions::mapRestart)->arg("<n1>,<n2>"), "Shuffle problem after <n1>+(<n2>*i) restarts\n")

		("deletion!,d", notify(this, &SearchOptions::mapReduceOpts)->defaultsTo("1,75,3.0")->state(Value::value_defaulted), 
		 "Configure deletion strategy [%D]\n"
		 "      %A: <s {1..3}>[,<n {1..100}>][,<f>]\n"
		 "        <s>: Enable {1=size-based|2=conflict-based|3=combined} strategy\n"
		 "        <n>: Delete at most <n>%% of nogoods on reduction       [75]\n"
		 "        <f>: Set initial limit to P=estimated problem size/<f> [3.0]\n"
		 "      no   : Disable nogood deletion")
		("del-init-r", notify(this, &SearchOptions::mapReduceOpts)->arg("<n>,<o>"), "Clamp initial limit to the range [<n>,<n>+<o>]")
		("del-estimate", notify(this, &SearchOptions::mapReduceOpts)->flag(), "Use estimated problem complexity in limits")
		("del-max",  notify(this, &SearchOptions::mapReduceOpts)->arg("<n>,<X>"), "Keep at most <n> learnt nogoods taking up to <X> MB")
		("del-grow", notify(this, &SearchOptions::mapReduceOpts), 
		 "Configure size-based deletion policy\n"
		 "      %A: <f>[,<g>][,<sched>] (<f> >= 1.0 / deletion.<s> in {1|3})\n"
		 "        <f>     : Keep at most T = X*(<f>^i) learnt nogoods with X being the\n"
		 "                  initial limit and i the number of times <sched> fired\n"
		 "        <g>     : Stop growth once T > P*<g> (0=no limit)      [3.0]\n"
		 "        <sched> : Set grow schedule (<type {F|L|x|+}>) [grow on restart]")
		("del-cfl", notify(this, &SearchOptions::mapReduceOpts)->arg("<sched>"), 
		 "Configure conflict-based deletion policy\n"
		 "      %A:   <t {F|L|x|+}>,<n {1..umax}>[,<args>][,<lim>] (see restarts)\n"
		 "      condition: deletion.<s> in {2|3}")
		("del-algo", notify(this, &SearchOptions::mapReduceOpts), 
		 "Configure nogood deletion algorithm\n"
		 "      %A: <algo>[,<sc {0..2}>]\n"
		 "        <algo>: Use {basic|sort|inp_sort|inp_heap} algorithm\n"
		 "        <sc>  : Use {0=activity|1=lbd|2=combined} nogood scores [0]")
		("del-glue", notify(this, &SearchOptions::mapReduceOpts), "Configure glue clause handling\n"
		 "      %A: <n {0..127}>[,<m {0|1}>]\n"
		 "        <n>: Do not delete nogoods with LBD <= <n>\n"
		 "        <m>: Count (0) or ignore (1) glue clauses in size limit [0]")
		("del-on-restart", notify(this, &SearchOptions::mapReduceOpts)->arg("<n>")->implicit("33"), "Delete %A%% of learnt nogoods on each restart\n")
	
		("strengthen!", notify(this, &SearchOptions::mapSolverOpts),
		 "Use MiniSAT-like conflict nogood strengthening\n"
		 "      %A: <mode>[,<type>]\n"
		 "        <mode>: Use {local|recursive} self-subsumption check\n"
		 "        <type>: Follow {0=all|1=short|2=binary} antecedents  [0]")
		("otfs",notify(this, &SearchOptions::mapSolverOpts)->implicit("1")->arg("{0..2}"), "Enable {1=partial|2=full} on-the-fly subsumption")
		("update-lbd", notify(this, &SearchOptions::mapSolverOpts)->implicit("1")->arg("{0..3}"), "Update LBDs of learnt nogoods {1=<|2=strict<|3=+1<}")
		("update-act,@2", notify(this, &SearchOptions::mapSolverOpts)->flag(), "Enable LBD-based activity bumping")
		("reverse-arcs",notify(this, &SearchOptions::mapSolverOpts)->implicit("1")->arg("{0..3}"), "Enable ManySAT-like inverse-arc learning")
		("contraction!", notify(this, &SearchOptions::mapSolverOpts)->arg("<n>"),
		 "Contract learnt nogoods of size > <n> (0=disable)\n")
		
		("loops", notify(this, &SearchOptions::mapSolverOpts),
			"Configure learning of loop nogoods\n"
			"      %A: {common|distinct|shared|no}\n"
			"        common  : Create loop nogoods for atoms in an unfounded set\n"
			"        distinct: Create distinct loop nogood for each atom in an unfounded set\n"
			"        shared  : Create loop formula for a whole unfounded set\n"
			"        no      : Do not learn loop formulas")
	;
	root.add(search);
	root.add(lookback);
}

void SearchOptions::initOptions(const ProgramOptions::OptionContext& global, ProgramOptions::OptionContext& out, SolverConfig* x, uint32 delOpts) {
	if (!out.tryFindGroup(searchGroup)) {
		const OptionGroup* s = global.tryFindGroup(searchGroup);
		const OptionGroup* l = global.tryFindGroup(lookbackGroup);
		if (s && l) { out.add(*s); out.add(*l); }
		else        { initOptions(out); }
	}
	this->local      = x;
	this->enabledDel = delOpts;
}
const EnumMap SearchOptions::heuTypes[] = {
	{"berkmin", ClaspConfig::heu_berkmin}, {"vmtf", ClaspConfig::heu_vmtf},
	{"vsids", ClaspConfig::heu_vsids}, {"unit", ClaspConfig::heu_unit},
	{"none", ClaspConfig::heu_none}, {0,0}
};
const EnumMap SearchOptions::loopTypes[] = {
	{"common", DefaultUnfoundedCheck::common_reason}, {"shared", DefaultUnfoundedCheck::shared_reason},
	{"distinct", DefaultUnfoundedCheck::distinct_reason}, {"no", DefaultUnfoundedCheck::only_reason},
	{0,0}
};
const EnumMap SearchOptions::lookTypes[] = {
	{"atom", Lookahead::atom_lookahead}, {"body", Lookahead::body_lookahead}, {"hybrid", Lookahead::hybrid_lookahead},
	{0,0}
};
const EnumMap SearchOptions::delAlgos[] = {
	{"basic", ReduceStrategy::reduce_linear}, {"sort", ReduceStrategy::reduce_stable},
	{"inp_sort", ReduceStrategy::reduce_sort}, {"inp_heap", ReduceStrategy::reduce_heap}, 
	{0,0}
};

bool SearchOptions::mapRandOpts(SearchOptions* this_, const std::string& opt, const std::string& value) {
	StringSlice x = isFlagNo(value) ? StringSlice("0", 1) : StringSlice(value);
	std::pair<uint32, uint32> p(0,100); double f = 0.0;
	return (opt == "rand-freq" && parseValue(x,f,0) && this_->local->params.setRandomProbability(f))
		  || (opt == "rand-prob" && parseValue(x,p,0) && this_->local->params.setRandomizeParams(p.first,p.second));
}

bool SearchOptions::parseSchedule(const std::string& s, ScheduleStrategy& sched, bool allowNo) {
	if (allowNo && isFlagNo(s)) { sched = ScheduleStrategy::none(); return true; }
	return parseValue(StringSlice(s), sched, 0).ok();
}

bool SearchOptions::mapRestart(SearchOptions* this_, const std::string& n, const std::string& value) {
	RestartParams& p = this_->local->params.restart;
	bool ok = false, b;
	if      (n == "restarts")        { ok = parseSchedule(value, p.sched, true) && SET(p.dynRestart, uint32(p.sched.type == ScheduleStrategy::user_schedule)); }
	else if (n == "local-restarts")  { ok = FlagStr::store_true(value, b) && SET(p.cntLocal, uint32(b));   }
	else if (n == "counter-restarts" || n == "counter-bump") {
		uint32 i;
		if ( (ok=parse(value, i)) == true ) { set_clamp_max(n[8] == 'r' ? p.counterRestart : p.counterBump, i); }
	}
	else if (n == "bounded-restarts" || n == "reset-restarts") {
		ok = FlagStr::store_true(value, b);
		if (n[0] == 'b') { p.bndRestart = b && ok; }
		else             { p.rstRestart = b && ok; }
	}
	else if (n == "shuffle") {
		std::pair<uint32, uint32> x(0,0);
		ok = isFlagNo(value) || parse(value, x);
		const uint32 mV= (uint32(1)<<14)-1;
		p.shuffle      = std::min(x.first, mV);
		p.shuffleNext  = std::min(x.second, mV);
	}
	return ok;
}

bool SearchOptions::mapReduceOpts(SearchOptions* this_, const std::string& n, const std::string& value) {
	ReduceParams& p = this_->local->params.reduce;
	bool ok = false;
	if (n == "deletion") { // <s>[,<n>][,<f>]
		if (isFlagNo(value)) { p.disable(); this_->enabledDel = 0; return true; }
		std::pair<uint32, std::pair<uint32, double> > arg(0, std::make_pair(75, 3.0)); 
		ok = parse(value, arg) && SET_M(this_->enabledDel, arg.first, 3) && SET_R(p.strategy.fReduce, arg.second.first, 1, 100) 
			&& arg.second.second > 0 && (p.fInit = float(1.0 / arg.second.second)) > 0;
	}
	else if (n == "del-init-r") { // <b>,<o>
		std::pair<uint32, uint32> arg(p.initRange.lo, p.initRange.hi);
		ok = parse(value, arg) && SET(p.initRange.lo, arg.first) && SET(p.initRange.hi, clamp_max<uint32>(uint64(arg.first)+arg.second));
	}
	else if (n == "del-grow") { // <g>[,<h>][,<sched>]
		std::pair<std::pair<double, double>, ScheduleStrategy> arg(std::make_pair(1.0, 3.0), p.growSched);
		if (parse(value, arg) && arg.first.first >= 1.0 && arg.first.second >= 0.0) {
			p.fGrow     = (float)arg.first.first;  
			p.fMax      = (float)arg.first.second;
			p.growSched = arg.second; 
			ok          = true;
		}
	}
	else if (n == "del-algo") {
		std::pair<std::string, uint32> algo("", (uint32)p.strategy.score);
		int a;
		ok = parse(value, algo) && mapEnumImpl(delAlgos, algo.first, a) 
		  && SET_M(p.strategy.score, algo.second, 2)
			&& SET(p.strategy.algo, (uint32)a);
	}
	else if (n == "del-glue"){
		std::pair<uint32, uint32> x(0, 0);
		ok = parse(value, x) && SET_M(p.strategy.glue, x.first, (uint32)Activity::MAX_LBD)
		  && SET(p.strategy.noGlue, x.second);
	}
	else if (n == "del-max")       { 
		std::pair<uint32, uint32> x(0,0); 
		ok = parse(value, x) && x.first > 0 && SET(p.maxRange, x.first);
		if (ok && x.second != 0) { this_->local->solver->setMemLimit(x.second); } 
	}
	else if (n == "del-on-restart"){ uint32 x; ok = parse(value, x) && SET_M(p.strategy.fRestart, x, 100); }
	else if (n == "del-estimate")  { bool x; ok = FlagStr::store_true(value, x); p.strategy.estimate = (uint32)x; }
	else if (n == "del-cfl")       { ok = parseSchedule(value, p.cflSched, false); }
	return ok;
}


bool SearchOptions::mapSolverOpts(SearchOptions* this_, const std::string& n, const std::string& v) {
	SolverStrategies* opts = this_->solverOpts();
	uint32 x; bool b;
	if      (n == "init-watches")  { return parse(v, x) && SET_M(opts->initWatches, x, 2);}
	else if (n == "save-progress") { return parse(v, x) && (SET(opts->saveProgress, x) || x == UINT32_MAX); }
	else if (n == "opt-heuristic") { return parse(v, x) && SET(opts->optHeu, x);      }
	else if (n == "otfs")          { return parse(v, x) && SET(opts->otfs, x);         }
	else if (n == "reverse-arcs")  { return parse(v, x) && SET(opts->reverseArcs, x);  }
	else if (n == "update-lbd")    { return parse(v, x) && SET(opts->updateLbd, x);    }
	else if (n == "update-act")    { return FlagStr::store_true(v, b) && SET(opts->bumpVarAct, (uint32)b); }
	else if (n == "no-lookback")   { return FlagStr::store_true(v, b) && SET(opts->search, (uint32)b); }
	else if (n == "contraction")   { return ((x = (v!="no")) == 0 || parse(v, x)) && SET(opts->compress, x); }
	else if (n == "seed")          { b = parse(v, x); opts->rng.srand(x); return b; }
	else if (n == "loops")         { int i; return mapEnumImpl(loopTypes, v, i) && SET(opts->loopRep, (uint32)i); }
	else if (n == "strengthen")    {
		std::pair<std::string, uint32> x("local", SolverStrategies::no_antes); b = false;
		if      (isFlagNo(v))                    { opts->ccMinAntes = 0; opts->strRecursive = 0; return true; }
		else if (!parse(v, x))                   { return false; }
		else if (x.first == "recursive")         { b = true; }
		else if (x.first != "local")             { return false; }
		return SET(opts->ccMinAntes, x.second+1) && SET(opts->strRecursive, (uint32)b);
	}
	else if (n == "lookahead")     {
		std::pair<std::string, uint32> x("",-1); 
		int i = Lookahead::no_lookahead;
		if (!isFlagNo(v) && (!parse(v, x) || !mapEnumImpl(lookTypes, x.first, i) || x.second == 0)) { return false; }
		this_->local->params.init.lookType = static_cast<ClaspConfig::LookaheadType>(i);
		this_->local->params.init.lookOps  = static_cast<uint16>(x.second == UINT32_MAX ? 0 : std::min(x.second, UINT32_MAX>>16));
		return true;
	}
	return false;
}

bool SearchOptions::mapHeuOpts(SearchOptions* this_, const std::string& n, const std::string& v) {
	SolverStrategies& opts = *this_->solverOpts();
	int i; uint32 x; bool b;
	if      (n == "heuristic")   { return mapEnumImpl(heuTypes, v, i) && SET(opts.heuId, (uint32)i); }
	else if (n == "berk-max")    { return parse(v, x) && (SET(opts.heuParam,x) || x == UINT32_MAX);}
	else if (n == "vmtf-mtf")    { return parse(v, x) && (SET(opts.heuParam,x) || x == UINT32_MAX);}
	else if (n == "vsids-decay") { return parse(v, x) && (SET(opts.heuParam,x) || x == UINT32_MAX);}
	else if (n == "sign-def")    { return parse(v, x) && SET(opts.signDef, x); }
	else if (n == "score-other") { return parse(v, x) && SET_M(opts.heuOther,x,2);}
	else if (n == "sign-fix")    { return FlagStr::store_true(v, b) && SET(opts.signFix, (uint32)b); }
	else if (n == "nant")        { return FlagStr::store_true(v, b) && SET(opts.unitNant, (uint32)b); }
	else if (n == "berk-once")   { return FlagStr::store_true(v, b) && SET(opts.berkOnce, (uint32)b); }
	else if (n == "berk-huang")  { return FlagStr::store_true(v, b) && SET(opts.berkHuang, (uint32)b); }
	else if (n == "init-moms")   { return FlagStr::store_true(v, b) && SET(opts.heuMoms, (uint32)b); }
	return false;
}

bool SearchOptions::validateOptions(const ProgramOptions::OptionContext&, const ProgramOptions::ParsedOptions& vm, Messages& m) {
	if (solverOpts()->search == Clasp::SolverStrategies::no_learning) {	
		if (vm.count("heuristic") == 0) { solverOpts()->heuId         = ClaspConfig::heu_unit; }
		if (vm.count("lookahead") == 0) { local->params.init.lookType = Lookahead::atom_lookahead;  }
		const RestartParams& rs   = local->params.restart;
		const ReduceParams&  rd   = local->params.reduce;
		bool warnRs = rs.local() || rs.dynamic() || rs.bounded() || rs.shuffle || rs.counterRestart || vm.count("restarts");
		bool warnRd = !rd.cflSched.disabled() || !rd.growSched.disabled() || vm.count("deletion");
		if (warnRs || warnRd || vm.count("strengthen") || vm.count("otfs") || vm.count("reverse-arcs")) {
			m.warning.push_back("'no-lookback': lookback options are ignored!");
		}
	}
	ReduceParams& p = local->params.reduce;
	if (enabledDel != 3) {
		uint32 grow = vm.count("del-grow") != 0;
		uint32 cfl  = (vm.count("del-cfl") != 0) * 2;
		if ((enabledDel & grow) != grow) { m.error = "'--del-grow': strategy not enabled by 'deletion'!"; }
		if ((enabledDel & cfl)  != cfl)  { m.error = "'--del-cfl': strategy not enabled by 'deletion'!"; }
		if (enabledDel == 2 && p.cflSched.disabled()) { m.error = "'deletion=2' requires '--del-cfl'!"; }
		if ((enabledDel & 1) == 0)       { p.growSched = ScheduleStrategy::none(); p.fGrow = 1.0f; p.fMax = 0; }
		if ((enabledDel & 2) == 0)       { p.cflSched  = ScheduleStrategy::none(); }
	}
	if (!p.cflSched.disabled() && p.cflSched.type == ScheduleStrategy::user_schedule) {
		m.error = "'--del-cfl': dynamic schedule 'D' not supported!";
	}
	else if (!p.growSched.disabled() && p.growSched.type == ScheduleStrategy::user_schedule) {
		m.error = "'--del-grow': dynamic schedule 'D' not supported!";
	}
	return m.error.empty();
}

/////////////////////////////////////////////////////////////////////////////////////////
// clasp option validation
/////////////////////////////////////////////////////////////////////////////////////////
void OptionConfig::initFromRaw(const char* raw) {
	assert(raw);
	while (std::isspace(static_cast<unsigned char>(*raw))) { ++raw; }
	const char* n = raw+1;
	const char* e = strchr(raw, ']');
	if (*raw == '[' && e != 0 && e[1] == ':') {
		name.assign(n, e);
		for (e += 2; std::isspace(static_cast<unsigned char>(*e)); ++e) { ; }
		cmdLine = e;
	}
	else { 
		std::string err("Invalid configuration: '");
		uint32 len = strlen(raw);
		err.append(raw, raw + std::min(len, uint32(20)));
		err += "'";
		throw std::logic_error(err);
	}
}
void ClaspOptions::initOptions(ProgramOptions::OptionContext& root, ClaspConfig& config) {
	satPreDefault = true;
	genTemplate   = false;
	this->config  = &config;
	initThreadOptions(root);
	mode.reset(new GeneralOptions(&config));
	search.reset(new SearchOptions(config.master()));
	mode->initOptions(root);
	search->initOptions(root);
}

bool ClaspOptions::validateOptions(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& vm, Messages& m) {
	bool ok = applyDefaultConfig(root, vm, m) && populateThreadConfigs(root, vm, m);
	mode.reset(0);
	search.reset(0);
	return ok && config->validate(m.error);
}

void ClaspOptions::applyDefaults(Input::Format f) {
	if (f != Input::SMODELS && satPreDefault) {
		SatElite::SatElite* pre = new SatElite::SatElite();
		pre->options.maxIters = 20;
		pre->options.maxOcc   = 25;
		pre->options.maxTime  = 120;
		config->ctx.satPrepro.reset(pre);		
	}
}
const char* ClaspOptions::getInputDefaults(Input::Format f) const {
	if (f == Input::SMODELS) { return "--eq=5"; }
	else                     { return "--sat-prepro=20,25,120"; }
}
bool ClaspOptions::applyDefaultConfig(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& vm, ProgramOptions::Messages& m) { 
	// check if command-line is valid
	if (!mode->validateOptions(root, vm, m) || !search->validateOptions(root, vm, m)) { return false; }
	OptionConfig def(mode->defConfig);
	if (search->solverOpts()->search == Clasp::SolverStrategies::no_learning) {
		if      (vm.count("configuration") == 0){ def = OptionConfig("[NoLearn]: --heuristic=unit"); }
		else if (vm.count("heuristic") == 0)    { removeConfig(def, "heuristic"); def.cmdLine.append(" --heuristic=unit"); }
	}
	if (vm.count("deletion") && search->enabledDel != 3) {
		if ((search->enabledDel & 1) == 0) { removeConfig(def, "del-grow"); }
		if ((search->enabledDel & 2) == 0) { removeConfig(def, "del-cfl");  }
	}
	// all from command line
	ProgramOptions::ParsedOptions parsed(vm);
	// add options from selected default config
	parsed.assign(ProgramOptions::parseCommandString(def.cmdLine, root));
	satPreDefault = parsed.count("sat-prepro") == 0;
	return search->validateOptions(root, parsed, m);
}
void ClaspOptions::removeConfig(OptionConfig& cfg, const char* opt) const {
	typedef std::string::size_type pos_type;
	pos_type pos = cfg.cmdLine.find(opt);
	if (pos != std::string::npos) {
		cfg.cmdLine.erase(pos-2, cfg.cmdLine.find("--", pos) - (pos-2));
	}
}

#define DEF_SOLVE    "--heuristic=Berkmin --restarts=x,100,1.5 --deletion=1,75 --del-init-r=200,40000 --del-max=400000 --del-algo=basic --contraction=250 --loops=common --save-p=180"
#define FRUMPY_SOLVE DEF_SOLVE " --del-grow=1.1 --strengthen=local"
#define JUMPY_SOLVE  "--heuristic=Vsids --restarts=L,100 --del-init-r=1000,20000 --del-algo=basic,2 --deletion=3,75 --del-grow=1.1,25,x,100,1.5 --del-cfl=x,10000,1.1 --del-glue=2 --update-lbd=3 --strengthen=recursive --otfs=2 --save-p=70"
#define HANDY_SOLVE  "--heuristic=Vsids --restarts=D,100,0.7 --deletion=2,50,20.0 --del-max=200000 --del-algo=sort,2 --del-init-r=1000,14000 --del-cfl=+,4000,600 --del-glue=2 --update-lbd --strengthen=recursive --otfs=2 --save-p=20 --contraction=600 --loops=distinct --counter-restarts=7 --counter-bump=1023 --reverse-arcs=2"
#define CRAFTY_SOLVE "--heuristic=Vsids --restarts=x,128,1.5 --deletion=3,75,10.0 --del-init-r=1000,9000 --del-grow=1.1,20.0 --del-cfl=+,10000,1000 --del-algo=basic --del-glue=2 --otfs=2 --reverse-arcs=1 --counter-restarts=3 --contraction=250"
#define TRENDY_SOLVE "--heuristic=Vsids --restarts=D,100,0.7 --deletion=3,50 --del-init=500,19500 --del-grow=1.1,20.0,x,100,1.5 --del-cfl=+,10000,2000 --del-algo=basic --del-glue=2 --strengthen=recursive --update-lbd --otfs=2 --save-p=75 --counter-restarts=3 --counter-bump=1023 --reverse-arcs=2  --contraction=250 --loops=common"

const char* defConfigs_g = {
	/*...*0*/"[frumpy]: " FRUMPY_SOLVE
	"\0"/*1*/"[jumpy]:  --sat-p=20,25,240,-1,1 --trans-ext=dynamic " JUMPY_SOLVE
	"\0"/*2*/"[handy]:  --sat-p=10,25,240,-1,1 --trans-ext=dynamic --backprop " HANDY_SOLVE
	"\0"/*3*/"[crafty]: --sat-p=10,25,240,-1,1 --trans-ext=dynamic --backprop " CRAFTY_SOLVE " --save-p=180"
	"\0"/*4*/"[trendy]: --sat-p=20,25,240,-1,1 --trans-ext=dynamic " TRENDY_SOLVE
#if WITH_THREADS
	"\0"/*5*/"[chatty]: --sat-p=20,25,240,-1,1 --trans-ext=dynamic --parallel-mode=4,compete --distribute=conflict,4 --integrate=gp,256"
#endif
	"\0"
};
/////////////////////////////////////////////////////////////////////////////////////////
// clasp multi-threading
/////////////////////////////////////////////////////////////////////////////////////////
#if WITH_THREADS
const EnumMap shareModes[] = {
	{"all", SharedContext::share_all}, {"problem", SharedContext::share_problem}, {"learnt", SharedContext::share_learnt}, {"no", SharedContext::share_no},
	{0,0}
};
const EnumMap intFilters[] = {
	{"all", SolveOptions::Integration::filter_no}, {"gp", SolveOptions::Integration::filter_gp},
	{"unsat", SolveOptions::Integration::filter_sat}, {"active", SolveOptions::Integration::filter_heuristic},
	{0,0}
};
const EnumMap topoMap[] = {
	{"all", SolveOptions::Integration::topo_all}, {"ring", SolveOptions::Integration::topo_ring},
	{"cube", SolveOptions::Integration::topo_cube}, {"cubex", SolveOptions::Integration::topo_cubex},
	{0,0}
};
const EnumMap distMap[] = {
	{"all", Constraint_t::learnt_conflict | Constraint_t::learnt_loop}, {"short", Constraint_t::max_value+1}, 
	{"conflict", Constraint_t::learnt_conflict}, {"loop", Constraint_t::learnt_loop},
	{0,0}
};

void ClaspOptions::initThreadOptions(ProgramOptions::OptionContext& root) {
	OptionGroup threadOps("Clasp - General Options");
	threadOps.addOptions() 
		("parallel-mode,t", notify(this, &ClaspOptions::mapSolveOpts),
		  "Run parallel search with given number of threads\n"
			"      %A: <n {1..64}>[,<mode {compete|split}>]\n"
			"        <n>   : Number of threads to use in search\n"
			"        <mode>: Run competition or splitting based search [compete]\n")
		
		("portfolio!,p,@1", storeTo(portfolio), 
		 "Use portfolio to configure threads\n"
		 "      %A: {default|seed|<file>}")
		("print-portfolio,g,@1" , flag(genTemplate), "Print default portfolio and exit\n")
		
		("global-restarts,@1", notify(this, &ClaspOptions::mapSolveOpts)->implicit("5")->arg("<X>"),
		 "Configure global restart policy\n"
		 "      %A: <n>[,<sched>] / Implicit: %I\n"
		 "        <n> : Maximal number of global restarts (0=disable)\n"
		 "     <sched>: Restart schedule [x,100,1.5] (<type {F|L|x|+}>)\n")
		
		("distribute!,@1", notify(this, &ClaspOptions::mapSolveOpts)->defaultsTo("conflict,4"),
		 "Configure nogood distribution [%D]\n"
		 "      %A: <type>[,<lbd {0..127}>][,<size>]\n"
		 "        <type> : Distribute {all|short|conflict|loop} nogoods\n"
		 "        <lbd>  : Distribute only if LBD  <= <lbd>  [4]\n"
		 "        <size> : Distribute only if size <= <size> [-1]")
		("integrate,@1", notify(this, &ClaspOptions::mapSolveOpts)->defaultsTo("gp")->state(Value::value_defaulted),
		 "Configure nogood integration [%D]\n"
		 "      %A: <pick>[,<n>][,<topo>]\n"
		 "        <pick>: Add {all|unsat|gp(unsat wrt guiding path)|active} nogoods\n"
		 "        <n>   : Always keep at least last <n> integrated nogoods   [1024]\n"
		 "        <topo>: Accept nogoods from {all|ring|cube|cubex} peers    [all]")
		("update-mode,@2", notify(this, &ClaspOptions::mapSolveOpts)->defaultsTo("0"), 
		 "Process messages on {0=propagation|1=conflict)")
		("share!,@1", notify(this, &ClaspOptions::mapSolveOpts)->defaultsTo("all"), 
		 "Configure physical sharing of constraints [%D]\n"
		 "      %A: {problem|learnt|all}\n")
	;
	root.add(threadOps);
}
bool ClaspOptions::mapSolveOpts(ClaspOptions* this_, const std::string& key, const std::string& value) {
	SolveOptions* thread = &this_->config->solve;
	if (key == "parallel-mode") {
		std::pair<uint32, std::string> x;
		if      (!parse(value, x) || x.first < 1 || x.first > SolveOptions::supportedSolvers()) { return false; }
		else if (x.second == "compete" || x.second.empty())       { thread->mode = SolveOptions::mode_compete; }
		else if (x.second == "split")                             { thread->mode = SolveOptions::mode_split;   }
		else return false;
		this_->config->reserveSolvers(x.first);
		if (thread->mode == SolveOptions::mode_compete && this_->portfolio.empty()) { this_->portfolio = "default"; }
	}
	else if (key == "distribute") {
		std::pair<std::string, std::pair<uint32, uint32> > parsed("", std::make_pair(4, UINT32_MAX));
		int typeMask = 0;
		if      (isFlagNo(value))                               { parsed.second = std::make_pair(0,0); }
		else if (!parse(value, parsed))                         { return false; }
		else if (!mapEnumImpl(distMap, parsed.first, typeMask)) { return false; }
		else if (parsed.second.first > Activity::MAX_LBD)       { return false; }
		this_->config->ctx.setDistribution(parsed.second.second, parsed.second.first, (uint32)typeMask);
	}
	else if (key == "integrate") {
		std::pair<std::string, std::pair<uint32, std::string> > parsed("", std::make_pair(1024, "all"));
		int i, j;
		if      (!parse(value, parsed))                            return false;
		else if (!mapEnumImpl(intFilters, parsed.first, i))        return false;
		else if (!mapEnumImpl(topoMap, parsed.second.second, j))   return false;
		thread->integrate.filter = static_cast<SolveOptions::Integration::Filter>(i);
		thread->integrate.topo   = static_cast<SolveOptions::Integration::Topology>(j);
		thread->integrate.grace  = parsed.second.first;
	}
	else if (key == "global-restarts") {
		std::pair<uint32, ScheduleStrategy> arg;
		if (!parse(value, arg) || arg.second.type == ScheduleStrategy::user_schedule) { return false; }
		thread->restarts.maxR  = arg.first;
		thread->restarts.sched = arg.second;
	}
	else if (key == "share") {
		int i = SharedContext::share_no;
		if (!isFlagNo(value) && !mapEnumImpl(shareModes, value, i)) { return false; }
		this_->config->ctx.physicalSharing(static_cast<SharedContext::PhysicalSharing>(i));
	}
	else if (key == "update-mode") {
		uint32 x;
		return parse(value, x) && x <= 1 && (this_->config->ctx.updateMode(SharedContext::UpdateMode(x)), true);
	}
	else { throw std::logic_error(std::string("Key not checked: ")+key); }
	return true;
}
const char* ClaspOptions::getPortfolio(std::string& mem) const {
	if (portfolio.empty() || portfolio == "no") { return 0; }
	if (portfolio == "seed")                    { return ""; }
	if (portfolio == "default")                 { return portfolio_g; }
	std::ifstream file(portfolio.c_str());
	if (!file) {
		mem = "Could not open portfolio file '";
		mem += portfolio;
		mem += "'";
		throw std::runtime_error(mem);
	}
	mem.clear();
	mem.reserve(128);
	for (std::string line; std::getline(file, line); ) {
		if (line.empty() || line[0] == '#') { continue; }
		mem += line;
		mem += '\0';
	}
	mem += '\0';
	return mem.data();
}

bool ClaspOptions::populateThreadConfigs(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& vm, Messages& m) {
	if (config->ctx.numSolvers() <= 1) {
		config->ctx.physicalSharing(SharedContext::share_no);
		config->ctx.setDistribution(0,0,0);
		return true;
	}
	std::string mem;
	const char* port = getPortfolio(mem);
	bool heuDef      = vm.count("opt-heuristic") == 0;
	for (uint32 i = 1; i < config->ctx.numSolvers(); ++i) {
		// copy all from command-line
		// NOTE: this also copies any default values
		SolverConfig* x = config->addSolver();
		x->initFrom(*config->master());
		if (heuDef) {
			x->solver->strategies().optHeu |= (i & 3u);
		}
	}
	if (port != 0) { 
		bool addSeed = false;
		const char* p= port;
		OptionContext local;
		uint32 delOpt= search->enabledDel;
		for (uint32 i = 0; i != config->numSolvers(); ++i) {
			search->initOptions(root, local, config->getSolver(i), delOpt);
			OptionConfig cmd(p);
			if (!applySearchConfig(local, cmd, vm, m)) { return false; }
			if (addSeed) { search->local->solver->strategies().rng.srand(i); }
			if (*p)      { p += strlen(p) + 1; }
			if (!*p)     { p  = port; addSeed = true; }
		}
	}
	return true;
}

bool ClaspOptions::applySearchConfig(const OptionContext& ctx, const OptionConfig& c, const ProgramOptions::ParsedOptions& vm, Messages& m) {
	try {
		// all from command-line
		ProgramOptions::ParsedOptions parsed(vm);
		// all from current config
		parsed.assign(ProgramOptions::parseCommandString(c.cmdLine, ctx));
		if (!search->validateOptions(ctx, parsed, m)) { throw std::logic_error(m.error); }
		return true;
	}
	catch(const std::exception& e) {
		std::string err("In [");
		err += c.name;
		err += "]: ";
		err += e.what();
		m.error = err;
		return false;
	}
}
const char* portfolio_g = {
	/*     0 */"[CRAFTY]: " CRAFTY_SOLVE " --opt-heu=1"
	"\0"/* 1 */"[TRENDY]: " TRENDY_SOLVE " --opt-heu=1"
	"\0"/* 2 */"[FRUMPY]: " FRUMPY_SOLVE
	"\0"/* 3 */"[JUMPY]:  " JUMPY_SOLVE " --opt-heu=3"
	"\0"/* 4 */"[STRONG]: " DEF_SOLVE " --berk-max=512 --del-grow=1.1,25 --otfs=2 --reverse-arcs=2 --strengthen=recursive --init-w=2 --lookahead=atom,10"
	"\0"/* 5 */"[HANDY]:  " HANDY_SOLVE
	"\0"/* 6 */"[S2]: --heuristic=Vsids --reverse-arcs=1 --otfs=1 --local-restarts --save-progress=0 --contraction=250 --counter-restart=7 --counter-bump=200 --restarts=x,100,1.5 --del-init=800,-1 --del-algo=basic,0 --deletion=3,60 --strengthen=local --del-grow=1.0,1.0 --del-glue=4 --del-cfl=+,4000,300,100"
	"\0"/* 7 */"[S4]: --heuristic=Vsids --restarts=L,256 --counter-restart=3 --strengthen=recursive --update-lbd --del-glue=2 --otfs=2 --del-algo=inp_sort,2 --deletion=1,75,20 --del-init=1000,19000"
	"\0"/* 8 */"[SLOW]:  --heuristic=Berkmin --berk-max=512 --restarts=F,16000 --lookahead=atom,50"
	"\0"/* 9 */"[VMTF]:  --heu=VMTF --str=no --contr=0 --restarts=x,100,1.3 --del-init-r=800,9200"
	"\0"/* 10 */"[SIMPLE]:  --heu=VSIDS  --strengthen=recursive --restarts=x,100,1.5,15 --contraction=0"
	"\0"/* 11*/"[LUBY-SP]: --heu=VSIDS --restarts=L,128 --save-p --otfs=1 --init-w=2 --contr=0 --opt-heu=3"
	"\0"/* 12 */"[LOCAL-R]: --berk-max=512 --restarts=x,100,1.5,6 --local-restarts --init-w=2 --contr=0"
	"\0"
};
#else
void ClaspOptions::initThreadOptions(ProgramOptions::OptionContext&) {}
bool ClaspOptions::mapSolveOpts(ClaspOptions*, const std::string&, const std::string&) { return false; }
bool ClaspOptions::populateThreadConfigs(const ProgramOptions::OptionContext&, const ProgramOptions::ParsedOptions&, Messages&) { return true; }
#endif
#undef SET
#undef SET_M
#undef SET_R
}
