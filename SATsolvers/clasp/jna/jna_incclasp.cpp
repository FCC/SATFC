// Guillaume Saulnier-Comte
#include <iostream>

#include "jna_incclasp.h"

namespace JNA
{

JNAIncProblem::JNAIncProblem(const char* (*readCallback)()) : readCallback_(readCallback) {}

bool JNAIncProblem::read(ApiPtr api, uint32 properties)
{
	std::cout << readCallback_() << std::endl;
	return true;
}

void JNAIncProblem::getAssumptions(Clasp::LitVec& vec) 
{
	vec = assumptions_;
}

JNAIncControl::JNAIncControl(int (*nextCallback)()) : nextCallback_(nextCallback) {}

void solveIncremental(JNAFacade &facade, JNAIncProblem& problem, JNAConfig& config, JNAIncControl& inc, JNAResult &result)
{
        facade.solveIncremental(problem, *(config.getConfig()), inc, &result);
        if (result.getState() != JNAResult::r_SAT)
        {
                result.setState(JNAResult::r_UNSAT);
        }
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

void* createIncControl(int (*_nextCallback)())
{
	JNA::JNAIncControl* control = new JNA::JNAIncControl(_nextCallback);
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


void test(const char* (*fn)(), int (*fn1)())
{
	JNA::JNAIncProblem problem(fn);
	Clasp::SharedContext* ctx = 0;
	Clasp::Input::ApiPtr ptr(ctx);
	problem.read(ptr,0);
	std::cout << fn1() << std::endl;
}
