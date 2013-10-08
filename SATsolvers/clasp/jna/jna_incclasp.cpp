// Guillaume Saulnier-Comte
#include <iostream>
#include <sstream>
#include <climits>

#include "jna_incclasp.h"
#include <clasp/clause.h>

using namespace std;

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

JNAIncProblem::JNAIncProblem(void* (*readCallback)()) : readCallback_(readCallback) {}

/**
* Format
* [0]: size of array
* [1]: total number of variables
* [2]: number of new clauses
* [3]: number of control literals of clauses that need to be solved // MUST BE INCREASING
* [4..(4+[2]-1)]: new control variables (1 per new clause)
* [(4+[2])..(4+[2]+[3]-1)]: control literals that are true // MUST BE SORTED
* [(4+[2]+[3])..end]: new clauses separated by 0s.
* 
* or
* 
* [0]: -1 = terminate the solver.
*/
bool JNAIncProblem::read(ApiPtr api, uint32 properties)
{

	void* _problem = readCallback_();
        int* problem = reinterpret_cast<int*>(_problem);

	int aSize = problem[0];
	// initialize variables

	SharedContext& ctx = *(api.ctx);
	ClauseCreator cc(ctx.master());

	if (aSize == -1) // exit command
	{
		return false;
	}	

	// increase the number of vars needed
	while (ctx.numVars() < problem[1])
	{
		ctx.addVar(Var_t::atom_var);
	}
	ctx.symTab().startInit();
	ctx.symTab().endInit(SymbolTable::map_direct, problem[1]+1);

	// set the index
	int index = 4;

	// add the new control variables
	for (int i = 0; i < problem[2]; i++)
	{
		int lit = problem[index+i];
		if (lit < 0) lit *= -1;
		allControls_.push_back(lit);
	}
	index += problem[2];

	// set the true control literals i.e. assumptions
	assumptions_.clear();
	assumptions_.reserve(allControls_.size());
	Literal rLit;
	int j = 0;
	for (int i = 0; i < allControls_.size(); i++)
	{
		int cControl = allControls_.at(i);
		if (j < problem[3] && -problem[index + j] == cControl)
		{
			int lit = problem[index + j];	
			rLit = negLit(-lit);
			assumptions_.push_back(rLit);
			j++;
		}
		else
		{
			rLit = posLit(cControl);
			assumptions_.push_back(rLit);
		}
	}
	index += problem[3];

	// add new clauses
	LitVec clause;
	ctx.startAddConstraints();
	for (int clauseCount = 0; clauseCount < problem[2]; clauseCount++)
	{
		// create the clause
		clause.clear();
		while (problem[index] != 0)
		{
			int lit = problem[index];
			rLit = lit >= 0 ? posLit(lit) : negLit(-lit);
			clause.push_back(rLit);
			index++;
		}
		// add the clause
		cc.start();
                for (LitVec::iterator it = clause.begin(); it != clause.end(); ++it) 
                {
			cc.add(*it);
                } 
                cc.end();
		index++;
	}
	return true;
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

void* createIncProblem(void* (*_readCallback)())
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

