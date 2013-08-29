// Guillaume Saulnier-Comte
#ifndef JNA_INCCLASP_H
#define JNA_INCCLASP_H

#include "jna_clasp.h"

using namespace Clasp;
namespace JNA
{

class JNAIncProblem : public Clasp::Input {
public:
        JNAIncProblem(const char* (*readCallback)());// readCallback must be a java function
        Format format() const { return DIMACS; }
        bool    read(ApiPtr api, uint32 properties);
        void    addMinimize(Clasp::MinimizeBuilder&, ApiPtr) {}
        void    getAssumptions(Clasp::LitVec& vec);

	bool    getStatus() { return status_; } // returns the output of the read function after solve has been called on the problem
private:
	const char* (*readCallback_)();
        bool status_;
	LitVec assumptions_;
};

class JNAIncControl : public Clasp::IncrementalControl
{
public:
	JNAIncControl(int (*nextCallback)(), JNAResult* result);
	void initStep(ClaspFacade& f) {}
        bool nextStep(ClaspFacade& f);
private:
	int (*nextCallback_)();
	JNAResult* result_;
};

bool parseLitVec(StreamSource& in, LitVec& vec);

} //end JNA namespace

// C function definitions
extern "C"
{
void* createIncProblem(const char* (*readCallback)());
void destroyIncProblem(void* _problem);
	
void* createIncControl(int (*_nextCallback)(), void* _result);
void destroyIncControl(void* _control);
	
void jnasolveIncremental(void* _facade, void* _problem, void* _config, void* _control, void* _result);

} // end Extern "C"


#endif
