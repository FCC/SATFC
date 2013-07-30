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
#ifndef CLASP_ENUMERATOR_H_INCLUDED
#define CLASP_ENUMERATOR_H_INCLUDED

#ifdef _MSC_VER
#pragma once
#endif
#include <clasp/literal.h>
#include <clasp/constraint.h>
#include <clasp/util/misc_types.h>
#include <clasp/util/atomic.h>

namespace Clasp { 
class  Solver;
class  MinimizeConstraint;
struct SharedMinimizeData;
class  SharedContext;
class  SatPreprocessor;

/**
 * \defgroup enumerator Enumerators and related classes
 */
//@{

//! Interface for enumerating models.
/*!
 * Enumerators are global w.r.t to one search operation, that is
 * even if the search operation itself is a parallel search, there
 * shall be only one enumerator and it is the user's responsibility
 * to protect the enumerator with appropriate locking.
 *
 * Concrete enumerators may create and install an enumeration constraint 
 * in each solver to support (parallel) enumeration.
 */
class Enumerator {
public:
	//! Interface used by the Enumerator to report model events.
	class Report : public ProgressReport {
	public:
		Report();
		//! The solver has found a new model.
		virtual void reportModel(const Solver& /* s */, const Enumerator& /* self */) {}
		//! Enumeration has terminated.
		virtual void reportSolution(const Enumerator& /* self */, bool /* complete */) {}
	};
	//! A solver-local (i.e. thread-local) constraint to support enumeration.
	class EnumeratorConstraint : public Constraint {
	public:
		using Constraint::minimize;
		EnumeratorConstraint();
		//! Attaches to minimize constraint to use during enumeration.
		void   attach(Solver& s);
		//! Detaches and destroys minimize constraint if any.
		void   destroy(Solver* s, bool detach);
		MinimizeConstraint* minimize()  const { return mini_; }
		uint32 updates() const      { return updates_; }
		void   setUpdates(uint32 u) { updates_ = u; }
	protected:
		//! Activates minimize constraint on assignment of tag literal.
		PropResult propagate(Solver&, Literal, uint32&);
		//! Restores minimize constraint on undo of tag literal.
		void       undoLevel(Solver&);
		//! Noop.
		void       reason(Solver&, Literal, LitVec&);
	private:
		MinimizeConstraint* mini_;
		uint32              updates_;
	};
	
	explicit Enumerator(Report* r = 0);
	virtual ~Enumerator();
	
	//! How to continue after a model was found.
	enum Result {
		enumerate_continue,      /**< Continue enumeration. */
		enumerate_stop_complete, /**< Stop because search space is exceeded.  */
		enumerate_stop_enough    /**< Stop because enough models have been enumerated. */
	};

	//! Sets the report callback to be used during enumeration.
	/*!
	 * \note Ownership is *not* transferred and r must be valid
	 * during complete search.
	 */
	void setReport(Report* r);

	//! If true, does a search-restart every time a model is found.
	void setRestartOnModel(bool r);
	
	//! Starts initialization of this enumerator.
	/*!
	 * Must be called once before search is started and before solvers
	 * can be attached. Shall freeze relevant variables.
	 * \note In the incremental setting, startInit() must be called once for each incremental step.
	 */
	void startInit(SharedContext& ctx);

	//! Sets the (shared) minimize object to be used during enumeration.
	/*!
	 * \note Ownership is transferred.
	 */
	void setMinimize(SharedMinimizeData* min);
	const SharedMinimizeData* minimize() const { return mini_; }

	//! Sets the maximum number of models to enumerate.
	/*!
	 * \param numModels Number of models to enumerate (0 means all models).
	 */
	void enumerate(uint64 numModels);

	bool enumerate() const { return numModels_ != 1; }

	//! Completes the initialization of this enumerator.
	/*!
	 * Sets the number of solvers sharing this enumerator.
	 */
	EnumeratorConstraint* endInit(SharedContext& ctx, uint32 shareCount);
	//! Force termination of model enumeration in backtrackFromModel().
	void   terminate()        { fetch_and_or(updates_, uint32(1)); }
	bool   terminated() const { return (updates_ & 1u) != 0; }

	uint64 enumerated; /**< Number of models enumerated so far. */

	//! Returns the enumeration constraint attached to s.
	EnumeratorConstraint* constraint(const Solver& s) const;

	//! Processes and returns from a model stored in the given solver.
	/*!
	 * The return value determines how search should proceed.
	 * If enumerate_continue is returned, the enumerator has
	 * removed at least one decision level and search may
	 * continue from the new level.
	 * Otherwise, the search shall be stopped.
	 * \pre The solver contains a model and DL = s.decisionLevel()
	 * \post If enumerate_continue is returned:
	 *  - s.decisionLevel() < DL and s.decisionLevel() >= s.rootLevel()
	 *
	 * \note If enumerate_continue is returned and callContinue is false, 
	 *       the caller must call Enumerator::continueFromModel(s) before 
	 *       continuing enumeration.
	 * \note The function is not concurrency-safe, i.e. in a parallel search
	 *       at most one solver shall call the function at any one time.
	 */
	Result backtrackFromModel(Solver& s, bool callContinue = true);
	//! Continues enumeration after a model was found.
	/*!
	 * Adds new information from the recent model and strengthens
	 * any conditional knowledge in s.
	 * \pre backtrackFromModel(s) was called
	 * \return false if an unresolvable conflict was found.
	 *
	 * \note The function is concurrency-safe, i.e. can be called
	 *       concurrently by different solvers.
	 *
	 */
	bool continueFromModel(Solver& s, bool allowModelHeuristic = true);

	//! Updates solver s with new model-related information.
	/*!
	 * The function is used to transfer information in
	 * parallel search. Whenever a solver s1 found a new model
	 * (i.e. called backtrackFromModel()), all other solvers
	 * shall eventually call update() to incorporate any new information.
	 * \note The function is concurrency-safe, i.e. can be called
	 *       concurrently by different solvers.
	 */
	bool update(Solver& s, bool disjointPath);

	//! Called once after search has stopped.
	/*!
	 * The function notifies the installed Report object.
	 */
	void reportResult(bool complete) {
		if (report_) report_->reportSolution(*this, complete);
	}
	//! Returns true if optimization is active.
	bool   optimize() const;
	bool   optimizeHierarchical() const;
	//! Returns true if there is a next level to optimize.
	bool   optimizeNext() const;
	//! Returns whether or not this enumerator supports full restarts once a model was found.
	virtual bool supportsRestarts() const{ return true; }
	//! Returns whether or not this enumerator supports parallel search.
	virtual bool supportsParallel() const{ return true; }
protected:
	uint32 getHighestActiveLevel() const { return activeLevel_; }
	uint32 nextUpdate()                  { return (updates_ += 2); }
	virtual EnumeratorConstraint* doInit(SharedContext& s, uint32 numThreads, bool start) = 0;
	virtual bool backtrack(Solver& s) = 0;
	virtual bool updateConstraint(Solver& s, bool disjoint) = 0;
	virtual bool updateModel(Solver& s) = 0;
	virtual bool ignoreSymmetric() const;
private:
	Enumerator(const Enumerator&);
	Enumerator& operator=(const Enumerator&);
	uint64              numModels_;
	Report*             report_;
	SharedMinimizeData* mini_;
	std::atomic<uint32> updates_;
	uint32              activeLevel_;
	bool                restartOnModel_;
};

class NullEnumerator : public Enumerator {
public:
	NullEnumerator() {}
	class NullConstraint : public EnumeratorConstraint {
	public:
		Constraint* cloneAttach(Solver& o);
	};
private:
	EnumeratorConstraint* doInit(SharedContext&, uint32, bool) { return 0; }
	bool backtrack(Solver&)                                    { return false; }
	bool updateConstraint(Solver&, bool)                       { return true;  }
	bool updateModel(Solver&)                                  { return false; }
};

//@}

}

#endif
