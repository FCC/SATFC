// 
// Copyright (c) 2009-2012, Benjamin Kaufmann
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
#include "clasp_output.h"
#include "alarm.h" // for SIGALRM
#include <clasp/clasp_facade.h>
#include <clasp/minimize_constraint.h>
#include <clasp/satelite.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <numeric>
#include <climits>

namespace Clasp { namespace {
inline std::string prettify(const std::string& str) {
	if (str.size() < 40) return str;
	std::string t("...");
	t.append(str.end()-38, str.end());
	return t;
}
inline double percent(uint64 r, uint64 b) { 
	if (b == 0) return 0;
	return (static_cast<double>(r)/b)*100.0;
}
inline double average(uint64 x, uint64 y) {
	if (!x || !y) return 0.0;
	return static_cast<double>(x) / static_cast<double>(y);
}
}

/////////////////////////////////////////////////////////////////////////////////////////
// RunSummary and OutputFormat
/////////////////////////////////////////////////////////////////////////////////////////
OutputFormat::RunSummary::Result OutputFormat::RunSummary::result() const {
	const Enumerator* e = ctx.enumerator();
	uint64 m  = e->enumerated;
	Result r  = result_unknown;
	if (m != 0) { // SAT
		if (!e->minimize()) { r = result_sat; }
		else if (complete()){ r = result_optimum; }
		else                { r = result_sat_opt; }
	}
	else if (complete()) { r = result_unsat; }
	return r;
}

uint64 OutputFormat::RunSummary::models() const {
	return (ctx.enumerator()->enumerated == 0 || !(ctx.enumerator()->optimize()||consequences)) ? ctx.enumerator()->enumerated : 1;
}

OutputFormat::OutputFormat(uint32 verbosity) : curr_(0), next_(0), lpStats_(0), verbosity_(0), quiet_(0) {
	result[RunSummary::result_unknown] = "UNKNOWN";
	result[RunSummary::result_unsat]   = "UNSATISFIABLE";
	result[RunSummary::result_sat]     = "SATISFIABLE";
	result[RunSummary::result_optimum] = "OPTIMUM FOUND";
	result[RunSummary::result_sat_opt] = "SATISFIABLE";
	verbosity_ = (int)std::min(verbosity, (uint32)INT_MAX);
}
OutputFormat::~OutputFormat() {
	delete lpStats_;
}
void OutputFormat::initSolve(const Solver& s, ProgramBuilder* api) {
	// grab the stats_ so that we can later print them
	if (api) {
		if (!lpStats_) lpStats_ = new PreproStats;
		lpStats_->accu(api->stats);
	}
	saved_.resize(2*s.assignment().numVars());
	curr_ = 0;
	next_ = &saved_[0];
}

void OutputFormat::reportModel(const Solver& s, const Enumerator& en) {
	// remember current (tentative) model
	if (modelQ() < print_no) {
		Model m = storeModel(s.assignment());
		if (modelQ() == print_all) {
			// fire & forget: just output the model
			curr_ = 0;
			printModel(m, s.sharedContext()->symTab(), en);
		}
	}
	reportOptimize(en, print_all);
}

void OutputFormat::reportConsequences(const Solver& s, const Enumerator& en, const char* cbType) {
	if (modelQ() == print_all) {
		printConsequences(s.sharedContext()->symTab(), en, cbType);
	}
	else if (modelQ() == print_best) {
		curr_ = &saved_[0];
	}
	reportOptimize(en, print_all);
}

void OutputFormat::reportResult(const RunSummary& sol, const SolveStats** stats, size_t num) {
	const Enumerator& en = *sol.ctx.enumerator();
	if (curr_ != 0) {
		// print saved model
		if (sol.consequences) {
			printConsequences(sol.ctx.symTab(), en, sol.consequences);
		}
		else {
			Model m(curr_, curr_ > next_ ? static_cast<uint32>(curr_ - next_) : static_cast<uint32>(next_ - curr_));
			printModel(m, sol.ctx.symTab(), en);
		}
	}
	if (en.enumerated > 0 && optQ() == print_best) {
		reportOptimize(en, print_best);
	}
	printResult(sol, stats, num);
}

void OutputFormat::reportOptimize(const Enumerator& en, PrintLevel printLevel) {
	if (en.minimize() && optQ() == printLevel) {
		printOptimize(*en.minimize());
	}
}

OutputFormat::Model OutputFormat::storeModel(const Assignment& a) {
	assert(saved_.size() == 2*a.numVars());
	ValueRep* x= next_;
	for (Var v = 0, end = a.numVars(); v != end; ++v) {
		*x++ = a.value(v);
	}
	curr_      = next_;
	next_      = curr_ == &saved_[0] ? x : &saved_[0];
	return Model(curr_, curr_ > next_ ? static_cast<uint32>(curr_ - next_) : static_cast<uint32>(next_ - curr_));
}
/////////////////////////////////////////////////////////////////////////////////////////
// DefaultOutput
/////////////////////////////////////////////////////////////////////////////////////////
#define printKey(k)                  printf("%s%-*s: ", format_[cat_comment], w_, (k))
#define printKeyValue(k, fmt, value) printf("%s%-*s: "fmt, format_[cat_comment], w_, (k), (value))
const char* const lnSep = "----------------------------------------------------------------------------";

DefaultOutput::DefaultOutput(uint32 v, const std::pair<uint32,uint32>& q, Format f, char ifs)
	: OutputFormat(v)
	, prepro_(0)
	, as_(0)
	, eom_(0)
	, header_(1)
	, ev_(2)
	, IFS_(ifs) {
	configureFormat(f, q, ifs);
}

DefaultOutput::~DefaultOutput() {
	free((void*)as_);
	free((void*)eom_);
}

// sets up format strings and configures verbosity and quiet levels
void DefaultOutput::configureFormat(Format f, PrintPair q, char ifs) {
	format_[cat_comment]   = "";
	format_[cat_value]     = "";
	format_[cat_objective] = "Optimization: ";
	format_[cat_result]    = "";
	format_[cat_value_term]= "";
	format_[cat_atom_pre]  = "";
	format_[cat_atom_post] = "";
	if (f == format_aspcomp) {
		format_[cat_atom_post] = ".";
		format_[cat_objective] = "OPT: ";
		result[RunSummary::result_sat]     = "ANSWER SET FOUND";
		result[RunSummary::result_unsat]   = "INCONSISTENT";
		result[RunSummary::result_sat_opt] = "";
		setVerbosity(-1);
		if (q.second == uint32(print_f_default)) {
			q.second = print_no;
		}
	}
	else if (f == format_sat09 || f == format_pb09) {
		format_[cat_comment]   = "c ";
		format_[cat_value]     = "v ";
		format_[cat_objective] = "o ";
		format_[cat_result]    = "s ";
		format_[cat_value_term]= "0";
		if (f == format_pb09) {
			format_[cat_value_term]= "";
			format_[cat_atom_pre]  = "x";
			if (q.first == uint32(print_f_default)) {
				q.first = print_best;
			}
		}
	}
	setQuiet(q);
	free((void*)as_);
	free((void*)eom_);
	if ( (IFS_=ifs) != '\n') {
		char t[2] = { IFS_, '\0' };
		as_       = strdup(t);
	}
	else {
		char* temp= (char*)malloc(strlen(format_[cat_value])+2);
		temp[0]   = IFS_;
		temp[1]   = '\0';
		temp      = strcat(temp, format_[cat_value]);
		as_       = temp;
	}
	if (strcmp(format_[cat_value_term], "") == 0) {
		eom_ = strdup("\n");
	}
	else {
		char* temp = (char*)malloc(strlen(as_)+strlen(format_[cat_value_term])+2);
		temp       = strcpy(temp, as_);
		temp       = strcat(temp, format_[cat_value_term]);
		temp       = strcat(temp, "\n");
		eom_       = temp;
	}
	w_ = 12+(int)strlen(format_[cat_comment]);
}

void DefaultOutput::comment(int v, const char* fmt, ...) const {
	if (verbosity() >= v) {
		printf("%s", format_[cat_comment]);
		va_list args;
		va_start(args, fmt);
		vfprintf(stdout, fmt, args);
		va_end(args);
		fflush(stdout);
	}
}	

void DefaultOutput::reportProgress(const PreprocessEvent& e) {
	if (e.type == (int)SatElite::SatElite::event_type && verbosity() >= 2) {
		const SatElite::SatElite::Event& ev = static_cast<const SatElite::SatElite::Event&>(e);
		printf("\r");
		if (ev.op != '*' || ev.cur != ev.max) { comment(2, "Preprocessing: %c: %8u/%-8u", ev.op, ev.cur, ev.max); }
		else                                  { comment(2, "Preprocessing: "); prepro_ = ev.self; }
	}
}

void DefaultOutput::reportProgress(const SolveEvent& e) {
	if (verbosity() < 2 || verbosity() < e.type) { return; }
	if (e.type != SolveEvent_t::progress_path) {
		if (ev_ != 1) { ev_ = 1; printf("%s%s\n", format_[cat_comment], lnSep); }
		if (e.type == SolveEvent_t::progress_state) {
			const SolveStateEvent& ev = static_cast<const SolveStateEvent&>(e);
			if (ev.time < 0) { printf("%s[%u:S]| %-15s %-20s  %30c|\n", format_[cat_comment], e.solver->id(), "ENTERING", ev.state, ' '); }
			else             { printf("%s[%u:S]| %-15s %-20s after %-6.3fs %17c|\n", format_[cat_comment], e.solver->id(), "LEAVING", ev.state, ev.time, ' '); }
		}
		else if (e.type == SolveEvent_t::progress_msg) { 
			printf("%s[%u:G]| %-30s%38c|\n", format_[cat_comment], e.solver->id(), static_cast<const SolveMsgEvent&>(e).msg, ' '); 
		}
	}
	else {
		const SolvePathEvent& ev = static_cast<const SolvePathEvent&>(e);
		const Solver& s = *ev.solver;
		uint32 fixed    = s.decisionLevel() > 0 ? s.levelStart(1) : s.numAssignedVars();
		if (--header_ == 0) {
			ev_     = 2;
			header_ = 20;
			printf("%s%s\n"
				"%s[ID:T]     Vars           Constraints         State            Limits\n"
				"%s       #free/#fixed   #problem/#learnt  #conflicts/ratio #conflict/#learnt\n"
				"%s%s\n", 
				format_[cat_comment], lnSep, format_[cat_comment], format_[cat_comment], format_[cat_comment], lnSep);
		}
		else if (++ev_ == 2) { printf("%s%s\n", format_[cat_comment], lnSep); }
		printf("%s[%u:%c]|%7u/%-7u|%8u/%-8u|%10"PRIu64"/%-6.3f|%8"PRId64"/%-8"PRId64"|\n"
			, format_[cat_comment]
			, s.id()
			, static_cast<char>(ev.evType)
			, s.numFreeVars()
			, fixed
			, s.numConstraints()
			, s.numLearntConstraints()
			, s.stats.conflicts
			, s.stats.conflicts/std::max(1.0,double(s.stats.choices))
			, ev.cLimit <= (UINT32_MAX) ? (int64)ev.cLimit:-1
			, ev.lLimit != (UINT32_MAX) ? (int64)ev.lLimit:-1
		);
	}
	fflush(stdout);
}

void DefaultOutput::reportState(int state, bool enter, double time) {
	if (enter && verbosity()) {
		if (state == ClaspFacade::state_start) {
			if (!solver().empty()) comment(1, "%s\n", solver().c_str());
			if (!input().empty())  comment(1, "Reading from %s\n", prettify(input()).c_str());
		}
		else if (state == ClaspFacade::state_read) {
			comment(2, "Reading      : ");
		}
		else if (state == ClaspFacade::state_preprocess) {
			comment(2, "Preprocessing: ");
		}
		else if (state == ClaspFacade::state_solve) {
			if (verbosity() > 1) { printf("%s%s\n%sSolving...%66c\n", format_[cat_comment], lnSep, format_[cat_comment], '|'); }
			else                 { comment(1, "Solving...\n"); }
		}
	}
	else if (!enter && verbosity() > 1) {
		if (state == ClaspFacade::state_read || state == ClaspFacade::state_preprocess) {
			printf("%.3f", time);
			if (prepro_ != 0) { printf(" (ClRemoved: %u ClAdded: %u LitsStr: %u)", prepro_->stats.clRemoved, prepro_->stats.clAdded, prepro_->stats.litsRemoved); }
			printf("\n");
		}
		else if (state == ClaspFacade::state_solve) { printf("%s%s\n", format_[cat_comment], lnSep); } 
	}
}	

void DefaultOutput::printModel(const Model& m, const SymbolTable& index, const Enumerator& en) {
	comment(1, "Answer: %" PRIu64"\n", en.enumerated);
	printf("%s", format_[cat_value]);
	const char* sep = "";
	if (index.type() == SymbolTable::map_indirect) {
		for (SymbolTable::const_iterator it = index.begin(); it != index.end(); ++it) {
			if (m.value(it->second.lit.var()) == trueValue(it->second.lit) && !it->second.name.empty()) {
				printf("%s%s%s%s", sep, format_[cat_atom_pre], it->second.name.c_str(), format_[cat_atom_post]);
				sep = as_;
			}	
		}
	}
	else {
		uint32 const maxLineLen = 70;
		uint32       printed    = 0;
		for (Var v = 1; v < index.size(); ++v) {
			printed += printf("%s%s%s%u%s", sep, m.value(v) == value_false ? "-":"", format_[cat_atom_pre], v, format_[cat_atom_post]);
			sep      = as_;
			if (printed >= maxLineLen && IFS_ != '\n') {
				printed = 0;
				sep     = "";
				printf("\n%s", format_[cat_value]);
			}
		}
	}
	printExtendedModel(m);
	printf("%s", eom_);
	fflush(stdout);
}

void DefaultOutput::printConsequences(const SymbolTable& index, const Enumerator&, const char* cbType) {
	comment(1, "%s consequences:\n", cbType);
	printf("%s", format_[cat_value]);
	const char* sep = "";
	for (SymbolTable::const_iterator it = index.begin(); it != index.end(); ++it) {
		if (it->second.lit.watched()) {
			printf("%s%s%s%s", sep, format_[cat_atom_pre], it->second.name.c_str(), format_[cat_atom_post]);
			sep = as_;
		}
	}
	printf("%s", eom_);
	fflush(stdout);
}
void DefaultOutput::printOptimize(const SharedMinimizeData& m) {
	printf("%s", format_[cat_objective]);
	printOptimizeValues(m);
	printf("\n");
	fflush(stdout);
}

void DefaultOutput::printOptimizeValues(const SharedMinimizeData& m) const {
	const SharedMinimizeData::SumVec& opt = m.optimum()->opt;
	const char* sep = "";
	printf("%s%" PRId64, sep, opt[0]);
	sep = IFS_ != '\n' ? as_ : " ";
	for (uint32 i = 1, end = m.numRules(); i != end; ++i) {
		printf("%s%" PRId64, sep, opt[i]);
	}
}

void DefaultOutput::printResult(const RunSummary& sol, const SolveStats** stats, size_t num) {
	const Enumerator& en = *sol.ctx.enumerator();
	printf("%s%s\n", format_[cat_result], result[sol.result()]);
	if (verbosity() > (0 - (num!=0))) {
		printf("\n");
		if      (sol.termSig() == SIGALRM) { printKeyValue("TIME LIMIT", "%d\n", 1);  }
		else if (sol.termSig() != 0)       { printKeyValue("INTERRUPTED", "%d\n", 1); }
		printKey("Models");
		if (!sol.complete()) {
			char buf[64];
			int wr    = sprintf(buf, "%" PRIu64, sol.models());
			buf[wr]   = '+';
			buf[wr+1] = 0;
			printf("%-6s\n", buf);
		}
		else {
			printf("%-6"PRIu64"\n", sol.models());
		}
		if (en.enumerated) {
			if (en.enumerated != sol.models()) { 
				printKeyValue("  Enumerated", "%" PRIu64"\n", en.enumerated);
			}
			if (sol.consequences) {
				printf("%s  %-*s: %s\n", format_[cat_comment], w_-2, sol.consequences, sol.complete()?"yes":"unknown");
			}
			if (en.minimize()) {
				printKeyValue("  Optimum", "%s\n", sol.complete()?"yes":"unknown");
				if (en.optimize()) {
					printKey("Optimization");
					printOptimizeValues(*en.minimize());
				}
				printf("\n");
			}
		}
		printKey("Time");
		printf("%.3fs (Solving: %.2fs 1st Model: %.2fs Unsat: %.2fs)\n"
				, sol.totalTime
				, sol.solveTime
				, sol.modelTime
				, sol.unsatTime);
		printKeyValue("CPU Time", "%.3fs\n", sol.cpuTime);
	}
	if (verbosity() >= 0 && num != 0) {
		printStats(sol, stats, num);
	}
}

void DefaultOutput::printStats(const RunSummary& sol, const SolveStats** stats, std::size_t num) {
	const ProblemStats& pr = sol.ctx.stats();
	const SolveStats* st   = stats[0];
	printSolveStats(*st);
	printf("\n");
	if (lpStats()) {
		printLpStats(*lpStats());
		printf("\n");
	}
	printProblemStats(pr);
	printLemmaStats(*st);
	printJumpStats(*st);
	printThreadStats(stats, num, sol);
	fflush(stdout);
}

void DefaultOutput::printSolveStats(const SolveStats& st) const {
	printKeyValue("Choices", "%" PRIu64"\n", st.choices);
	printKeyValue("Conflicts", "%" PRIu64"\n", st.conflicts);
	printKeyValue("Restarts", "%-6"PRIu64"", st.restarts);
	if (st.restarts) {
		printf(" (Average: %.2f Last: %" PRIu64")", average(st.analyzed, st.restarts), st.cflLast);
	}
	printf("\n");
}

void DefaultOutput::printLpStats(const PreproStats& lp) const {
	printKeyValue("Atoms", "%-6u", lp.atoms);
	if (lp.trStats) {
		printf(" (Original: %u Auxiliary: %u)", lp.atoms-lp.trStats->auxAtoms, lp.trStats->auxAtoms);
	}
	printf("\n"); 
	printKeyValue("Rules", "%-6u", lp.rules[0]);
	printf(" (1: %u", lp.rules[BASICRULE] - (lp.trStats?lp.trStats->rules[0]:0));
	if (lp.trStats) { printf("/%u", lp.rules[BASICRULE]); }
	for (int i = 2; i <= OPTIMIZERULE; ++i) {
		if (lp.rules[i] > 0) { 
			printf(" %d: %u", i, lp.rules[i]);
			if (lp.trStats) { printf("/%u", lp.rules[i]-lp.trStats->rules[i]); }
		}
	}
	printf(")\n");
	printKeyValue("Bodies", "%-6u\n", lp.bodies);
	if (lp.sumEqs() > 0) {
		printKeyValue("Equivalences", "%-6u", lp.sumEqs());
		printf(" (Atom=Atom: %u Body=Body: %u Other: %u)\n" 
			, lp.numEqs(Var_t::atom_var)
			, lp.numEqs(Var_t::body_var)
			, lp.numEqs(Var_t::atom_body_var));
	}
	printKey("Tight");
	if      (lp.sccs == 0)               { printf("Yes"); }
	else if (lp.sccs != PrgNode::noScc)  { printf("%-6s (SCCs: %u Nodes: %u)", "No", lp.sccs, lp.ufsNodes); }
	else                                 { printf("N/A"); }
	printf("\n");
}

void DefaultOutput::printProblemStats(const ProblemStats& ps) const {
	uint32 sum = ps.constraints + ps.constraints_binary + ps.constraints_ternary;
	printKeyValue("Variables", "%-6u", ps.vars);
	printf(" (Eliminated: %4u Frozen: %4u)\n", ps.vars_eliminated, ps.vars_frozen);
	printKeyValue("Constraints", "%-6u", sum);
	printf(" (Binary:%5.1f%% Ternary:%5.1f%% Other:%5.1f%%)\n"
		, percent(ps.constraints_binary, sum)
		, percent(ps.constraints_ternary, sum)
		, percent(ps.constraints, sum));
}

void DefaultOutput::printLemmaStats(const SolveStats& st) const {
	uint64 sum   = std::accumulate(st.learnts, st.learnts+Constraint_t::max_value, uint64(0));
	printKeyValue("Lemmas", "%-6"PRIu64, sum);
	printf(" (Binary:%5.1f%% Ternary:%5.1f%% Other:%5.1f%%)\n"
			, percent(st.binary, sum)
			, percent(st.ternary, sum)
			, percent(sum - st.binary - st.ternary, sum));
	printKeyValue("  Conflict", "%-6"PRIu64, st.learnts[0]);
	printf(" (Average Length: %.1f) \n", average(st.lits[0], st.learnts[0]));
	printKeyValue("  Loop", "%-6"PRIu64, st.learnts[1]);
	printf(" (Average Length: %.1f) \n", average(st.lits[1], st.learnts[1]));
	printKeyValue("  Other", "%-6"PRIu64, st.learnts[2]);
	printf(" (Average Length: %.1f)\n"  , average(st.lits[2], st.learnts[2]));
	printKeyValue("  Deleted", "%-6"PRIu64"\n", st.deleted);
}

void DefaultOutput::printJumpStats(const SolveStats& st) const {
	if (st.jumps) {
		const JumpStats& js = *st.jumps;
		w_ += 8;
		printf("\n");
		printKeyValue("Backtracks", "%-6"PRIu64"\n", st.conflicts-st.analyzed);
		printKeyValue("Backjumps", "%-6"PRIu64, st.analyzed);
		printf(" (Bounded: %" PRIu64")\n", js.bJumps);
		printKeyValue("Skippable Levels", "%-6"PRIu64, js.jumpSum);
		printf(" (Skipped: %" PRIu64" Rate: %5.1f%%)\n", js.jumpSum - js.boundSum, percent(js.jumpSum - js.boundSum, js.jumpSum));
		printKeyValue("Max Jump Length", "%-6u", js.maxJump);
		printf(" (Executed: %u)\n", js.maxJumpEx);
		printKeyValue("Max Bound Length", "%-6u\n", js.maxBound);
		printKeyValue("Average Jump Length", "%-6.1f", average(js.jumpSum, st.analyzed));
		printf(" (Executed: %.1f)\n", average(js.jumpSum-js.boundSum, st.analyzed));
		printKeyValue("Average Bound Length", "%-6.1f\n", average(js.boundSum,js.bJumps));
		printKeyValue("Average Model Length", "%-6.1f\n", average(js.modLits,st.models));
		w_ -= 8;
	}
}

void DefaultOutput::printThreadStats(const SolveStats** stats, std::size_t num, const RunSummary& sol) const {
	if (num > 1 || stats[0]->parallel) {
		printf("\n");
		comment(0, "============ PARALLEL STATISTICS ============\n\n");
		printKeyValue("Threads", "%u\n", sol.ctx.numSolvers());
		printKeyValue("Winner",  "%d\n", sol.winner());
		DefaultOutput::printParallelStats(*stats[0], true);
		for (std::size_t i = 1; i < num; ++i) {
			comment(0, "[Thread %u]\n", (uint32)i-1);
			DefaultOutput::printParallelStats(*stats[i], false);
			DefaultOutput::printSolveStats(*stats[i]);
			DefaultOutput::printLemmaStats(*stats[i]);
			DefaultOutput::printJumpStats(*stats[i]);
			printf("\n\n");
		}
	}
}

void DefaultOutput::printParallelStats(const SolveStats& stats, bool accu) const {
	if (stats.parallel) {
		const ParallelStats& ps = *stats.parallel;
		uint64 sumLem = stats.learnts[0] + stats.learnts[1];
		if (!accu) {
			printKeyValue("CPU Time",  "%.3fs\n", ps.cpuTime);
			printKeyValue("Models", "%" PRIu64"\n", stats.models);
		}
		printKeyValue("Distributed", "%-6"PRIu64, ps.distributed);
		printf(" (Ratio: %.2f Average LBD: %.2f) \n", ps.distributed/std::max(1.0, double(sumLem)), average(ps.sumLbd, ps.distributed));
		printKeyValue("Integrated", "%-6"PRIu64, ps.integrated);
		if (accu) { printf(" (Ratio: %.2f ", ps.integrated/std::max(1.0, double(ps.distributed))); }
		else      { printf(" ("); }
		printf("Unit: %" PRIu64" Average Jumps: %.2f)\n", ps.imps, average(ps.jumps, ps.imps));
		printKeyValue("Splits", "%" PRIu64"\n", (uint64)ps.splits);
		printKeyValue("Problems", "%-6"PRIu64, (uint64)ps.gps);
		printf(" (Average Length: %.2f) \n\n", average(ps.gpLits, ps.gps));
	}
}

/////////////////////////////////////////////////////////////////////////////////////////
// JsonOutput
/////////////////////////////////////////////////////////////////////////////////////////
#undef printKeyValue
#undef printKey

JsonOutput::JsonOutput(uint32 v, const std::pair<uint32,uint32>& q) : OutputFormat(v), indent_(0), open_(""), hasModel_(false), hasWitness_(false) {
	setQuiet(q);
}

void JsonOutput::printKey(const char* k) {
	printf("%s%-*s\"%s\": ", open_, indent_, " ", k);
	open_ = ",\n";
}

void JsonOutput::printString(const char* v, const char* sep) {
	assert(v);
	const uint32 BUF_SIZE = 1024;
	char buf[BUF_SIZE];
	uint32 n = 0;
	buf[n++] = '"';
	while (*v) {
		if      (*v != '\\' && *v != '"')                       { buf[n++] = *v++; }
		else if (*v == '"' || !strchr("\"\\/\b\f\n\r\t", v[1])) { buf[n++] = '\\'; buf[n++] = *v++; }
		else                                                    { buf[n++] = v[0]; buf[n++] = v[1]; v += 2; }
		if (n > BUF_SIZE - 2) { buf[n] = 0; printf("%s%s", sep, buf); n = 0; sep = ""; }
	}
	buf[n] = 0;
	printf("%s%s\"", sep, buf);
}

void JsonOutput::printKeyValue(const char* k, const char* v) {
	printf("%s%-*s\"%s\": ", open_, indent_, " ", k);
	printString(v,"");
	open_ = ",\n";
}
void JsonOutput::printKeyValue(const char* k, uint64 v) {
	printf("%s%-*s\"%s\": %" PRIu64, open_, indent_, " ", k, v);
	open_ = ",\n";
}
void JsonOutput::printKeyValue(const char* k, uint32 v) { return printKeyValue(k, uint64(v)); }
void JsonOutput::printKeyValue(const char* k, double v) {
	printf("%s%-*s\"%s\": %.3f", open_, indent_, " ", k, v);
	open_ = ",\n";
}

void JsonOutput::startObject(const char* k, ObjType t) {
	if (k) {
		printKey(k);	
	}
	else {
		printf("%s%-*.*s", open_, indent_, indent_, " ");
	}
	printf("%c\n", t == type_object ? '{' : '[');
	indent_ += 2;
	open_ = "";
}
void JsonOutput::endObject(ObjType t) {
	assert(indent_ > 0);
	indent_ -= 2;
	printf("\n%-*.*s%c", indent_, indent_, " ", t == type_object ? '}' : ']');
	open_ = ",\n";
}

void JsonOutput::reportState(int state, bool enter, double) {
	if (indent_ == 0) {
		open_ = "";
		startObject();
	}
	if (state == ClaspFacade::state_start && enter) {
		printKeyValue("Solver", solver().c_str());
		printKeyValue("Input", input().c_str());
	}
}

void JsonOutput::startModel() {
	if (!hasWitness_) {
		startObject("Witnesses", type_array);
		hasWitness_ = true;
	}
	if (hasModel_) {
		endObject();
		hasModel_ = false;
	}
	startObject();
	hasModel_ = true;
}
	
void JsonOutput::printModel(const Model& m, const SymbolTable& index, const Enumerator&) {
	startModel();
	startObject("Value", type_array);
	const char* sep = "";
	printf("%-*s", indent_, " ");
	if (index.type() == SymbolTable::map_indirect) {
		for (SymbolTable::const_iterator it = index.begin(); it != index.end(); ++it) {
			if (m.value(it->second.lit.var()) == trueValue(it->second.lit) && !it->second.name.empty()) {
				printString(it->second.name.c_str(), sep);
				sep = ", ";
			}	
		}
	}
	else {
		for (Var v = 1; v < index.size(); ++v) {
			printf("%s%d", sep, (m.value(v) == value_false ? -static_cast<int>(v) : static_cast<int>(v)));
			sep = ", ";
		}
	}
	endObject(type_array);
}

void JsonOutput::printConsequences(const SymbolTable& index, const Enumerator&, const char* cbType) {
	startModel();
	startObject(cbType, type_array);
	const char* sep = "";
	printf("%-*s", indent_, " ");
	for (SymbolTable::const_iterator it = index.begin(); it != index.end(); ++it) {
		if (it->second.lit.watched()) {
			printString(it->second.name.c_str(), sep);
			sep = ", ";
		}
	}
	endObject(type_array);
}

void JsonOutput::printOptimize(const SharedMinimizeData& m) { 
	startObject("Opt", type_array);
	const SharedMinimizeData::SumVec& opt = m.optimum()->opt;
	printf("%-*s", indent_, " ");
	const char* sep = "";
	for (uint32 i = 0, end = m.numRules(); i != end; ++i) {
		printf("%s%" PRId64, sep, opt[i]);
		sep = ", ";
	}
	endObject(type_array);
	if (hasModel_) {
		endObject();
		hasModel_ = false;		
	}
}

void JsonOutput::printResult(const RunSummary& sol, const SolveStats** stats, std::size_t num) {
	if (hasModel_) {
		hasModel_ = false;
		endObject();
	}
	if (hasWitness_) {
		hasWitness_ = false;
		endObject(type_array);
	}
	printKeyValue("Result", result[sol.result()]);
	const Enumerator& en = *sol.ctx.enumerator();
	startObject("Stats");
	if      (sol.termSig() == SIGALRM) { printKeyValue("TIME LIMIT", uint32(1));  }
	else if (sol.termSig() != 0)       { printKeyValue("INTERRUPTED", uint32(1)); }
	printKeyValue("Models", sol.models());
	printKeyValue("Complete", sol.complete() ? "yes" : "no");
	if (en.enumerated) {
		if (sol.models() != en.enumerated) {
			printKeyValue("Enumerated", en.enumerated);
		}
		if (sol.consequences) {
			printKeyValue(sol.consequences, sol.complete() ? "yes":"unknown");
		}
		if (en.minimize()) {
			printKeyValue("Optimum", sol.complete()?"yes":"unknown");
			if (en.optimize()) {
				printOptimize(*en.minimize());
			}
		}
	}
	startObject("Time");
	printKeyValue("Total", sol.totalTime);
	printKeyValue("Solve", sol.solveTime);
	printKeyValue("Model", sol.modelTime);
	printKeyValue("Unsat", sol.unsatTime);
	printKeyValue("CPU", sol.cpuTime);
	endObject(); // Time
	if (num > 0) {
		const SolveStats* st = stats[0];
		printKeyValue("Choices", st->choices);
		printKeyValue("Conflicts", st->conflicts);
		printKeyValue("Restarts", st->restarts);
		const ProblemStats& p = sol.ctx.stats();
		printKeyValue("Variables", p.vars);
		printKeyValue("Eliminated", p.vars_eliminated);
		printKeyValue("Frozen", p.vars_frozen);
		startObject("Constraints");
		uint32 sum = p.constraints + p.constraints_binary + p.constraints_ternary;
		printKeyValue("Sum", sum);
		printKeyValue("Binary", p.constraints_binary);
		printKeyValue("Ternary", p.constraints_ternary);
		endObject(); // Constraints
		if (lpStats()) {
			printLpStats(*lpStats());
		}
		printLemmaStats(*st);
		printJumpStats(*st);
		printThreadStats(stats, num, sol);
		endObject(); // Stats
	}
	else {
		endObject(); // Stats
	}
	endObject(); // Root
	printf("\n");
}

void JsonOutput::printLpStats(const PreproStats& lp) {
	startObject("LP");
	printKeyValue("Atoms", lp.atoms);
	if (lp.trStats) {
		printKeyValue("AuxAtoms", lp.trStats->auxAtoms);
	}
	startObject("Rules");
	printKeyValue("Sum", lp.rules[0]);
	uint32 basic = lp.rules[BASICRULE] - (lp.trStats?lp.trStats->rules[0]:0);
	printKeyValue("R1", basic);
	char buf[10];
	for (uint32 i = 2; i <= OPTIMIZERULE; ++i) {
		if (lp.rules[i] > 0) {
			sprintf(buf, "R%u", i);
			printKeyValue(buf, lp.rules[i]);
		}
	}
	if (lp.trStats) {
		printKeyValue("Created", lp.trStats->rules[0]);
		startObject("Translated");
		for (uint32 i = 2; i <= OPTIMIZERULE; ++i) {
			if (lp.rules[i] > 0) {
				sprintf(buf, "R%u", i);
				printKeyValue(buf, lp.trStats->rules[i]);
			}
		}
		endObject();
	}
	endObject(); // Rules
	printKeyValue("Bodies", lp.bodies);
	if (lp.sccs == 0) { 
		printKeyValue("Tight", "yes");
	}
	else if (lp.sccs != PrgNode::noScc) {
		printKeyValue("Tight", "no");
		printKeyValue("SCCs", lp.sccs);
		printKeyValue("UfsNodes", lp.ufsNodes);
	}
	else {
		printKeyValue("Tight", "N/A");
	}
	startObject("Equivalences");
	printKeyValue("Sum", lp.sumEqs());
	printKeyValue("Atom", lp.numEqs(Var_t::atom_var));
	printKeyValue("Body", lp.numEqs(Var_t::body_var));
	printKeyValue("Other", lp.numEqs(Var_t::atom_body_var));
	endObject();
	endObject(); // LP
}

void JsonOutput::printLemmaStats(const SolveStats& st) {
	startObject("Lemma");
	uint64 sum   = std::accumulate(st.learnts, st.learnts+Constraint_t::max_value, uint64(0));
	printKeyValue("Sum", sum);
	printKeyValue("Binary", st.binary);
	printKeyValue("Ternary", st.ternary);
	printKeyValue("Conflict", st.learnts[0]);
	printKeyValue("Loop", st.learnts[1]);
	printKeyValue("Other", st.learnts[2]);
	printKeyValue("Deleted" , st.deleted);
	printKeyValue("AvgConflict", average(st.lits[0], st.learnts[0]));
	printKeyValue("AvgLoop", average(st.lits[1], st.learnts[1]));
	printKeyValue("AvgOther", average(st.lits[2], st.learnts[2]));
	endObject();
}

void JsonOutput::printJumpStats(const SolveStats& st) {
	if (st.jumps) {
		const JumpStats& js = *st.jumps;
		startObject("Jumps");
		printKeyValue("Backtracks", st.conflicts-st.analyzed);
		printKeyValue("Backjumps", st.analyzed);
		printKeyValue("Bounded", js.bJumps);
		printKeyValue("Skippable", js.jumpSum);
		printKeyValue("Skipped", js.jumpSum - js.boundSum);
		printKeyValue("MaxJump", js.maxJump);
		printKeyValue("MaxJumpEx", js.maxJumpEx);
		printKeyValue("MaxBound", js.maxBound);
		printKeyValue("AvgJump", average(js.jumpSum, st.analyzed));
		printKeyValue("AvgJumpEx", average(js.jumpSum-js.boundSum, st.analyzed));
		printKeyValue("AvgBound", average(js.boundSum,js.bJumps));
		printKeyValue("AvgModel", average(js.modLits,st.models));
		endObject();
	}
}

void JsonOutput::printThreadStats(const SolveStats** stats, std::size_t num, const RunSummary& sol) {
	if (stats[0]->parallel) {
		startObject("Parallel");
		const ParallelStats& ps = *stats[0]->parallel;
		if (sol.winner() >= 0) printKeyValue("Winner", (uint32)sol.winner());
		printKeyValue("Distributed", ps.distributed);
		printKeyValue("Splits", ps.splits);
		printKeyValue("Problems", ps.gps);
		printKeyValue("AvgGPLength", average(ps.gpLits, ps.gps));
		endObject();
	}
	if (num > 1) {
		startObject("Threads", type_array);
		for (std::size_t i = 1; i < num; ++i) {
			startObject();
			JsonOutput::printParallelStats(*stats[i]);
			JsonOutput::printLemmaStats(*stats[i]);
			JsonOutput::printJumpStats(*stats[i]);
			endObject();
		}
		endObject(type_array);
	}
}

void JsonOutput::printParallelStats(const SolveStats& stats) {
	if (stats.parallel) {
		const ParallelStats& ps = *stats.parallel;
		startObject("Basic");
		printKeyValue("CPU Time", ps.cpuTime);
		printKeyValue("Models", stats.models);
		printKeyValue("Choices", stats.choices);
		printKeyValue("Conflicts", stats.conflicts);
		printKeyValue("Restarts", stats.restarts);
		printKeyValue("Problems", ps.gps);
		printKeyValue("AvgGPLength", average(ps.gpLits, ps.gps));
		printKeyValue("Splits", ps.splits);
		printKeyValue("Distributed", ps.distributed);
		endObject();
	}
}

}

using namespace Clasp;
#if WITH_CLASPRE
#include <program_opts/program_options.h>
#include <program_opts/typed_value.h>
#include <sstream>
namespace Claspre {
void Options::initOptions(ProgramOptions::OptionContext& root) {
	using namespace ProgramOptions;
	OptionGroup pre("Claspre Options");
	pre.addOptions()
		("list-features"   , flag(listFeatures),"Print feature names (in --features=C1 format)")
		("features!"   , storeTo(features, &Options::mapFormat),
			"Print features in selected format\n"
			"      %A: {V|C1|C2|C3}\n"
			"        V : Print features in human-readable format\n"
			"        C1: Print in compact format; iterated features in separate lines\n"
			"        C2: Print in compact format; iterated features all in one line\n"
			"        C3: Print in C2 format but only if search state is unknown on exit")
	;
	root.add(pre);
}
bool Options::validateOptions(const ProgramOptions::ParsedOptions& vm) { 
	hasLimit = vm.count("search-limit") != 0;
	return true; 
}

void Options::printFeatures() const {
	printf("maxLearnt,maxConflicts,Constraints,LearntConstraints,FreeVars,Vars/FreeVars,FreeVars/Constraints,Vars/Constraints,maxLearnt/Constraints\n");
	printf(
		"Completed,,Atoms,_Original,_Auxiliary,Rules,_Basic,_Constraint,_Choice,_Weight,NormalRules/ExtRules"
		",Bodies,Equivalences,_Atom=Atom,_Body=Body,_Other,Tight,_SCCs,_Nodes,Variables,_Eliminated,Constraints,_Binary,_Ternary,_Other"
		",Models,Choices,Conflicts,Restarts,Constraints deleted,Backtracks,Backjumps,_Bounded,Skippable Levels,Levels skipped,_%%,Max Jump Length"
		",_Executed,Max Bound Length,Average Jump Length,_Executed,Average Bound Length,Average Model Length,Lemmas,_Binary,_Ternary,_Other,Conflicts,_Average Length,Loops,_Average Length\n"
	);
}

bool Options::mapFormat(const std::string& s, int& format) {
	if (s.size() == 1 && s[0] == 'V' || s[0] == 'v') {
		format = features_verbose; 
		return true;
	}
	if (s.size() == 2 && s[0] == 'C' || s[0] == 'c') {
		if (s[1] == '1') { format = features_compact_1; return true; }
		if (s[1] == '2') { format = features_compact_2; return true; }
		if (s[1] == '3') { format = features_compact_3; return true; }
	}
	return s == "no" && (format = features_no, true);
}

/////////////////////////////////////////////////////////////////////////////////////////
// ClaspreOutput
/////////////////////////////////////////////////////////////////////////////////////////
class ClaspreOutput : public DefaultOutput {
public:
	ClaspreOutput(Options::FeatureFormat f, int v, const std::pair<int,int>& q, DefaultOutput::Format outf);
	void initSolve(const Solver& s, ProgramBuilder* api);
	void printResult(const RunSummary& sol, const SolveStats** st, std::size_t num);
	void reportProgress(const PreprocessEvent&) {}
	void reportProgress(const SolveEvent& ev);
	void reportState(int state, bool enter, double time) {
		if (format_ ==  Options::features_verbose) {
			DefaultOutput::reportState(state, enter, time);
		}
	}
private:
	std::string iterationBuffer_;
	Options::FeatureFormat format_;
	uint32 starts_;
};

ClaspreOutput::ClaspreOutput(Options::FeatureFormat f, int v, const std::pair<int,int>& q, DefaultOutput::Format outf) 
	: DefaultOutput(v, q, outf), format_(f), starts_(0) {
}

void ClaspreOutput::initSolve(const Solver& s, ProgramBuilder* api) {
	DefaultOutput::initSolve(s, api);
	starts_          = 0;
	iterationBuffer_ = "";
	if (format_ != Options::features_verbose) {
		int v = (format_ == Options::features_compact_3);
		setVerbosity(v);
	}
}

void ClaspreOutput::reportProgress(const SolveEvent& e) {
	if (e.type != SolveEvent_t::progress_path || static_cast<const SolvePathEvent&>(e).evType != SolvePathEvent::event_restart) { return; }
	const SolvePathEvent& ev = static_cast<const SolvePathEvent&>(e);
	std::stringstream temp;
	const Solver& s = *ev.solver;
	uint64 maxC     = ev.cLimit;
	uint32 maxL     = ev.lLimit;
	if (format_ == Options::features_verbose) {
		printf("\n[Iteration %u]\n", starts_);
		printf("%-22s: %u\n", "maxLearnt", maxL);
		printf("%-22s: %" PRIu64"\n", "maxConflicts", maxC);
		printf("%-22s: %u\n", "Constraints", s.numConstraints());
		printf("%-22s: %u\n", "LearntConstraints", s.numLearntConstraints());
		printf("%-22s: %u\n", "FreeVars", s.numFreeVars());
		printf("%-22s: %.3f\n", "Vars/FreeVars", ((double)s.numVars())/std::max(s.numFreeVars(),uint32(1)));
		printf("%-22s: %.3f\n", "FreeVars/Constraints", ((double)s.numFreeVars())/s.numConstraints());
		printf("%-22s: %.3f\n", "Vars/Constraints", ((double)s.numVars())/s.numConstraints());
		printf("%-22s: %.3f\n", "maxLearnt/Constraints", ((double)maxL)/s.numConstraints());
		if (s.stats.jumps) {
			printf("%-22s: %.3f\n", "Average Jump Length", average(s.stats.jumps->jumpSum, s.stats.analyzed));
		}
		printf("=================================\n\n");
	}
	else {
		uint32 numC = std::max(s.numConstraints(), uint32(1));
		temp.precision(3);
		temp << std::fixed << maxL << "," << maxC << "," << s.numConstraints() << "," << s.numLearntConstraints() << "," << s.numFreeVars() << ","
			   << (static_cast<double>(s.numVars())/std::max(s.numFreeVars(), uint32(1))) << ","
				 << (static_cast<double>(s.numFreeVars())/numC) << ","
				 << (static_cast<double>(s.numVars())/numC) << ","
				 << (static_cast<double>(maxL)/numC);
		if (format_ == Options::features_compact_1) {
			printf("%s\n", temp.str().c_str());
		}
		else {
			if (!iterationBuffer_.empty()) iterationBuffer_ += ",";
			iterationBuffer_ += temp.str();
		}
	}
	++starts_;
}

void ClaspreOutput::printResult(const RunSummary& sol, const SolveStats** stats, std::size_t num) {
	bool hasSolution = sol.complete() || sol.models() > 0;
	if (format_ == Options::features_verbose) {
		DefaultOutput::printResult(sol, stats, num);
	}
	else {
		if (starts_ == 0) {
			Solver empty;
			reportProgress(SolvePathEvent(empty, SolvePathEvent::event_restart, 0, 0));
		}
		if (!iterationBuffer_.empty()) {
			printf("\n%s\n", iterationBuffer_.c_str());
		}
		const PreproStats& ps = lpStats() ? *lpStats() : PreproStats();
		uint32 auxAtoms = ps.trStats ? ps.trStats->auxAtoms : 0;
		uint32 orgAtoms = ps.atoms-auxAtoms;
		uint32 extRules = ps.rules[2] + ps.rules[5];
		if (ps.trStats) {
			extRules     -= (ps.trStats->rules[2]+ps.trStats->rules[5]);
		}
		if (!extRules) extRules = 1;
		printf("%s,%u,%u,%u,%u,%u,%u,%u,%u,%.3f", (hasSolution ? "Yes":"No"), ps.atoms, orgAtoms, auxAtoms, ps.rules[0], ps.rules[1]
			, ps.rules[2]-(ps.trStats?ps.trStats->rules[2]: 0)
			, ps.rules[3]-(ps.trStats?ps.trStats->rules[3]: 0)
			, ps.rules[5]-(ps.trStats?ps.trStats->rules[5]: 0)
			, ps.rules[1] / static_cast<double>(extRules));
		uint32 tight = ps.sccs == 0 || ps.sccs == PrgNode::noScc;
		uint32 sccs  = ps.sccs != PrgNode::noScc ? ps.sccs : 0;
		printf(",%u,%u,%u,%u,%u,%u,%u,%u,%u,%u,%u,%.3f,%.3f,%.3f", ps.bodies, ps.sumEqs(), ps.numEqs(Var_t::atom_var), ps.numEqs(Var_t::body_var), ps.numEqs(Var_t::atom_body_var)
			, tight, sccs, ps.ufsNodes, sol.ctx.stats().vars, sol.ctx.stats().vars_eliminated
			, sol.ctx.stats().constraints
			, percent(sol.ctx.stats().constraints_binary, sol.ctx.stats().constraints)
			, percent(sol.ctx.stats().constraints_ternary, sol.ctx.stats().constraints)
			, percent(sol.ctx.stats().constraints - (sol.ctx.stats().constraints_binary+sol.ctx.stats().constraints_ternary), sol.ctx.stats().constraints));
		if (num > 0) {
			std::stringstream temp;
			const SolveStats& st  = *stats[0];
			const JumpStats& js   = st.jumps ? *st.jumps   : JumpStats();
			uint64 learntSum      = std::accumulate(st.learnts, st.learnts+Constraint_t::max_value, uint64(0));
			temp.precision(3);
			temp << std::fixed << "," << st.models << "," << st.choices << "," << st.conflicts << "," << st.restarts << "," << st.deleted << "," 
					 << (st.conflicts-st.analyzed) << "," << st.analyzed << "," << js.bJumps << ","
					 << js.jumpSum << "," << (js.jumpSum - js.boundSum) << "," << percent(js.jumpSum - js.boundSum, js.jumpSum) << ","
					 << js.maxJump << "," << js.maxJumpEx << "," << js.maxBound << "," << average(js.jumpSum, st.analyzed) << "," << average(js.jumpSum-js.boundSum, st.analyzed) << ","
					 << average(js.boundSum,js.bJumps) << "," << average(js.modLits,st.models) << ","
					 << learntSum << "," << percent(st.binary, learntSum) << "," << percent(st.ternary, learntSum) << "," 
					 << percent(learntSum-st.binary-st.ternary, learntSum) << ","
					 << st.learnts[0] << "," << average(st.lits[0], st.learnts[0]) << ","
					 << st.learnts[1] << "," << average(st.lits[1], st.learnts[1]);
			printf("%s\n\n", temp.str().c_str());
		}
		if (format_ == Options::features_compact_3 && hasSolution) {
			DefaultOutput::printResult(sol, stats, 0);
		}
	}
}

OutputFormat* Options::createOutput(uint32 v, DefaultOutput::PrintPair q, DefaultOutput::Format f) { 
	if (features != Options::features_verbose) {
		v = 0;
		if (features != Options::features_compact_3) {
			q.first = q.second = DefaultOutput::print_no;
		}
	}
	return new ClaspreOutput(static_cast<Options::FeatureFormat>(features), v, q, f);
}

} // namespace Claspre
#else
namespace Claspre {
void Options::initOptions(ProgramOptions::OptionContext&) {}
bool Options::validateOptions(const ProgramOptions::ParsedOptions&) { return true; }
void Options::printFeatures() const {}
OutputFormat* Options::createOutput(uint32, DefaultOutput::PrintPair, DefaultOutput::Format) { return 0; }
}
#endif
