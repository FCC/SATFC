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
#ifndef CLASP_OUTPUT_H_INCLUDED
#define CLASP_OUTPUT_H_INCLUDED

#include <clasp/program_builder.h> // for PreproStats
#include <clasp/enumerator.h>      // for Enumerator::ProgressReport

namespace Clasp {
struct SharedMinimizeData;
struct ProblemStats;

enum ExitCode {
	E_UNKNOWN       = 0,       /*!< satisfiablity of problem not knwon; search not started     */
	E_INTERRUPT     = 1,       /*!< run was interrupted                                        */
	E_SAT           = 10,      /*!< at least one model was found                               */
	E_EXHAUST       = 20,      /*!< search-space was completely examined                       */
	E_MEMORY        = 33,      /*!< run was interrupted by out of memory exception             */
	E_ERROR         = 65,      /*!< run was interrupted by internal error                      */
	E_NO_RUN        = 128      /*!< search not started because of syntax or command line error */
};

/*!
 * Base class for printing status and input format dependent information,
 * like models, optimization values, summaries, and program statistics.
 */
class OutputFormat : public ProgressReport {
public:
	explicit OutputFormat(uint32 verbosity);
	virtual ~OutputFormat();
	typedef PodVector<ValueRep>::type ValueVec;
	class Model {
	public:
		Model(const ValueRep* m, uint32 size) : model_(m), size_(size) {}
		uint32   size()       const { return size_; }
		ValueRep value(Var v) const { return model_[v]; }
	private:
		const ValueRep* model_;
		uint32          size_;
	};
	//! Type describing the result of a solver run
	struct RunSummary {
		//! Possible results
		enum Result   { 
			result_unknown = 0, /**< The satisfiability of the problem is still unknown */
			result_unsat   = 1, /**< The problem was found to be unsat */
			result_sat     = 2, /**< The problem is satisfiable (has at least one model) */
			result_optimum = 3, /**< The optimization problem is satisfiable and the optimal model was found */
			result_sat_opt = 4, /**< The optimization problem is satisfiable but the optimal model was not found */
			not_a_result   = 5 
		};
		RunSummary(const SharedContext& a_ctx) : ctx(a_ctx) {}
		const SharedContext& ctx;       /**< context object of the run */ 
		double               totalTime; /**< total wall clock time */
		double               solveTime; /**< time solving */
		double               modelTime; /**< time to first model */
		double               unsatTime; /**< time to prove unsat */
		double               cpuTime;   /**< total cpu time */
		const char*       consequences; /**< type of consequences computed or 0 */
		uint32               termId:30; /**< id of solver that terminated the algorithm */
		uint32               sig   : 1; /**< if 1, termId is actually the termination signal */
		uint32               comp  : 1;/**< search space exhausted? */
		int    termSig()const { return sig != 0 ? (int)termId : 0; }
		int    winner() const { return sig == 0 ? (int)termId : -1;}
		bool   complete()const{ return sig == 0 && comp != 0; }
		int    exitCode()const;
		Result result() const;
		uint64 models() const;
	private: RunSummary& operator=(const RunSummary&);
	};
	const char* result[RunSummary::not_a_result]; /**< Default result strings */
	//! Supported levels for printing models and optimize values
	enum PrintLevel { 
		print_f_default = -1,/**< Let selected format decide what to print  */ 
		print_all       = 0, /**< print all models or optimize values       */
		print_best      = 1, /**< only print last model or optimize value   */ 
		print_no        = 2, /**< do not print any models or optimize values*/
	};
	typedef std::pair<uint32, uint32> PrintPair;

	void init(const std::string& solver, const std::string& input) {
		solver_   = solver;
		input_    = input;
	}
	//! current solver name
	const std::string& solver()     const { return solver_; }
	//! current input
	const std::string& input()      const { return input_; }
	//! current verbosity level
	int                verbosity()  const { return verbosity_; }
	//! output any models?
	bool               quiet()      const { return quiet_ == 8; }
	//! print level for models
	int                modelQ()     const { return quiet_ / (print_no+1); }
	//! print level for optimization values
	int                optQ()       const { return quiet_ % (print_no+1); }
	
	//! Called once before solving starts but after preprocessing finished
	/*!
	 * \param s   the (master) solver to be used for solving
	 * \param api the program builder if a logic program is to be solved
	 */
	virtual void initSolve(const Solver& s, ProgramBuilder* api);
	
	//! Called on state enter and exit events
	/*!
	 * \param state One of the states defined by ClaspFacade
	 * \param enter true on entering state
	 * \param time if enter is false, time that was spent in state
	 */
	virtual void reportState(int state, bool enter, double time) = 0;
	
	/*!
	 * Called for each model. 
	 *
	 * Depending on the quiet level, the model is either
	 * printed, saved, or ignored.
	 * 
	 * \param s  The solver storing the current model
	 * \param en The active enumerator
	 *
	 * \see void printModel(const Model& m, const SymbolTable& index, const Enumerator& en)
	 */
	void reportModel(const Solver& s, const Enumerator& en);
	
	/*!
	 * Called for each set of consequences. 
	 *
	 * Depending on the quiet level, the model is either
	 * printed, saved, or ignored.
	 * 
	 * \param s  The solver storing the current model
	 * \param en The active enumerator
	 * \param cbType The type of consequences computed
	 * \see void printConsequences(const SymbolTable& index, const Enumerator& en, const char* cbType)
	 */
	void reportConsequences(const Solver& s, const Enumerator& en, const char* cbType);

	//! Called after search has stopped
	/*!
	 * Prints any saved model and calls 
	 * void printResult(const RunSummary& sol, const SolveStats** st, std::size_t num).
	 * \param sol object describing result of run
	 * \param st  An array of statistics of the last solve operation
	 * \param num The number of elements in st that should be reported
	 * \note if num > 0, st[0] is an accumulation of all following statistics
	 */
	void reportResult(const RunSummary& sol, const SolveStats** st, std::size_t num);
protected:
	void setVerbosity(int v) { verbosity_ = v; }
	void setQuiet(PrintPair q) { 
		q.first  = q.first == (uint32)print_f_default ? (uint32)print_all : std::min(q.first, (uint32)print_no);
		q.second = q.second== (uint32)print_f_default ? (uint32)print_all : std::min(q.second,(uint32)print_no);
		quiet_   = (q.first*(print_no+1))+q.second;
	}
	const PreproStats* lpStats() const { return lpStats_; }
	//! Shall output the model m
	virtual void printModel(const Model& m, const SymbolTable& index, const Enumerator& en) = 0;
	//! Shall output the consequences stored in index
	virtual void printConsequences(const SymbolTable& index, const Enumerator& en, const char* cbType) = 0;
	//! Shall print a result summary
	virtual void printResult(const RunSummary& sol, const SolveStats** st, std::size_t num) = 0;
	//! Shall print the optimum stored in m
	virtual void printOptimize(const SharedMinimizeData& m) = 0;
private:
	OutputFormat(const OutputFormat&);
	OutputFormat& operator=(const OutputFormat&);
	void  reportOptimize(const Enumerator& en, PrintLevel printLevel);
	Model storeModel(const Assignment& a);
	std::string solver_;    // current solver
	std::string input_;     // current input
	ValueVec    saved_;     // saved variable values
	ValueRep*   curr_;      // pointer to current model
	ValueRep*   next_;      // where to store next model
	PreproStats*lpStats_;   // stats from ProgramBuilder
	int         verbosity_; // verbosity level
	uint32      quiet_;     // quiet level encoded as two tribits: (modLevel*3)+optLevel
};

//! Default clasp format printer.
/*!
 * Prints all output to stdout in given format:
 * - format_asp prints in clasp's default asp format
 * - format_aspcomp prints in in ASP competition format
 * - format_sat09 prints in SAT-competition format
 * - format_pb09 in PB-competition format
 * .
 * \see https://www.mat.unical.it/aspcomp2011/files/LanguageSpecifications.pdf
 * \see http://www.satcompetition.org/2009/format-solvers2009.html
 * \see http://www.cril.univ-artois.fr/PB09/solver_req.html
 *
 */
class DefaultOutput : public OutputFormat {
public:
	enum Format { format_asp, format_aspcomp, format_sat09, format_pb09 };
	explicit DefaultOutput(uint32 v, const PrintPair& q, Format f, char ifs = ' ');
	~DefaultOutput();
	void reportProgress(const SolveEvent& ev);
	void reportProgress(const PreprocessEvent& ev);
	void reportState(int state, bool enter, double time);

	/*!
	 * Prints comment(1, "Answer: %"PRIu64"\n", en.enumerated).
	 * Then prints the model in the requested format, i.e.
	 * - if asp   : prints named atoms that are true in the current model
	 * - if sat09 : prints a null-terminated set of intergers representing the true literals
	 * - if pb09  : prints the set of true literals; variables are named x1...xn
	 */
	void printModel(const Model& m, const SymbolTable& index, const Enumerator& en);

	/*!
	 * comment(1, "%s consequences:\n", cbType).
	 * Then prints the current consequences (marked atoms) in active format
	 */
	void printConsequences(const SymbolTable& index, const Enumerator& en, const char* cbType);

	/*!
	 * prints the current optimum in a suitable foramt
	 */
	void printOptimize(const SharedMinimizeData& m);

	/*!
	 * Outputs the given summary and statistics.
	 * Always prints(format_[cat_result], result[sol.result()]) and
	 * any not yet printed models.
	 *
	 * If --verbose > 0 the summary furthermore consists of:
	 *   - the number of computed models m followed by a + if search was not completed
   *   - the number of enumerated models e if e != m
   *   - the state of any optimization and whether the last model was optimal
   *   - the state of consequence computation and whether the last model corresponded to the consequences
   *   - timing information
	 *
	 * Finally, if num > 0, statistics are printed.
	 *
	 * \note Summary and statistics are treated as belonging to the comment category.
	 */
	void printResult(const RunSummary& sol, const SolveStats** st, std::size_t num);
protected:
	enum CategoryKey { cat_comment, cat_value, cat_objective, cat_result, cat_value_term, cat_atom_pre, cat_atom_post, cat_eoc };
	// Hook for derived classes
	virtual void printExtendedModel(const Model&) const {}
	// ---
	const char*  format_[cat_eoc];
private:
	void printOptimizeValues(const SharedMinimizeData& m) const;
	void printSolveStats(const SolveStats& st) const;
	void printLemmaStats(const SolveStats& st) const;
	void printJumpStats(const SolveStats& st) const;
	void printThreadStats(const SolveStats** st, std::size_t num, const RunSummary& sol) const;
	void printParallelStats(const SolveStats& st, bool accu) const;
	void printProblemStats(const ProblemStats& st) const;
	void printLpStats(const PreproStats& st) const;
	void printStats(const RunSummary& sol, const SolveStats** st, std::size_t num);
	void comment(int v, const char* fmt, ...) const;
	void configureFormat(Format f, PrintPair q, char ifs);
	const SatPreprocessor* prepro_;
	const char*  as_;     // atom separator when printing models; derived from IFS_
	const char*  eom_;    // model terminator
	mutable int  w_;      // output width
	int          header_; // print header in verbose output?
	int          ev_;     // last event type
	char         IFS_;    // internal field separator
};

class JsonOutput : public OutputFormat {
public:
	explicit JsonOutput(uint32 v, const PrintPair& q);
	void reportState(int state, bool enter, double time);
	void printModel(const Model& m, const SymbolTable& index, const Enumerator& en);
	void printConsequences(const SymbolTable& index, const Enumerator& en, const char* cbType);
	void printOptimize(const SharedMinimizeData& m);
	void printResult(const RunSummary& sol, const SolveStats** st, std::size_t num);
private:
	void startModel();
	void printLemmaStats(const SolveStats& st);
	void printJumpStats(const SolveStats& st);
	void printThreadStats(const SolveStats** st, std::size_t num, const RunSummary& sol);
	void printParallelStats(const SolveStats& st);
	void printLpStats(const PreproStats& st);
	inline void printKey(const char* k);
	inline void printString(const char* s, const char* sep);
	inline void printKeyValue(const char* k, const char* v) ;
	inline void printKeyValue(const char* k, uint64 v);
	inline void printKeyValue(const char* k, uint32 v);
	inline void printKeyValue(const char* k, double d);
	enum ObjType { type_object, type_array };
	inline void startObject(const char* k = 0, ObjType t = type_object);
	inline void endObject(ObjType t = type_object);
	uint32 indent_;
	const char* open_;
	bool   hasModel_;
	bool   hasWitness_;
};
}

#if !defined(WITH_CLASPRE)
#define WITH_CLASPRE 0
#endif


namespace ProgramOptions {
class OptionContext;
class ParsedOptions;
}

namespace Claspre {
// Group "Claspre Options"
struct Options {
	static bool mapFormat(const std::string& s, int& f);
	Options() : features(features_no), listFeatures(false), hasLimit(false) {}
	enum FeatureFormat {
		features_no        = 0,
		features_verbose   = 1, // like claspre with --features
		features_compact_1 = 2, // like claspre with --claspfolio=1
		features_compact_2 = 3, // like claspre with --claspfolio=2
		features_compact_3 = 4  // like claspre with --claspfolio=3
	};
	void initOptions(ProgramOptions::OptionContext& root);
	bool validateOptions(const ProgramOptions::ParsedOptions& vm);
	void printFeatures() const;
	Clasp::OutputFormat* createOutput(uint32 v, Clasp::DefaultOutput::PrintPair q, Clasp::DefaultOutput::Format f);
	int  features;     // print feature in selected format
	bool listFeatures; // print available features
	bool hasLimit;     // true if "--search-limit" was given
};
}
#endif
