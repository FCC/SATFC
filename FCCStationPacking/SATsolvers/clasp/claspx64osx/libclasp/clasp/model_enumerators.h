// 
// Copyright (c) 2006-2011, Benjamin Kaufmann
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
#ifndef CLASP_MODEL_ENUMERATORS_H
#define CLASP_MODEL_ENUMERATORS_H

#ifdef _MSC_VER
#pragma once
#endif

#include <clasp/enumerator.h>
#include <clasp/clause.h>

namespace Clasp { 

//! Common base class for model enumeration with minimization and projection
/*!
 * \ingroup enumerator
 */
class ModelEnumerator : public Enumerator {
public:
	/*! 
	 * \param p The printer to use for outputting results.
	 */
	explicit ModelEnumerator(Enumerator::Report* p = 0);
	~ModelEnumerator();
	//! Enables/disbales projective solution enumeration.
	/*!
	 * \note Must be called before Enumerator::init() is called.
	 */
	void     setEnableProjection(bool b);
protected:
	bool backtrack(Solver& s);
	bool ignoreSymmetric() const;
	bool projectionEnabled() const { return project_ != 0; }
	void initContext(SharedContext& ctx);
	void addProjectVar(SharedContext& ctx, Var v);
	virtual bool   doBacktrack(Solver& s, uint32 bl) = 0;
	virtual uint32 getProjectLevel(Solver& s) = 0;
	uint32         numProjectionVars() const { assert(projectionEnabled()); return (uint32)project_->size(); }
	Var            projectVar(uint32 i)const { assert(projectionEnabled()); return (*project_)[i]; }	
private:
	VarVec* project_;
};

//! Backtrack based model enumeration
/*!
 * \ingroup enumerator
 *
 * This class enumerates models by maintaining a special backtracking level 
 * and by suppressing certain backjumps. 
 * For normal model enumeration and minimization no extra nogoods are created.
 * For projection, the number of additionally needed nogoods is linear in the number of
 * projection atoms.
 */
class BacktrackEnumerator : public ModelEnumerator {
public:
	/*! \copydoc ModelEnumerator::ModelEnumerator(ModelPrinter*)
	 * \params projectOpts An octal digit specifying the options to use if projection is enabled. 
	 *         The 3-bits of the octal digit have the following meaning:
	 *         - bit 0: use heuristic when selecting a literal from a projection nogood
	 *         - bit 1: enable progress saving after the first solution was found
	 *         - bit 2: minimize backjumps when backtracking from a solution
	 */
	BacktrackEnumerator(uint32 projectOpts, Enumerator::Report* p = 0);
	bool supportsRestarts() const { return optimize(); }
	bool supportsParallel() const { return !projectionEnabled(); }
private:
	enum ProjectOptions {
		ENABLE_HEURISTIC_SELECT = 1,
		ENABLE_PROGRESS_SAVING  = 2,
		MINIMIZE_BACKJUMPING    = 4
	};
	struct LocalConstraint : EnumeratorConstraint {
		typedef std::pair<Constraint*, uint32> NogoodPair;
		typedef PodVector<NogoodPair>::type Nogoods;
		void        destroy(Solver* s, bool detach);
		void        add(Solver& s, LearntConstraint* c);
		void        undoLevel(Solver& s);
		Constraint* cloneAttach(Solver& other);
		Nogoods nogoods;
	};
	EnumeratorConstraint* doInit(SharedContext& ctx, uint32 t, bool start);
	bool    updateModel(Solver& s);
	bool    updateConstraint(Solver& s, bool disjoint);
	bool    doBacktrack(Solver& s, uint32 bl);
	uint32  getProjectLevel(Solver& s);
	uint32  getHighestBacktrackLevel(const Solver& s, uint32 bl) const;
	LitVec  projAssign_;
	uint8   projectOpts_;
};

//! Recording based model enumeration
/*!
 * \ingroup enumerator
 * 
 * This class enumerates models by recording nogoods for found solutions.
 * For normal model enumeration and projection (with or without minimization) the
 * number of nogoods is bounded by the number of solutions.
 * For minimization (without projection) nogoods are only recorded if *all* optimal models are to be computed. 
 * In that case, the number of nogoods is bounded by the number of optimal models.
 *
 * If projection is not used, recorded solution nogoods only contain decision literals.
 * Otherwise, recorded solutions contain all projection literals.
 */
class RecordEnumerator : public ModelEnumerator {
public:
	/*! 
	 * \copydoc ModelEnumerator::ModelEnumerator(ModelPrinter*)
	 */
	RecordEnumerator(Enumerator::Report* p = 0);
	~RecordEnumerator();
private:
	class   SolutionQueue;
	class   LocalConstraint;
	typedef SharedLiterals SL;
	EnumeratorConstraint* doInit(SharedContext& ctx, uint32 t, bool start);
	bool   updateModel(Solver&);
	bool   updateConstraint(Solver& s, bool disjoint);
	bool   doBacktrack(Solver& s, uint32 bl);
	uint32 getProjectLevel(Solver& s);
	void   createSolutionNogood(Solver& s);
	uint32 assertionLevel(const Solver& s);
	ClauseCreator  solution_;
	SolutionQueue* queue_;
};
}
#endif
