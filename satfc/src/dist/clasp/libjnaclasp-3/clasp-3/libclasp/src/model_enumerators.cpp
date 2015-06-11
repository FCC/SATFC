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
#include <cstdlib>
namespace Clasp {
class ModelEnumerator::ModelFinder : public EnumerationConstraint {
protected:
	ModelFinder(Solver& s, MinimizeConstraint* min, VarVec* p) : EnumerationConstraint(s,min), project(p) {}
	void destroy(Solver* s, bool detach) {
		destroyNogoods(s, detach);
		if (project && s && s->sharedContext()->master() == s) {
			SharedContext& problem = const_cast<SharedContext&>(*s->sharedContext());
			while (!project->empty()) {
				problem.setProject(project->back(), false);
				project->pop_back();
			}
		}
		delete project;
		EnumerationConstraint::destroy(s, detach);
	}
	bool simplify(Solver& s, bool) { 
		EnumerationConstraint::simplify(s, false);
		simplifyNogoods(s);
		return false; 
	}
	virtual void destroyNogoods(Solver*, bool)= 0;
	virtual void simplifyNogoods(Solver& s)   = 0;
	VarVec* project;
};
/////////////////////////////////////////////////////////////////////////////////////////
// strategy_record
/////////////////////////////////////////////////////////////////////////////////////////
class ModelEnumerator::SolutionQueue : public mt::MultiQueue<SharedLiterals*, void (*)(SharedLiterals*)> {
public:
	typedef SharedLiterals SL;
	typedef mt::MultiQueue<SL*, void (*)(SL*)> base_type;
	SolutionQueue(uint32 m) : base_type(m, releaseLits) {}
	void addSolution(SL* solution, const ThreadId& id) {
		publish(solution, id);
	}
	static void releaseLits(SL* x) { x->release(); }
};

class ModelEnumerator::RecordFinder : public ModelFinder {
public:
	typedef ModelEnumerator::QPtr   QPtr;
	typedef SolutionQueue::ThreadId ThreadId;
	typedef SolutionQueue::SL       SL;
	typedef ClauseCreator::Result   Result;
	RecordFinder(Solver& s, MinimizeConstraint* min, VarVec* project, SolutionQueue* q) : ModelFinder(s, min, project), queue(q) {
		if (q) { id = q->addThread(); }
	}
	Constraint* cloneAttach(Solver& s) { return new RecordFinder(s, cloneMinimizer(s), 0, queue); }
	void doCommitModel(Enumerator& ctx, Solver& s);
	bool doUpdate(Solver& s);
	void simplifyNogoods(Solver& s)       { simplifyDB(s, nogoods, false); }
	void destroyNogoods(Solver* s, bool b){ 
		while (!nogoods.empty()) {
			nogoods.back()->destroy(s, b);
			nogoods.pop_back();
		}
	}
	typedef Solver::ConstraintDB ConstraintDB;
	QPtr         queue;
	ThreadId     id;
	LitVec       solution;
	ConstraintDB nogoods;
};

bool ModelEnumerator::RecordFinder::doUpdate(Solver& s) {
	if (queue) {
		uint32 f = ClauseCreator::clause_no_add | ClauseCreator::clause_no_release | ClauseCreator::clause_explicit;
		for (SL* clause; queue->tryConsume(id, clause); ) {
			ClauseCreator::Result res = ClauseCreator::integrate(s, clause, f);	
			if (res.local) { nogoods.push_back(res.local); }
			if (!res.ok()) { return false; }
		}
	}
	else if (!solution.empty()) {
		ClauseInfo e(Constraint_t::learnt_other);
		ClauseCreator::Result ret = ClauseCreator::create(s, solution, ClauseCreator::clause_no_add, e);
		solution.clear();
		if (ret.local) { nogoods.push_back(ret.local); }
		return ret.ok();
	}
	return true;
}

void ModelEnumerator::RecordFinder::doCommitModel(Enumerator& en, Solver& s) {
	ModelEnumerator& ctx = static_cast<ModelEnumerator&>(en);
	if (ctx.trivial()) { return; }
	assert(solution.empty() && "Update not called!");
	solution.clear();
	if (!ctx.projectionEnabled()) {
		for (uint32 x = s.decisionLevel(); x != 0; --x) {
			Literal d = s.decision(x);
			if      (!s.auxVar(d.var()))  { solution.push_back(~d); }
			else if (d != s.tagLiteral()) {
				// Todo: set of vars could be reduced to those having the aux var in their reason set.
				const LitVec& tr = s.trail();
				const uint32  end= x != s.decisionLevel() ? s.levelStart(x+1) : (uint32)tr.size();
				for (uint32 n = s.levelStart(x)+1; n != end; ++n) {
					if (!s.auxVar(tr[n].var())) { solution.push_back(~tr[n]); }
				}
			}
		}
	}
	else {
		for (uint32 i = 0, end = ctx.numProjectionVars(); i != end; ++i) {
			solution.push_back(~s.trueLit(ctx.projectVar(i)));
		}
	}
	if (queue) {
		assert(!queue->hasItems(id));
		// parallel solving active - share solution nogood with other solvers
		SL* shared = SL::newShareable(solution, Constraint_t::learnt_other);
		queue->addSolution(shared, id);
		solution.clear();
	}
	else if (solution.empty()) { 
		solution.push_back(negLit(0)); 
	}
}
/////////////////////////////////////////////////////////////////////////////////////////
// strategy_backtrack
/////////////////////////////////////////////////////////////////////////////////////////
class ModelEnumerator::BacktrackFinder : public ModelFinder {
public:
	BacktrackFinder(Solver& s, MinimizeConstraint* min, VarVec* project, uint32 projOpts) : ModelFinder(s, min, project), opts(projOpts) {}
	bool hasModel() const { return !solution.empty(); }
	// Base interface
	void simplifyNogoods(Solver& s) {
		for (ConstraintDB::iterator it = nogoods.begin(), end = nogoods.end(); it != end; ++it) {
			if (it->second && it->second->simplify(s, false)) { 
				s.removeWatch(it->first, this);
				it->second->destroy(&s, false);
				it->second = 0;
			}
		}
		while (!nogoods.empty() && nogoods.back().second == 0) { nogoods.pop_back(); }
	}
	void destroyNogoods(Solver* s, bool b) {
		while (!nogoods.empty()) {
			NogoodPair x = nogoods.back();
			if (x.second) {
				if (s) { s->removeWatch(x.first, this); }
				x.second->destroy(s, b);
			}
			nogoods.pop_back();
		}
	}
	// EnumerationConstraint interface
	void doCommitModel(Enumerator& ctx, Solver& s);
	bool doUpdate(Solver& s);
	// Constraint interface
	Constraint* cloneAttach(Solver& s){ return new BacktrackFinder(s, cloneMinimizer(s), 0, opts); }
	PropResult  propagate(Solver&, Literal, uint32&);
	void        reason(Solver& s, Literal p, LitVec& x){
		for (uint32 i = 1, end = s.level(p.var()); i <= end; ++i) { 
			x.push_back(s.decision(i)); 
		}
	}
	typedef std::pair<Literal, Constraint*> NogoodPair;
	typedef PodVector<NogoodPair>::type     ConstraintDB;
	LitVec       solution;
	ConstraintDB nogoods;
	uint32 opts;
};

Constraint::PropResult ModelEnumerator::BacktrackFinder::propagate(Solver& s, Literal, uint32& pos) {
	assert(pos < nogoods.size() && nogoods[pos].second != 0);
	ClauseHead* c = static_cast<ClauseHead*>(nogoods[pos].second);
	if (!c->locked(s)) {
		c->destroy(&s, true);
		nogoods[pos].second = (c = 0);
		while (!nogoods.empty() && !nogoods.back().second) {
			nogoods.pop_back();
		}
	}
	return PropResult(true, c != 0);
}
bool ModelEnumerator::BacktrackFinder::doUpdate(Solver& s) {
	if (hasModel()) {
		bool   ok = true;
		uint32 sp = s.strategy.saveProgress;
		if ((opts & ModelEnumerator::project_save_progress) != 0 ) { s.strategy.saveProgress = 1; }
		s.undoUntil(s.backtrackLevel());
		s.strategy.saveProgress = sp;
		ClauseRep rep = ClauseCreator::prepare(s, solution, 0, Constraint_t::learnt_conflict);
		if (rep.size == 0 || s.isFalse(rep.lits[0])) { // The decision stack is already ordered.
			ok = s.backtrack();
		}
		else if (rep.size == 1 || s.isFalse(rep.lits[1])) { // The projection nogood is unit. Force the single remaining literal from the current DL.
			ok = s.force(rep.lits[0], this);
		}
		else if (!s.isTrue(rep.lits[0])) { // Shorten the projection nogood by assuming one of its literals to false.
			uint32  f = static_cast<uint32>(std::stable_partition(rep.lits+2, rep.lits+rep.size, std::not1(std::bind1st(std::mem_fun(&Solver::isFalse), &s))) - rep.lits);
			Literal x = (opts & ModelEnumerator::project_use_heuristic) != 0 ? s.heuristic()->selectRange(s, rep.lits, rep.lits+f) : rep.lits[0];
			LearntConstraint* c = Clause::newContractedClause(s, rep, f, true);
			CLASP_FAIL_IF(!c, "Invalid constraint!");
			s.assume(~x);
			// Remember that we must backtrack the current decision
			// level in order to guarantee a different projected solution.
			s.setBacktrackLevel(s.decisionLevel());
			// Attach nogood to the current decision literal. 
			// Once we backtrack to x, the then obsolete nogood is destroyed 
			// keeping the number of projection nogoods linear in the number of (projection) atoms.
			s.addWatch(x, this, (uint32)nogoods.size());
			nogoods.push_back(NogoodPair(x, c));
			ok = true;
		}
		solution.clear();
		return ok;
	}
	if (optimize() || s.sharedContext()->concurrency() == 1 || disjointPath()) {
		return true;
	}
	s.setStopConflict();
	return false;
}

void ModelEnumerator::BacktrackFinder::doCommitModel(Enumerator& ctx, Solver& s) {
	ModelEnumerator& en = static_cast<ModelEnumerator&>(ctx);
	uint32           dl = s.decisionLevel();
	solution.assign(1, dl ? ~s.decision(dl) : negLit(0));
	if (en.projectionEnabled()) {
		// Remember the current projected assignment as a nogood.
		solution.clear();
		for (uint32 i = 0, end = en.numProjectionVars(); i != end; ++i) {
			solution.push_back(~s.trueLit(en.projectVar(i)));
		}
		// Remember initial decisions that are projection vars.
		for (dl = s.backtrackLevel(); dl < s.decisionLevel(); ++dl) {
			if (!s.varInfo(s.decision(dl+1).var()).project()) { break; }
		}
	}
	s.setBacktrackLevel(dl);
}
/////////////////////////////////////////////////////////////////////////////////////////
// class ModelEnumerator
/////////////////////////////////////////////////////////////////////////////////////////
ModelEnumerator::ModelEnumerator(Strategy st)
	: Enumerator()
	, queue_(0)
	, project_(0)
	, options_(st) {
}

Enumerator* EnumOptions::createModelEnumerator(const EnumOptions& opts) {
	ModelEnumerator*          e = new ModelEnumerator();
	ModelEnumerator::Strategy s = ModelEnumerator::strategy_auto;
	if (opts.type > (int)ModelEnumerator::strategy_auto && opts.type <= (int)ModelEnumerator::strategy_record) {
		s = static_cast<ModelEnumerator::Strategy>(opts.type);
	}
	e->setStrategy(s, opts.project);
	return e;
}

ModelEnumerator::~ModelEnumerator() {
	delete queue_;
}

void ModelEnumerator::setStrategy(Strategy st, uint32 projection) {
	options_ = st;
	project_ = 0;
	if (projection) { 
		options_ |= (((projection|1u) & 7u) << 4u);
		project_  = new VarVec();
	}
	if (st == strategy_auto) {
		options_ |= detect_strategy_flag;
	}
}

EnumerationConstraint* ModelEnumerator::doInit(SharedContext& ctx, MinimizeConstraint* min, int numModels) {
	delete queue_;
	queue_ = 0;
	initProjection(ctx); 
	uint32 st = strategy();
	if (detectStrategy() || (ctx.concurrency() > 1 && !ModelEnumerator::supportsParallel())) {
		st = 0;
	}
	bool optOne  = minimizer() && minimizer()->mode() == MinimizeMode_t::optimize;
	bool trivial = optOne || std::abs(numModels) == 1;
	if (optOne && project_.get()) {
		for (const WeightLiteral* it =  minimizer()->lits; !isSentinel(it->first) && trivial; ++it) {
			trivial = ctx.varInfo(it->first.var()).project();
		}
		if (!trivial) { ctx.report(warning(Event::subsystem_prepare, "Projection: Optimization may depend on enumeration order.")); }
	}
	if (st == strategy_auto) { st  = trivial || (project_.get() && ctx.concurrency() > 1) ? strategy_record : strategy_backtrack; }
	if (trivial)             { st |= trivial_flag; }
	if (ctx.concurrency() > 1 && !trivial && st != strategy_backtrack) {
		queue_ = new SolutionQueue(ctx.concurrency()); 
		queue_->reserve(ctx.concurrency() + 1);
	}
	options_ &= ~uint32(strategy_opts_mask);
	options_ |= st;
	Solver& s = *ctx.master();
	EnumerationConstraint* c = st == strategy_backtrack 
	  ? static_cast<ConPtr>(new BacktrackFinder(s, min, project_.release(), projectOpts()))
	  : static_cast<ConPtr>(new RecordFinder(s, min, project_.release(), queue_));
	if (projectionEnabled()) { setIgnoreSymmetric(true); }
	return c;
}

void ModelEnumerator::initProjection(SharedContext& ctx) {
	if (!projectionEnabled()) { return; }
	if (!project_.is_owner()) { project_ = new VarVec(); }
	project_->clear();
	const SymbolTable& index = ctx.symbolTable();
	if (index.type() == SymbolTable::map_indirect) {
		for (SymbolTable::const_iterator it = index.begin(); it != index.end(); ++it) {
			if (!it->second.name.empty() && it->second.name[0] != '_') {
				addProjectVar(ctx, it->second.lit.var(), true);
			}
		}
		for (VarVec::const_iterator it = project_->begin(), end = project_->end(); it != end; ++it) {
			ctx.unmark(*it);
		}
	}
	else {
		for (Var v = 1; v < index.size(); ++v) { addProjectVar(ctx, v, false); }
	}
	// tag projection nogoods with step literal (if any).
	addProjectVar(ctx, ctx.stepLiteral().var(), false);
	if (project_->empty()) { 
		// We project to the empty set. Add true-var so that 
		// we can distinguish this case from unprojected search
		project_->push_back(0);
	}
}

void ModelEnumerator::addProjectVar(SharedContext& ctx, Var v, bool tag) {
	if (ctx.master()->value(v) == value_free && (!tag || !ctx.marked(posLit(v)))) {
		project_->push_back(v);
		ctx.setFrozen(v, true);
		ctx.setProject(v, true);
		if (tag) { ctx.mark(posLit(v)); ctx.mark(negLit(v)); }
	}
}

}
