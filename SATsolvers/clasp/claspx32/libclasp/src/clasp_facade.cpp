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
#include <clasp/clasp_facade.h>
#include <clasp/model_enumerators.h>
#include <clasp/cb_enumerator.h>
#include <clasp/weight_constraint.h>
#include <clasp/minimize_constraint.h>
#include <clasp/parallel_solve.h>
#include <stdio.h>
namespace Clasp {
/////////////////////////////////////////////////////////////////////////////////////////
// GlobalOptions
/////////////////////////////////////////////////////////////////////////////////////////
GlobalOptions::GlobalOptions() { }

Enumerator* GlobalOptions::createEnumerator(Enumerator::Report* r) {
	ModelEnumerator* e = 0;
	Enumerator* ret    = 0;
	if (consequences()) {
		ret = new CBConsequences(enumerate.mode == enum_brave ? CBConsequences::brave_consequences : CBConsequences::cautious_consequences);
	}
	else if (enumerate.mode == enum_record) {
		ret = (e = new RecordEnumerator());
	}
	else {
		ret = (e = new BacktrackEnumerator(enumerate.projectOpts));
	}
	if (e) { e->setEnableProjection(enumerate.project); }
	ret->setRestartOnModel(enumerate.restartOnModel);
	ret->setReport(r);
	return ret;
}
/////////////////////////////////////////////////////////////////////////////////////////
// ClaspConfig
/////////////////////////////////////////////////////////////////////////////////////////
ClaspConfig::ClaspConfig()  { addSolver(ctx.master()); }
ClaspConfig::~ClaspConfig() {
	setMaxSolvers(1);
	delete solvers_.back();
	solvers_.clear();
}
void ClaspConfig::reserveSolvers(uint32 num) {
	ctx.setSolvers(num);
	solvers_.reserve(num);
}
void ClaspConfig::addSolver(Solver* s) {
	std::auto_ptr<Solver> x(s);
	s->setId(solvers_.size());
	s->strategies().heuId   = heu_berkmin;
	s->strategies().heuOther= 3;
	s->strategies().heuMoms = 1;
	solvers_.push_back(new SolverConfig(*x.get()));
	x.release();
	if (solvers_.size() > ctx.numSolvers()) { ctx.setSolvers(solvers_.size()); }
}
uint32 ClaspConfig::removeSolvers(uint32 id) {
	uint32 j = 1;
	for (uint32 i = 1; i != numSolvers(); ++i) {
		Solver* s = solvers_[i]->solver;
		if (s->id() >= id) { delete s; delete solvers_[i]; }
		else               { s->setId(j); solvers_[j++] = solvers_[i]; }
	}
	solvers_.resize(j);
	ctx.setSolvers(j);
	return j;
}

bool ClaspConfig::validate(SolverConfig& sc, std::string& err) {
	if (sc.solver->strategies().search == SolverStrategies::no_learning) {
		if (sc.solver->strategies().heuId != heu_unit && sc.solver->strategies().heuId != heu_none) {
			err  = "Selected heuristic requires lookback strategy!";
			return false;
		}
		SolverStrategies* s = &sc.solver->strategies();
		s->ccMinAntes  = SolverStrategies::no_antes;
		s->strRecursive= 0;
		s->compress    = UINT32_MAX;
		s->saveProgress= 0;
		sc.params.restart.disable();
		sc.params.reduce.disable();
	}
	if (sc.params.restart.sched.disabled()) { sc.params.restart.disable(); }
	if (sc.params.reduce.fReduce() == 0.0f) { sc.params.reduce.disable();  }
	if (sc.params.reduce.fMax != 0.0f)      { sc.params.reduce.fMax = std::max(sc.params.reduce.fMax, sc.params.reduce.fInit); }
	return true;
}

bool ClaspConfig::validate(std::string& err) {
	if (enumerate.mode == enum_bt && enumerate.restartOnModel) { 
		err = "Options 'restart-on-model' and 'enum-mode=bt' are mutually exclusive!";
		return false;
	}
	for (uint32 i = 0; i != numSolvers(); ++i) {
		if (!validate(*getSolver(i), err)) { return false; }
	}
	return true;
}

DecisionHeuristic* ClaspConfig::createHeuristic(const SolverStrategies& str) {
	uint32 heuParam = str.heuParam;
	uint32 id       = str.heuId;
	if      (id == heu_berkmin) { return new ClaspBerkmin(heuParam); }
	else if (id == heu_vmtf)    { return new ClaspVmtf(heuParam == 0 ? 8 : heuParam); }
	else if (id == heu_none)    { return new SelectFirst(); }
	else if (id == heu_unit)    { 
		if (heuParam == 0 || heuParam > Lookahead::hybrid_lookahead) { heuParam = Lookahead::atom_lookahead; }
		return new UnitHeuristic((LookaheadType)heuParam); 
	}
	else if (id == heu_vsids)   {
		double m = heuParam == 0 ? 0.95 : heuParam;
		while (m > 1.0) { m /= 10; }
		return new ClaspVsids(m);
	}
	throw std::runtime_error("Unknown heuristic id!");
}

void ClaspConfig::applyHeuristic(SolverConfig& sc) {
	Lookahead::Type lookT = (Lookahead::Type)sc.params.init.lookType;
	HeuristicType heuType = (HeuristicType)sc.solver->strategies().heuId;
	bool          lookHeu = heuType == heu_unit;
	// check for unrestricted lookahead
	if (lookHeu || (lookT != Lookahead::no_lookahead && sc.params.init.lookOps == 0)) {
		sc.params.init.lookType = Lookahead::no_lookahead;
		sc.params.init.lookOps  = 0;
		if (!lookHeu) { sc.solver->addPost(new Lookahead(lookT)); }
		else          { sc.solver->strategies().heuParam = lookT != Lookahead::no_lookahead ? lookT : Lookahead::atom_lookahead; }
	}
	sc.solver->setHeuristic(heuType, createHeuristic(sc.solver->strategies()));
}

void ClaspConfig::applyHeuristic() {
	if (SolverStrategies::heuFactory_s == 0) {
		SolverStrategies::heuFactory_s = &ClaspConfig::createHeuristic;
	}
	for (uint32 i = 0; i != numSolvers(); ++i) {
		applyHeuristic(*getSolver(i));
	}
}

void ClaspConfig::reset() {
	ctx.reset();
	solve    = SolveOptions();
	eq       = EqOptions();
	opt      = Optimize();
	enumerate= EnumOptions();
	master()->solver = ctx.master();
	for (uint32 i = 1; i < numSolvers(); ++i) {
		getSolver(i)->solver->reset();
	}
}

IncrementalControl::IncrementalControl()  {}
IncrementalControl::~IncrementalControl() {}
/////////////////////////////////////////////////////////////////////////////////////////
// ClaspFacade
/////////////////////////////////////////////////////////////////////////////////////////
ClaspFacade::ClaspFacade() 
	: config_(0)
	, inc_(0)
	, cb_(0)
	, input_(0)
	, ctrl_(0)
	, graph_(0)
	, enum_(0)
	, api_(0)
	, result_(result_unknown)
	, state_(num_states)
	, step_(0)
	, more_(true) {
}

void ClaspFacade::init(Input& problem, ClaspConfig& config, IncrementalControl* inc, Callback* c) {
	config_ = &config;
	inc_    = inc;
	cb_     = c;
	input_  = &problem;
	ctrl_   = 0;
	graph_  = 0;
	enum_   = config.enumerate.mode != GlobalOptions::enum_auto ? config.createEnumerator(this) : 0;
	api_    = 0;
	result_ = result_unknown;
	state_  = num_states;
	step_   = 0;
	more_   = true;
	validateWeak();
	config.applyHeuristic();
}

void ClaspFacade::validateWeak(ClaspConfig& cfg) {
	if (cfg.numSolvers() > cfg.solve.supportedSolvers()) {
		warning("Too many solvers.");
		cfg.setMaxSolvers(cfg.solve.supportedSolvers());
	}
	bool warnUnit = true;
	bool warnInit = true;
	for (uint32 i = 0; i != cfg.numSolvers(); ++i) {
		if (cfg.getSolver(i)->solver->strategies().heuId == ClaspConfig::heu_unit) {
			InitParams& p = cfg.getSolver(i)->params.init;
			if (p.lookType == Lookahead::no_lookahead) {
				if (warnUnit) {
					warning("Heuristic 'Unit' implies lookahead. Using atom.");
					warnUnit = false;
				}
				p.lookType = Lookahead::atom_lookahead;
			}
			else if (p.lookOps != 0) {
				if (warnInit) {
					warning("Heuristic 'Unit' implies unrestricted lookahead.");
					warnInit = false;
				}
				p.lookOps = 0;
			}
		}
	}
}

void ClaspFacade::validateWeak() {
	if (inc_) {
		if (config_->enumerate.onlyPre) {
			warning("'--pre' is ignored in incremental setting."); 
			config_->enumerate.onlyPre = false;
		}
	}
	if (config_->eq.noSCC && config_->eq.iters != 0) {
		warning("Selected reasoning mode implies '--eq=0'.");
		config_->eq.noEq();
	}
	if (config_->numSolvers() > 1 && enum_.get() && !enum_->supportsParallel()) {
		warning("Selected reasoning mode implies #Threads=1.");
		config_->setMaxSolvers(1);
	}
	if (config_->numSolvers() > config_->solve.recommendedSolvers()) {
		char buf[128];
		sprintf(buf, "Oversubscription: #Threads=%u exceeds logical CPUs (%u).", config_->numSolvers(), config_->solve.recommendedSolvers());
		warning(buf);
		warning("Oversubscription leads to excessive context switching.");
	}
	if (config_->opt.all || config_->opt.no) {
		config_->opt.hierarch = 0;
	}
	if (config_->enumerate.numModels == -1 && config_->consequences()) {
		config_->enumerate.numModels = 0;
	}
	validateWeak(*config_);
}

// Solving...
void ClaspFacade::solve(Input& problem, ClaspConfig& config, IncrementalControl* inc, Callback* c) {
	init(problem, config, inc, c);
	const bool onlyPre = config.enumerate.onlyPre;
	AutoState outer(this, state_start);
	LitVec assume;
	do {
		if (inc) { inc->initStep(*this); }
		result_   = result_unknown;
		more_     = true;
		if (config.ctx.master()->hasConflict() || !read() || !preprocess()) {
			result_ = result_unsat;
			more_   = false;
			reportSolution(*config.ctx.enumerator(), true);
			break;
		}
		else if (!onlyPre) {
			assume.clear();
			problem.getAssumptions(assume);
			more_    = solve(assume);
			if (result_ == result_unknown && !more_) {
				// initial assumptions are unsat
				result_ = result_unsat;
			}
		}
	} while (inc && inc->nextStep(*this) && ++step_);
}

// Creates a ProgramBuilder-object if necessary and reads
// the input by calling input_->read().
// Returns false, if the problem is trivially UNSAT.
bool ClaspFacade::read() {
	AutoState state(this, state_read);
	Input::ApiPtr ptr(&config_->ctx);
	if (input_->format() == Input::SMODELS) {
		if (step_ == 0) {
			api_   = new ProgramBuilder();
			api_->startProgram(config_->ctx, config_->eq);
		}
		if (inc_) {
			api_->updateProgram();
		}
		ptr.api= api_.get();
	}
	if (config_->opt.hierarch > 0 && !config_->opt.no) {
		config_->ctx.requestTagLiteral();
	}
	uint32 properties = !config_->enumerate.maxSat || input_->format() != Input::DIMACS ? 0 : Input::AS_MAX_SAT;
	if (config_->enumerate.numModels != 1) { 
		properties |= config_->enumerate.numModels == -1 ? Input::PRESERVE_MODELS_ON_MIN : Input::PRESERVE_MODELS;
	}
	return input_->read(ptr, properties);
}

// Prepare the solving state:
//  - if necessary, transforms the input to nogoods by calling ProgramBuilder::endProgram()
//  - fires event_p_prepared after input was transformed to nogoods
//  - adds any minimize statements to the solver and initializes the enumerator
//  - calls Solver::endInit().
// Returns false, if the problem is trivially UNSAT.
bool ClaspFacade::preprocess() {
	AutoState state(this, state_preprocess);
	SharedContext& ctx = config_->ctx;
	SharedMinimizeData* m = 0;
	Input::ApiPtr ptr(&ctx);
	if (api_.get()) {
		if (!api_->endProgram()) {
			fireEvent(*ctx.master(), event_p_prepared);
			return false;
		}
		setGraph();
		ptr.api = api_.get();
	}
	if (!config_->opt.no && step_ == 0) {
		MinimizeBuilder builder;
		input_->addMinimize(builder, ptr);
		if (builder.hasRules()) {
			if (!config_->opt.vals.empty()) {
				const SumVec& opt = config_->opt.vals;
				for (uint32 i = 0; i != opt.size(); ++i) {
					builder.setOptimum(i, opt[i]);
				}
			}
			m = builder.build(ctx, config_->ctx.tagLiteral());
			if (!m) { return false; }
		}
		if (!builder.hasRules() || (builder.numRules() == 1 && config_->opt.hierarch < 2)) {
			config_->ctx.removeTagLiteral();
		}
	}
	fireEvent(*ctx.master(), event_p_prepared);
	if (!inc_ && api_.is_owner()) {
		api_ = 0;
	}
	return config_->enumerate.onlyPre || (initEnumeration(m) && initContextObject(ctx));
}

// Finalizes initialization of model enumeration.
// Configures and adds an eventual minimize constraint,
// sts the number of models to compute and adds warnings
// if this number conflicts with the preferred number of the enumerator.
bool ClaspFacade::initEnumeration(SharedMinimizeData* min)  {
	GlobalOptions::EnumOptions& opts = config_->enumerate;
	MinimizeMode minMode             = !min || config_->opt.all ? MinimizeMode_t::enumerate : MinimizeMode_t::optimize;
	if (step_ == 0) {
		GlobalOptions::EnumMode autoMode = GlobalOptions::enum_bt;
		if (opts.restartOnModel) {
			autoMode = GlobalOptions::enum_record;
		}
		if (minMode == MinimizeMode_t::optimize && !opts.project) {
			autoMode = GlobalOptions::enum_record;
		}
		if (opts.project && config_->numSolvers() > 1) {
			autoMode = GlobalOptions::enum_record;
		}
		uint32 autoModels = !min && !config_->consequences();
		if (autoModels == 0 && opts.numModels > 0) {
			if (config_->consequences())            { warning("'--number' not 0: last model may not cover consequences.");   }
			if (minMode == MinimizeMode_t::optimize){ warning("'--number' not 0: optimality of last model not guaranteed."); }
		}
		if (opts.numModels == -1)      { opts.numModels = autoModels; }	
		if (config_->consequences() && minMode == MinimizeMode_t::optimize) {
			warning("Optimization: Consequences may depend on enumeration order.");
		}
		if (opts.project && minMode == MinimizeMode_t::optimize) {
			for (const WeightLiteral* it = min->lits; !isSentinel(it->first); ++it) {
				if ( !config_->ctx.project(it->first.var()) ) {
					warning("Projection: Optimization values may depend on enumeration order.");
					break;
				}
			}
		}
		if (config_->enumerate.mode == GlobalOptions::enum_auto) {
			config_->enumerate.mode = autoMode;
			enum_ = config_->createEnumerator(this);
		}
	}
	if (min) {
		min->setMode(minMode, config_->opt.hierarch);
		enum_->setMinimize(min);
	}
	enum_->enumerate(opts.numModels);
	config_->ctx.addEnumerator(enum_.release());
	return true;
}

// Finalizes initialization of SharedContext and
// computes a value that represents the problem size.
// The value is then used by the reduce-heuristic
// to determine the initial learnt db size.
bool ClaspFacade::initContextObject(SharedContext& ctx) const {
	if (!ctx.endInit()) { return false; }
	uint32 estimate = 0;
	uint32 size     = 0;
	for (uint32 i = 0; i != config_->numSolvers(); ++i) {
		SolverConfig* x = config_->getSolver(i);
		if (x->params.reduce.estimate() && estimate == 0) {
			estimate = ctx.problemComplexity();
			break;
		}
	}
	size = ctx.numConstraints();
	if (input_->format() != Input::DIMACS) {
		double r = ctx.numVars() / std::max(1.0, double(ctx.numConstraints()));
		if (r < 0.1 || r > 10.0) {
			size = std::max(ctx.numVars(), ctx.numConstraints());
		}
		else {
			size = std::min(ctx.numVars(), ctx.numConstraints());
		}
	}
	ctx.setProblemSize(size, estimate);
	return true;
}

void ClaspFacade::setGraph() {
	if (!graph_.get() && api_->dependencyGraph() && api_->dependencyGraph()->nodes() > 0) {
		graph_ = api_->dependencyGraph(true);
		for (uint32 i = 0; i != config_->numSolvers(); ++i) {
			DefaultUnfoundedCheck* ufs = new DefaultUnfoundedCheck();
			ufs->attachTo(*config_->getSolver(i)->solver, graph_.get());
		}
	}
}

bool ClaspFacade::solve(const LitVec& assume) {
	struct OnExit : AutoState { 
		OnExit(ClaspFacade* f) : AutoState(f, state_solve) {}
		~OnExit() { SolveAlgorithm* x = self_->ctrl_; self_->ctrl_ = 0; delete x; }
	};
	OnExit resetCtrl(this);
	config_->solve.createSolveObject(ctrl_, config_->ctx, config_->solvers());
	bool more = ctrl_->solve(config_->ctx, config_->master()->params, assume);
	if (ctrl_->hasErrors()) {
		for (uint32 i = 0; i != config_->numSolvers(); ++i) {
			if (config_->getSolver(i)->solver->id() == Solver::invalidId) {
				char buf[128];
				sprintf(buf, "Thread %u failed and was removed.", i);
				warning(buf);
			}
		}
		config_->removeSolvers(Solver::invalidId);
	}
	return more;
}
}
