// Guillaume Saulnier-Comte
#include <iostream>
#include <sstream>
#include <climits>

#include "jna_incclasp.h"
#include <clasp/clause.h>

namespace JNA
{

bool parseLitVec(StreamSource& in, LitVec& vec)
{
	Literal rLit;
	int lit = -1;
	while (in.parseInt(lit))
	{
		if (lit == 0)
		{
			return true;
		}
		else
		{
			rLit = lit >= 0 ? posLit(lit) : negLit(-lit);
			vec.push_back(rLit);
		}
	}
	return false;
}

JNAIncProblem::JNAIncProblem(const char* (*readCallback)()) : readCallback_(readCallback) {}

bool JNAIncProblem::read(ApiPtr api, uint32 properties)
{
	const char* problem = readCallback_();

	// initialize variables
	std::istringstream istr(problem);
	StreamSource in(istr);

	SharedContext& ctx = *(api.ctx);
	ClauseCreator cc(ctx.master());

	int state = 1; // 1, we need to read the p line; 2, we need to read the a line; 3 we need to read the new clauses, 0 stop and parsing completed successfully, -1..-n stop and an error occured

	int numClauses = 0;
	int clauseCount = 0;
	LitVec clause;

	// start parsing the message and update the problem
	in.skipWhite();
	if (match(in, "exit", false))
	{
		return false;
	}
	// parse the problem
	while (state > 0)
	{
		in.skipWhite();
		if (*in == 'c') { skipLine(in); continue; } // skip comments (should not have any but...)
		if (state == 1) // read the p line
		{
			if (!match(in, "p ", false)) { state = -1; continue;}
			if (!match(in, "pcnf ", false)) { state = -2; continue;}
			int totalVars = 0;
			// check the total number of vars and increase the count in the ctx
			if (!in.parseInt(totalVars, 1, INT_MAX)) { state = -3; continue;}
			while (ctx.numVars() < totalVars)
			{
				ctx.addVar(Var_t::atom_var);
			}
			ctx.symTab().startInit();
                        ctx.symTab().endInit(SymbolTable::map_direct, totalVars+1);

			// set the number of clauses to read.
			if (!in.parseInt(numClauses, 0, INT_MAX)) { state = -4; continue;}
			state = 2;
		}
		else if (state == 2) // read the a line
		{
			assumptions_.clear();
			if (!match(in, "a ", false)) { state = -5; continue;}
			if (!parseLitVec(in, assumptions_)) { state = -6; continue;}
			state = 3;
			ctx.startAddConstraints();
		}
		else if (state == 3) // read the new clause
		{
			if (clauseCount == numClauses) { state = 0; continue;}
			clause.clear();
			if (!parseLitVec(in, clause)) { state = -7; continue;}

			// add clause to context
			cc.start();
			for (LitVec::iterator it = clause.begin(); it != clause.end(); ++it) 
			{
				cc.add(*it);
                        } 
                        if (!cc.end()) { state = -8; continue;}

			clauseCount++;
		}
	}

	if (state == 0) return true;
	return false;
}

void JNAIncProblem::getAssumptions(Clasp::LitVec& vec) 
{
	vec = assumptions_;
}

JNAIncControl::JNAIncControl(int (*nextCallback)(), JNAResult* result) : nextCallback_(nextCallback), result_(result) {}

bool JNAIncControl::nextStep(ClaspFacade& f)
{
	if (result_->getState() != JNAResult::r_SAT)
	{
		result_->setState(JNAResult::r_UNSAT);
	}
	return nextCallback_();
}

void solveIncremental(JNAFacade &facade, JNAIncProblem& problem, JNAConfig& config, JNAIncControl& inc, JNAResult &result)
{
        facade.solveIncremental(problem, *(config.getConfig()), inc, &result);
}

} //end JNA namespace

void* createIncProblem(const char* (*_readCallback)())
{
	JNA::JNAIncProblem* problem = new JNA::JNAIncProblem(_readCallback);
	return problem;
}

void destroyIncProblem(void* _problem)
{
	delete reinterpret_cast<JNA::JNAIncProblem*>(_problem);
}

void* createIncControl(int (*_nextCallback)(), void* _result)
{
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	JNA::JNAIncControl* control = new JNA::JNAIncControl(_nextCallback, result);
	return control;
}

void destroyIncControl(void* _control)
{
	delete reinterpret_cast<JNA::JNAIncControl*>(_control);
}



void jnasolveIncremental(void* _facade, void* _problem, void* _config, void* _control, void* _result)
{
        JNA::JNAFacade* facade = reinterpret_cast<JNA::JNAFacade*>(_facade);
        JNA::JNAIncProblem* problem = reinterpret_cast<JNA::JNAIncProblem*>(_problem);
        JNA::JNAConfig* config = reinterpret_cast<JNA::JNAConfig*>(_config);
        JNA::JNAIncControl* control = reinterpret_cast<JNA::JNAIncControl*>(_control);
        JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
        JNA::solveIncremental(*facade, *problem, *config, *control, *result);
}

