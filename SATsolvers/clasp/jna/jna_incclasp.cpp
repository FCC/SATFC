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

void JNAIncProblem::getAssumptions(Clasp::LitVec& vec) {}

} //end JNA namespace

void test(const char* (*fn)())
{
	JNA::JNAIncProblem problem(fn);
	Clasp::SharedContext* ctx = 0;
	Clasp::Input::ApiPtr ptr(ctx);
	problem.read(ptr,0);
}
