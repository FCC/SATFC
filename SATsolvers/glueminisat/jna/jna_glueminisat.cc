/**
@author gsauln, narnosti
**/
#include "core/Solver.h"

using namespace Minisat;

// Added functionality
class JNASolver : public Solver
{
public:
	bool getInterruptState();
};

inline bool JNASolver::getInterruptState() { return asynch_interrupt; }

bool BOOL(int x) { if (x) return 1; return 0; }

// functions accessible from the Java Library
extern "C"
{

	// functions used to operate the solver with jna

	void* createSolver()
	{
		JNASolver* s = new JNASolver();
		return s;
	}

	void destroySolver(void* _solver)
	{
		delete reinterpret_cast<JNASolver*>(_solver);
	}

	void* createVecLit()
	{
		return new vec<Lit>();
	}
	
	void destroyVecLit(void* _vec)
	{
		delete reinterpret_cast<vec<Lit>*>(_vec);
	}


	void addLitToVec(void* _vec, int variable, int state)
	{
		reinterpret_cast<vec<Lit>*>(_vec)->push(mkLit(variable, BOOL(state)));
	}

	int addClause(void* _solver, void* _vec)
	{
		vec<Lit>* ps = reinterpret_cast<vec<Lit>*>(_vec);
		return reinterpret_cast<JNASolver*>(_solver)->addClause(*ps);
	}

	int solve(void* _solver)
	{
		return reinterpret_cast<JNASolver*>(_solver)->solve();
	}
	
	int solveWithAssumptions(void* _solver, void* _vec)
	{
		return reinterpret_cast<JNASolver*>(_solver)->solve(*reinterpret_cast<vec<Lit>*>(_vec));
	}
	
	int solveWithOneAssumption(void* _solver,int variable, int state)
	{
		return reinterpret_cast<JNASolver*>(_solver)->solve(mkLit(variable, BOOL(state)));
	}	

	int newVar(void* _solver)
	{
		return reinterpret_cast<JNASolver*>(_solver)->newVar();
	}

	int nVars(void* _solver)
	{
		return reinterpret_cast<JNASolver*>(_solver)->nVars();
	}

	int okay(void* _solver)
	{
		return reinterpret_cast<JNASolver*>(_solver)->okay();
	}

	int value(void* _solver, int var)
	{
		JNASolver* s = reinterpret_cast<JNASolver*>(_solver);
		return (s->modelValue(var)!=l_True);
	}

	void interrupt(void* _solver)
	{
		reinterpret_cast<JNASolver*>(_solver)->interrupt();
	}

	void clearInterrupt(void* _solver){
		reinterpret_cast<JNASolver*>(_solver)->clearInterrupt();
	}

	int getInterruptState(void* _solver)
	{
		return reinterpret_cast<JNASolver*>(_solver)->getInterruptState();
	}

	// Parameters

}


