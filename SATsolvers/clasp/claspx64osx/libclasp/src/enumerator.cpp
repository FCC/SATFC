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
#include <clasp/enumerator.h>
#include <clasp/minimize_constraint.h>
#include <clasp/solver.h>
namespace Clasp { 

/////////////////////////////////////////////////////////////////////////////////////////
// Enumerator::EnumeratorConstraint
/////////////////////////////////////////////////////////////////////////////////////////
Enumerator::EnumeratorConstraint::EnumeratorConstraint() : mini_(0), updates_(0) {}
void Enumerator::EnumeratorConstraint::attach(Solver& s) {
	Enumerator* e = s.sharedContext()->enumerator();
	if (e->mini_) {
		mini_ = e->mini_->attach(s);
		if (e->optimize()) { s.addWatch(s.sharedContext()->tagLiteral(), this); }
	}
}
Constraint* NullEnumerator::NullConstraint::cloneAttach(Clasp::Solver &o) {
	NullConstraint* c = new NullConstraint();
	return c->attach(o), c;
}
void Enumerator::EnumeratorConstraint::destroy(Solver* s, bool detach) {
	if (mini_) {
		mini_->destroy(s, detach);
		if (s) {
			s->removeWatch(s->sharedContext()->tagLiteral(), this);
			if (uint32 dl = s->isTrue(s->sharedContext()->tagLiteral()) ? s->level(s->sharedContext()->tagLiteral().var()) : 0) {
				s->removeUndoWatch(dl, this);
			}
		}
	}
	Constraint::destroy(s, detach);
}
Constraint::PropResult Enumerator::EnumeratorConstraint::propagate(Solver& s, Literal p, uint32&) { 
	PropResult r(true, true);
	if (mini_ && p == s.sharedContext()->tagLiteral() && s.decisionLevel() > 0) {
		// activate minimize constraint now that tag literal is assigned
		s.addUndoWatch(s.decisionLevel(), this);
		r.ok        = mini_->integrateNext(s);
	}
	return r;
}
void Enumerator::EnumeratorConstraint::undoLevel(Solver& s) {
	if (mini_ && !s.isTrue(s.sharedContext()->tagLiteral())) {
		// path complete and hierarchical optimization is active;
		// relax minimize constraint to avoid problems during simplification
		mini_->restoreOptimum();
	}
}
void Enumerator::EnumeratorConstraint::reason(Solver&, Literal, LitVec&) {}
/////////////////////////////////////////////////////////////////////////////////////////
// Enumerator
/////////////////////////////////////////////////////////////////////////////////////////
Enumerator::Report::Report()  {}
Enumerator::Enumerator(Report* r) : enumerated(0), numModels_(1), report_(r), mini_(0), restartOnModel_(false)  {}
Enumerator::~Enumerator()                               { if (mini_) mini_->destroy(); }
void Enumerator::setReport(Report* r)                   { report_   = r; }
void Enumerator::setRestartOnModel(bool r)              { restartOnModel_ = r; }
bool Enumerator::ignoreSymmetric() const                { return optimize(); }
bool Enumerator::optimize()        const                { return mini_ && mini_->mode() == MinimizeMode_t::optimize; }
bool Enumerator::optimizeHierarchical() const           { return mini_ && mini_->hierarchical(); }
bool Enumerator::optimizeNext()         const           { return optimizeHierarchical() && enumerated > 0 && mini_->optimizeNext(); }
void Enumerator::setMinimize(SharedMinimizeData* min)   { mini_ = min; }
void Enumerator::enumerate(uint64 m)                    { numModels_ = m; }
void Enumerator::startInit(SharedContext& ctx) { 
	enumerated = 0;
	updates_   = 0;
	doInit(ctx, 0, true);
}

Enumerator::EnumeratorConstraint* Enumerator::endInit(SharedContext& ctx, uint32 t) { 
	enumerated              = 0;
	updates_                = 0;
	EnumeratorConstraint* c = doInit(ctx, t, false);
	if (c == 0 && mini_)  c = new NullEnumerator::NullConstraint();
	if (mini_) { c->attach(*ctx.master()); }
	return c;
}
Enumerator::EnumeratorConstraint* Enumerator::constraint(const Solver& s) const {
	return static_cast<Enumerator::EnumeratorConstraint*>(s.getEnumerationConstraint());
}


bool Enumerator::continueFromModel(Solver& s, bool heu) {
	if (!optimize()) { return true; }
	s.strengthenConditional();
	MinimizeConstraint* min = constraint(s)->minimize();
	return !s.isTrue(s.sharedContext()->tagLiteral())
	  || (!s.hasConflict() && min->integrateNext(s) && (!heu || min->modelHeuristic(s)));
}

Enumerator::Result Enumerator::backtrackFromModel(Solver& s, bool callContinue) {
	assert(s.numFreeVars() == 0 && !s.hasConflict());
	bool update             = updateModel(s) || optimize();
	bool expandSym          = !ignoreSymmetric();
	activeLevel_            = mini_ ? constraint(s)->minimize()->commitCurrent(s)+1 : s.decisionLevel();
	Enumerator::Result    r = Enumerator::enumerate_continue;
	EnumeratorConstraint* c = constraint(s);
	do {
		++enumerated;
		s.stats.addModel(s.decisionLevel());
		if (report_) { 
			report_->reportModel(s, *this); 
		}
		if ((numModels_ != 0 && --numModels_ == 0) || terminated()) {
			// enough models enumerated
			return enumerate_stop_enough;
		}
		// Process symmetric models, i.e. models that differ only in the 
		// assignment of atoms outside of the solver's assignment. 
		// Typical example: vars eliminated by the SAT-preprocessor
	} while (expandSym && s.nextSymModel());
	if (activeLevel_ <= s.rootLevel() || !backtrack(s)) {
		r = enumerate_stop_complete;
		s.undoUntil(0, true);
	}
	else if (restartOnModel_) { s.undoUntil(0); }	
	if (update && s.sharedContext()->isShared()) {
		// enumerator is not trivial w.r.t current search scheme
		// force update of other solvers
		// - this one is up to date
		c->setUpdates(nextUpdate());
	}
	if (callContinue && !continueFromModel(s)) {
		r = enumerate_stop_complete;
	}
	return r;
}

bool Enumerator::update(Solver& s, bool disjoint) {
	EnumeratorConstraint* c = constraint(s);
	uint32 gUpdates;
	if (!c || c->updates() == (gUpdates = updates_)) {
		return true;
	}
	bool ret   = true;
	if (optimize()) {
		ret      = c->minimize()->integrateNext(s);
		disjoint = true; // enforced by minimize constraint
	}
	if (ret && (ret = updateConstraint(s, disjoint)) == true) {
		c->setUpdates(gUpdates);
	}
	return ret;
}

}
