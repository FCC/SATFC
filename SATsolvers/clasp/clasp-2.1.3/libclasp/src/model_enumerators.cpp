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

#include <clasp/model_enumerators.h>
#include <clasp/solver.h>
#include <clasp/minimize_constraint.h>
#include <clasp/util/multi_queue.h>
#include <algorithm>
namespace Clasp {

ModelEnumerator::ModelEnumerator(Report* p)
	: Enumerator(p)
	, project_(0) {
}

ModelEnumerator::~ModelEnumerator() {
	delete project_;
}

void ModelEnumerator::setEnableProjection(bool b) {
	delete project_;
	if (b) project_ = new VarVec();
	else   project_ = 0;
}

void ModelEnumerator::initContext(SharedContext& ctx) {
	assert(ctx.master());
	if (project_) {
		const SymbolTable& index = ctx.symTab();
		if (index.type() == SymbolTable::map_indirect) {
			for (SymbolTable::const_iterator it = index.curBegin(); it != index.end(); ++it) {
				if (!it->second.name.empty() && it->second.name[0] != '_') {
					addProjectVar(ctx, it->second.lit.var());
				}
			}
			std::sort(project_->begin(), project_->end());
			project_->erase(std::unique(project_->begin(), project_->end()), project_->end());
		}
		else {
			for (Var v = 1; v < index.size(); ++v) { addProjectVar(ctx, v); }
		}
		if (project_->empty()) { 
			// We project to the empty set. Add true-var so that 
			// we can distinguish this case from unprojected search
			project_->push_back(0);
		}
	}
}

void ModelEnumerator::addProjectVar(SharedContext& ctx, Var v) {
	if (ctx.master()->value(v) == value_free) {
		project_->push_back(v);
		ctx.setFrozen(v, true);
		ctx.setProject(v, true);
	}
}

bool ModelEnumerator::backtrack(Solver& s) {
	uint32 bl = getHighestActiveLevel();
	if (projectionEnabled()) {
		bl = std::min(bl, getProjectLevel(s));
	}
	if (bl <= s.rootLevel()) {
		return false;
	}
	return doBacktrack(s, bl-1);
}

bool ModelEnumerator::ignoreSymmetric() const {
	return projectionEnabled() || Enumerator::ignoreSymmetric();
}

/////////////////////////////////////////////////////////////////////////////////////////
// class BacktrackEnumerator
/////////////////////////////////////////////////////////////////////////////////////////
BacktrackEnumerator::BacktrackEnumerator(uint32 opts, Report* p)
	: ModelEnumerator(p)
	, projectOpts_((uint8)std::min(uint32(7), opts)) {
}


Enumerator::EnumeratorConstraint* BacktrackEnumerator::doInit(SharedContext& ctx, uint32 t, bool start) {
	if (start) {
		ModelEnumerator::initContext(ctx);
	}
	else if (projectionEnabled() || t > 1) {
		return new LocalConstraint();
	}
	return 0;
}

uint32 BacktrackEnumerator::getProjectLevel(Solver& s) {
	uint32 maxL = 0;
	for (uint32 i = 0; i != numProjectionVars() && maxL != s.decisionLevel(); ++i) {
		if (s.level(projectVar(i)) > maxL) {
			maxL = s.level(projectVar(i));
		}
	}
	return maxL;
}

uint32 BacktrackEnumerator::getHighestBacktrackLevel(const Solver& s, uint32 bl) const {
	if (!projectionEnabled() || (projectOpts_ & MINIMIZE_BACKJUMPING) == 0) {
		return s.backtrackLevel();
	}
	uint32 res = s.backtrackLevel();
	for (uint32 r = res+1; r <= bl; ++r) {
		if (!s.sharedContext()->project(s.decision(r).var())) {
			return res;
		}
		++res;
	}
	return res;
}

bool BacktrackEnumerator::updateModel(Solver&) {  return projectionEnabled() || enumerated == 0; }

bool BacktrackEnumerator::doBacktrack(Solver& s, uint32 bl) {
	// bl is the decision level on which search should proceed.
	// bl + 1 the minimum of:
	//  a) the highest DL on which one of the projected vars was assigned
	//  b) the highest DL on which one of the vars in a minimize statement was assigned
	//  c) the current decision level
	assert(bl >= s.rootLevel());
	++bl; 
	assert(bl <= s.decisionLevel());
	uint32 btLevel = getHighestBacktrackLevel(s, bl);
	if (!projectionEnabled() || bl <= btLevel) {
		// If we do not project or one of the projection vars is already on the backtracking level
		// proceed with simple backtracking.
		s.setBacktrackLevel(bl);
		s.undoUntil(bl);
		return s.backtrack();
	}
	else if (numProjectionVars() == 1u) {
		Literal x = s.trueLit(projectVar(0));
		s.undoUntil(0);
		s.addUnary(~x, Constraint_t::static_constraint); // force the complement of x
		s.setBacktrackLevel(s.decisionLevel());
	}
	else {
		// Store the current projected assignment as a nogood
		// and attach it to the current decision level.
		// Once the solver goes above that level, the nogood is automatically
		// destroyed. Hence, the number of projection nogoods is linear in the
		// number of (projection) atoms.
		if ( (projectOpts_ & ENABLE_PROGRESS_SAVING) != 0 ) {
			s.strategies().saveProgress = 1;
		}
		projAssign_.clear();
		projAssign_.resize(numProjectionVars());
		LitVec::size_type front = 0;
		LitVec::size_type back  = numProjectionVars();
		for (uint32 i = 0; i != numProjectionVars(); ++i) {
			Literal x = ~s.trueLit(projectVar(i)); // Note: complement because we store the nogood as a clause!
			if (s.level(x.var()) > btLevel) {
				projAssign_[front++] = x;
			}
			else if (s.level(x.var()) != 0) {
				projAssign_[--back] = x;
			}
			else {
				projAssign_[--back] = projAssign_.back();
				projAssign_.pop_back();
			}
		}
		s.undoUntil( btLevel );
		Literal x = projAssign_[0];
		LearntConstraint* c;
		ClauseInfo e(Constraint_t::learnt_conflict);
		if (front == 1) {
			// The projection nogood is unit. Force the single remaining literal
			// from the current DL. 
			++back; // so that the active part of the nogood contains at least two literals
			c = Clause::newContractedClause(s, projAssign_, e, back, false);
			s.force(x, c);
		}
		else {
			// Shorten the projection nogood by assuming one of its literals...
			if ( (projectOpts_ & ENABLE_HEURISTIC_SELECT) != 0 ) {
				x = s.heuristic()->selectRange(s, &projAssign_[0], &projAssign_[0]+back);
			}
			assert(s.value(projAssign_[1].var()) == value_free);
			c = Clause::newContractedClause(s, projAssign_, e, back, false);
			// to false.
			s.assume(~x);
		}
		if (s.backtrackLevel() < s.decisionLevel()) {
			// Remember that we must backtrack the current decision
			// level in order to guarantee a different projected solution.
			s.setBacktrackLevel(s.decisionLevel());
		}
		static_cast<LocalConstraint*>(s.getEnumerationConstraint())->add(s, c);
		assert(s.backtrackLevel() == s.decisionLevel());
	}
	return true;
}

bool BacktrackEnumerator::updateConstraint(Solver& s, bool disjoint) {
	if (disjoint) return true;
	s.setStopConflict();
	return false;
}
/////////////////////////////////////////////////////////////////////////////////////////
// class BacktrackEnumerator::LocalConstraint
/////////////////////////////////////////////////////////////////////////////////////////
void BacktrackEnumerator::LocalConstraint::destroy(Solver* s, bool detach) {
	while (!nogoods.empty()) {
		Constraint* c = nogoods.back().first;
		nogoods.pop_back();
		c->destroy(s, detach);
	}
	EnumeratorConstraint::destroy(s, detach);
}
void BacktrackEnumerator::LocalConstraint::add(Solver& s, LearntConstraint* c) {
	if (s.decisionLevel() != 0) {
		// Attach nogood to the current decision literal. 
		// Once the solver goes above that level, the nogood (which is then satisfied) is destroyed.
		s.addUndoWatch(s.decisionLevel(), this);
	}
	nogoods.push_back( NogoodPair(c, s.decisionLevel()) );
}
void BacktrackEnumerator::LocalConstraint::undoLevel(Solver& s) {
	while (!nogoods.empty() && nogoods.back().second >= s.decisionLevel()) {
		Constraint* c = nogoods.back().first;
		nogoods.pop_back();
		c->destroy(&s, true);
	}
}

Constraint* BacktrackEnumerator::LocalConstraint::cloneAttach(Solver& other) {
	LocalConstraint* ret = new LocalConstraint();
	ret->attach(other);
	return ret;
}

/////////////////////////////////////////////////////////////////////////////////////////
// class RecordEnumerator::SolutionQueue
/////////////////////////////////////////////////////////////////////////////////////////
class RecordEnumerator::SolutionQueue : public mt::MultiQueue<SL*, void (*)(SL*)> {
public:
	typedef mt::MultiQueue<SL*, void (*)(SL*)> base_type;
	SolutionQueue(uint32 m) : base_type(m, releaseLits) {}
	void addSolution(SL* solution, ThreadId& id);
	static void releaseLits(SL* x);
};
void RecordEnumerator::SolutionQueue::addSolution(SL* solution, ThreadId& id) {
	assert(hasItems(id) == false);
	Node* n = allocate(maxQ()-1, solution);
	publishRelaxed(n);
	id      = n;
}
void RecordEnumerator::SolutionQueue::releaseLits(SL* x) {  
	x->release(); 
}
/////////////////////////////////////////////////////////////////////////////////////////
// class RecordEnumerator::RecordConstraint
/////////////////////////////////////////////////////////////////////////////////////////
class RecordEnumerator::LocalConstraint : public EnumeratorConstraint {
public:
	typedef SolutionQueue::ThreadId ThreadId;
	typedef ClauseCreator::Result   Result;
	LocalConstraint(SolutionQueue* q);
	bool   simplify(Solver& s, bool);
	void   destroy(Solver* s, bool detach);
	bool   add(Solver& s, LitVec& lits, const ClauseInfo& e);
	bool   integrateAll(Solver& s);
	Result integrate(Solver& s, SharedLiterals* lits, uint32 flags);
	uint32 flags() const {
		return ClauseCreator::clause_no_add
			| ClauseCreator::clause_no_release
			| ClauseCreator::clause_explicit;
	}
	Constraint* cloneAttach(Solver& other);
	PodVector<Constraint*>::type db;
	SolutionQueue* queue;
	ThreadId       id;
};
RecordEnumerator::LocalConstraint::LocalConstraint(SolutionQueue* q) : queue(q), id(0) {
	if (queue) { id = queue->addThread(); }
}
bool RecordEnumerator::LocalConstraint::simplify(Solver& s, bool) {
	s.simplifyDB(db);
	return false;
}
void RecordEnumerator::LocalConstraint::destroy(Solver* s, bool detach) {
	while (!db.empty()) {
		db.back()->destroy(s, detach);
		db.pop_back();
	}
	EnumeratorConstraint::destroy(s, detach);
}

Constraint* RecordEnumerator::LocalConstraint::cloneAttach(Solver& other) {
	LocalConstraint* ret = new LocalConstraint(queue);
	ret->attach(other);
	return ret;
}

bool RecordEnumerator::LocalConstraint::add(Solver& s, LitVec& lits, const ClauseInfo& e) {
	ClauseCreator::Result ret;
	if (!queue) {
		ret = ClauseCreator::create(s, lits, ClauseCreator::clause_no_add|ClauseCreator::clause_known_order, e);
		if (ret.local) { db.push_back(ret.local); }
	}
	else {
		// parallel solving active - share solution literals
		// with other solvers
		SL* shared = SL::newShareable(lits, e.type());
		queue->addSolution(shared, id);
		// and add local representation to solver
		ret = integrate(s, shared, flags()|ClauseCreator::clause_known_order);
	}
	return ret.ok() || s.resolveConflict();
}

bool RecordEnumerator::LocalConstraint::integrateAll(Solver& s) {
	if (queue) {
		uint32 f = flags();
		for (SL* clause; queue->tryConsume(id, clause); ) {
			if (!integrate(s, clause, f)) return false;
		}
	}
	return true;
}

ClauseCreator::Result RecordEnumerator::LocalConstraint::integrate(Solver& s, SL* clause, uint32 f) {
	ClauseCreator::Result res = ClauseCreator::integrate(s, clause, f);
	if (res.local) { db.push_back(res.local); }
	return res;
}

/////////////////////////////////////////////////////////////////////////////////////////
// class RecordEnumerator
/////////////////////////////////////////////////////////////////////////////////////////
RecordEnumerator::RecordEnumerator(Report* p)
	: ModelEnumerator(p)
	, queue_(0) {
}

RecordEnumerator::~RecordEnumerator() {
	delete queue_;
}

Enumerator::EnumeratorConstraint* RecordEnumerator::doInit(SharedContext& ctx, uint32 t, bool start) {
	if (start) {
		ModelEnumerator::initContext(ctx);
		return 0;
	}
	delete queue_;
	queue_ = 0;
	if (t > 1 && (!optimize() || projectionEnabled())) {
		queue_ = new SolutionQueue(t); 
		queue_->reserve(t);
	}
	return new LocalConstraint(queue_);
}

uint32 RecordEnumerator::getProjectLevel(Solver& s) {
	solution_.clear(); solution_.setSolver(s);
	solution_.start(Constraint_t::learnt_conflict);
	for (uint32 i = 0; i != numProjectionVars(); ++i) {
		solution_.add( ~s.trueLit(projectVar(i))  );
	}
	return !solution_.empty() 
		? s.level( solution_[0].var() )
		: 0;
}

uint32 RecordEnumerator::assertionLevel(const Solver& s) {
	if (solution_.empty())      return s.decisionLevel();
	return solution_.implicationLevel();
}

void RecordEnumerator::createSolutionNogood(Solver& s) {
	solution_.clear(); solution_.setSolver(s);
	if (optimize()) return;
	solution_.start(Constraint_t::learnt_conflict);
	for (uint32 x = s.decisionLevel(); x != 0; --x) {
		solution_.add(~s.decision(x));
	}
}

bool RecordEnumerator::doBacktrack(Solver& s, uint32 bl) {
	assert(bl >= s.rootLevel());
	if (!projectionEnabled()) {
		createSolutionNogood(s);
	}
	bl = std::min(bl, assertionLevel(s));
	s.undoUntil(bl, true);
	if (solution_.empty()) {
		assert(optimize());
		return true;
	}
	LocalConstraint* c  = static_cast<LocalConstraint*>(s.getEnumerationConstraint());
	return c->add(s, solution_.lits(), ClauseInfo(Constraint_t::learnt_other));
}

bool RecordEnumerator::updateModel(Solver&) { 
	return queue_ != 0;
}
bool RecordEnumerator::updateConstraint(Solver& s, bool) {
	LocalConstraint* con = static_cast<LocalConstraint*>(s.getEnumerationConstraint());
	return con->integrateAll(s);
}

}
