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
#ifndef CLASP_CLASP_FACADE_H_INCLUDED
#define CLASP_CLASP_FACADE_H_INCLUDED

#ifdef _MSC_VER
#pragma warning (disable : 4200) // nonstandard extension used : zero-sized array
#pragma once
#endif

#if !defined(CLASP_VERSION)
#define CLASP_VERSION "2.1.3"
#endif
#if !defined(CLASP_LEGAL)
#define CLASP_LEGAL \
"Copyright (C) Benjamin Kaufmann\n"\
"License GPLv2+: GNU GPL version 2 or later <http://gnu.org/licenses/gpl.html>\n"\
"clasp is free software: you are free to change and redistribute it.\n"\
"There is NO WARRANTY, to the extent permitted by law."
#endif

#if !defined(WITH_THREADS)
#error Invalid thread configuration - use WITH_THREADS=0 for single-threaded or WITH_THREADS=1 for multi-threaded version of libclasp!
#endif

#if WITH_THREADS
#include <clasp/parallel_solve.h>
typedef Clasp::mt::ParallelSolveOptions SolveOptions;
#else
#include <clasp/solve_algorithms.h>
namespace Clasp {
struct SolveOptions {
	SolveLimits   limit;  /**< Solve limit (disabled by default). */
	void   createSolveObject(SolveAlgorithm*& out, SharedContext&, SolverConfig**) const { out = new SimpleSolve(limit); }
	static uint32 supportedSolvers()   { return 1; }
	static uint32 recommendedSolvers() { return 1; }
};
}
#endif
#include <clasp/literal.h>
#include <clasp/solver.h>
#include <clasp/enumerator.h>
#include <clasp/heuristics.h>
#include <clasp/lookahead.h>
#include <clasp/program_builder.h>
#include <clasp/unfounded_check.h>
#include <clasp/reader.h>
#include <clasp/util/misc_types.h>
#include <string>

/*!
 * \file 
 * This file provides a facade around the clasp library. 
 * I.e. a simplified interface for (incrementally) solving a problem using
 * some configuration (set of parameters).
 */
namespace Clasp {

/////////////////////////////////////////////////////////////////////////////////////////
// Parameter configuration
/////////////////////////////////////////////////////////////////////////////////////////	
//! Global options controlling global solving algorithms.
struct GlobalOptions {
public:
	typedef ProgramBuilder::EqOptions EqOptions;
	enum EnumMode { enum_auto = 0, enum_bt = 1, enum_record = 2, enum_consequences = 4, enum_brave = 5, enum_cautious = 6 };
	GlobalOptions();
	Enumerator*     createEnumerator(Enumerator::Report* r = 0);
	bool consequences() const { return enumerate.consequences(); }
	const char* cbType()const { return consequences() ? (enumerate.mode == enum_brave  ? "Brave" : "Cautious") : "none"; }
	SharedContext ctx;    /**< Context-object used by all solvers. */
	SolveOptions  solve;  /**< Additional options for solving. */
	EqOptions     eq;     /**< Options for equivalence preprocessing. */
	struct Optimize {     /**< Optimization options. */
		Optimize() : hierarch(0), no(false), all(false) {}
		SumVec vals;        /**< Initial values for optimize statements. */
		uint32 hierarch;    /**< Use hierarchical optimization scheme. */
		bool   no;          /**< Ignore optimize statements. */
		bool   all;         /**< Compute all models <= vals. */
	}             opt;
	struct EnumOptions {  /**< Enumeration options. */
		EnumOptions() : numModels(-1), mode(enum_auto), projectOpts(7), project(false)
		              , maxSat(false), restartOnModel(false), onlyPre(false)  {}
		bool consequences() const { return (mode & enum_consequences) != 0; }
		int       numModels;     /**< Number of models to compute. */
		EnumMode  mode;          /**< Enumeration type to use. */
		uint32    projectOpts;   /**< Options for projection. */
		bool      project;       /**< Enable projection. */
		bool      maxSat;        /**< Treat DIMACS input as MaxSat */
		bool      restartOnModel;/**< Restart after each model. */
		bool      onlyPre;       /**< Stop after preprocessing step? */
	}             enumerate;
};

class ClaspFacade;

//! Interface for controling incremental solving.
class IncrementalControl {
public:
	IncrementalControl();
	virtual ~IncrementalControl(); 
	//! Called before an incremental step is started.
	virtual void initStep(ClaspFacade& f)  = 0;
	//! Called after an incremental step finished.
	/*!
	 * \return
	 *  - true to signal that solving should continue with next step
	 *  - false to terminate the incremental solving loop
	 */
	virtual bool nextStep(ClaspFacade& f)  = 0;
private:
	IncrementalControl(const IncrementalControl&);
	IncrementalControl& operator=(const IncrementalControl&);
};

//! Parameter-object that groups & validates options.
class ClaspConfig : public GlobalOptions {
public:
	typedef DefaultUnfoundedCheck::ReasonStrategy LoopMode;
	typedef Lookahead::Type                       LookaheadType;
	typedef DecisionHeuristic                     Heuristic;
	enum HeuristicType { heu_none = 0, heu_berkmin = 1, heu_vsids = 2, heu_vmtf = 3, heu_unit = 4 };
	static Heuristic* createHeuristic(const SolverStrategies& str);
	ClaspConfig();
	~ClaspConfig();
	SolverConfig* getSolver(uint32 i) const { return solvers_.at(i);}
	SolverConfig* master()            const { return getSolver(0); }
	uint32        numSolvers()        const { return (uint32)solvers_.size(); }
	SolverConfig* addSolver()               { addSolver(new Solver()); return solvers_.back(); }
	SolverConfig**solvers()                 { return &solvers_[0]; }
	void          setMaxSolvers(uint32 i)   { removeSolvers(i); }
	void          reserveSolvers(uint32 num);
	uint32        removeSolvers(uint32 id);
	bool          validate(std::string& err);
	bool          validate(SolverConfig& sc, std::string& err);
	void          applyHeuristic();
	void          applyHeuristic(SolverConfig& sc);
	void          reset();
private:
	ClaspConfig(const ClaspConfig&);
	ClaspConfig& operator=(const ClaspConfig&);
	void          addSolver(Solver* s);
	typedef PodVector<SolverConfig*>::type SolverConfigs;
	SolverConfigs solvers_;
};
/////////////////////////////////////////////////////////////////////////////////////////
// ClaspFacade
/////////////////////////////////////////////////////////////////////////////////////////
//! Provides a simplified interface for (incrementally) solving a given problem.
class ClaspFacade : public Enumerator::Report {
public:
	//! Defines the possible solving states.
	enum State { 
		state_start,       /*!< Computation started. */
		state_read,        /*!< Problem is read from input. */
		state_preprocess,  /*!< Problem is prepared. */
		state_solve,       /*!< Search is active. */
		num_states
	};
	//! Defines important event types.
	enum Event { 
		event_state_enter, /*!< A new state was entered. */
		event_state_exit,  /*!< About to exit from the active state. */
		event_p_prepared,  /*!< Problem was transformed to nogoods. */
		event_model        /*!< A model was found. */
	};
	//! Defines possible solving results.
	enum Result { result_unsat, result_sat, result_unknown }; 
	//! Callback interface for notifying about important steps in solve process.
	class Callback {
	public:
		virtual ~Callback() {}
		//! State transition. Called on entering/exiting a state.
		/*!
		 * \param e Either event_state_enter or event_state_exit.
		 * \note Call f.state() to get the active state.
		 */
		virtual void state(Event e, ClaspFacade& f) = 0;
		//! Some operation triggered an important event.
		/*!
		 * \param s The solver that triggered the event.
		 * \param e An event that is neither event_state_enter nor event_state_exit.
		 */
		virtual void event(const Solver& s, Event e, ClaspFacade& f) = 0;
		//! Some configuration option is unsafe/unreasonable w.r.t the current problem.
		virtual void warning(const char* msg)       = 0;
	};
	ClaspFacade();
	/*!
	 * Solves the problem given in problem using the given configuration.
	 * \pre config is valid, i.e. config.valid() returned true
	 * \note Once solve() returned, the result of the computation can be
	 * queried via the function result().
	 * \note If config.onlyPre is true, solve() returns after
	 * the preprocessing step (i.e. once the solver is prepared) and does not start a search.
	 */
	void solve(Input& problem, ClaspConfig& config, Callback* c) { return solve(problem, config, 0, c); }

	/*!
	 * Incrementally solves the problem given in problem using the given configuration.
	 * \pre config is valid, i.e. config.valid() returned true
	 * \note Call result() to get the result of the computation.
	 * \note config.onlyPre is ignored in incremental setting!
	 *
	 * solveIncremental() runs a simple loop that is controlled by the
	 * given IncrementalControl object inc.
	 * \code
	 * do {
	 *   inc.initStep(*this);
	 *   read();
	 *   preprocess();
	 *   solve();
	 * } while (inc.nextStep(*this));
	 * \endcode
	 * 
	 */
	void solveIncremental(Input& problem, ClaspConfig& config, IncrementalControl& inc, Callback* c) { solve(problem, config, &inc, c); }

	//! Returns the result of a computation.
	Result result() const { return result_; }
	//! Returns true if search-space was completed. Otherwise false.
	bool   more()   const { return more_; }
	//! Returns the active state.
	State  state()  const { return state_; }
	//! Returns the current incremental step (starts at 0).
	int    step()   const { return step_; }
	//! Returns the current input problem.
	Input* input() const { return input_; }
	//! Tries to terminate an active search.
	bool   terminate() const { return ctrl_ && ctrl_->terminate(); }
	
	const ClaspConfig* config() const { return config_; }

	//! Returns the ProgramBuilder-object that was used to transform a logic program into nogoods.
	/*!
	 * \note A ProgramBuilder-object is only created if input()->format() == Input::SMODELS
	 * \note The ProgramBuilder-object is destroyed after the event
	 *       event_p_prepared was fired. Call releaseApi to disable auto-deletion of api.
	 *       In that case you must later manually delete it!
	 */
	ProgramBuilder* api() const  { return api_.get();     }
	ProgramBuilder* releaseApi() { return api_.release(); }

	void warning(const char* w) const { if (cb_) cb_->warning(w); }
private:
	ClaspFacade(const ClaspFacade&);
	ClaspFacade& operator=(const ClaspFacade&);
	void solve(Input& problem, ClaspConfig& config, IncrementalControl* inc, Callback* c);
	struct AutoState {
		AutoState(ClaspFacade* f, State s) : self_(f), state_(s) { self_->setState(s, event_state_enter); }
		~AutoState() { self_->setState(state_, event_state_exit); }
		ClaspFacade* self_;
		State        state_;
	};
	typedef SingleOwnerPtr<ProgramBuilder> Api;
	typedef SingleOwnerPtr<SharedDependencyGraph> GraphPtr;
	typedef SingleOwnerPtr<Enumerator>            EnumPtr;
	// -------------------------------------------------------------------------------------------  
	// Status information
	void setState(State s, Event e)          { state_ = s; if (cb_) cb_->state(e, *this); }
	void fireEvent(const Solver& s, Event e) { if (cb_) cb_->event(s, e, *this); }
	// -------------------------------------------------------------------------------------------
	// Enumerator::Report interface
	void reportModel(const Solver& s, const Enumerator&) {
		result_ = result_sat;
		fireEvent(s, event_model);
	}
	void reportSolution(const Enumerator& e, bool complete) {
		more_ = !complete;
		if (!more_ && e.enumerated == 0) {
			result_ = result_unsat;
		}
	}
	// -------------------------------------------------------------------------------------------
	// Internal setup functions
	void   validateWeak();
	void   validateWeak(ClaspConfig& cfg);
	void   init(Input&, ClaspConfig&, IncrementalControl*, Callback* c);
	bool   read();
	bool   preprocess();
	bool   solve(const LitVec& assume);
	bool   initEnumeration(SharedMinimizeData* min);
	bool   initContextObject(SharedContext& ctx) const;
	void   setGraph();
	// -------------------------------------------------------------------------------------------
	ClaspConfig*           config_;
	IncrementalControl*    inc_;
	Callback*              cb_;
	Input*                 input_;
	SolveAlgorithm*        ctrl_;
	GraphPtr               graph_;
	EnumPtr                enum_;
	Api                    api_;
	Result                 result_;
	State                  state_;
	int                    step_;
	bool                   more_;
};

}
#endif
