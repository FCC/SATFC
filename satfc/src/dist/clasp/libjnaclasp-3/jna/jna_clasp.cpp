// Guillaume Saulnier-Comte
// Neil Newman
#include <iostream>
#include <sstream>

#include "jna_clasp.h"
#include <clasp/solver.h>
#include <clasp/enumerator.h>
#include <signal.h>

namespace JNA {

	JNAProblem::JNAProblem() {
		resultState_ = r_UNKNOWN;
		configState_ = c_UNCONFIGURED;
		assignment_ = NULL;
		facade_ = NULL;
		config_ = NULL;
		asyncResult_ = NULL;
	}

	JNAProblem::~JNAProblem() {
		delete[] assignment_;
		assignment_ = NULL;
		delete facade_;
		facade_ = NULL;
		delete config_;
		config_ = NULL;
	}

	void JNAProblem::setConfig(Clasp::Cli::ClaspCliConfig* config) {
		config_ = config;
	}

	void JNAProblem::setAsyncResult(Clasp::ClaspFacade::AsyncResult* asyncResult) {
		asyncResult_ = asyncResult;
	}

	void JNAProblem::setResultState(Result_State resultState) {
		resultState_ = resultState;
	}

	void JNAProblem::setConfigState(Config_State configState) {
		configState_ = configState;
	}

	void JNAProblem::setFacade(Clasp::ClaspFacade* facade) {
		facade_ = facade;
	}

	void JNAProblem::setConfigErrorMessage(std::string message) {
		configErrorMessage_ = message;
	}

	Result_State JNAProblem::getResultState() {
		return resultState_;
	}

	Config_State JNAProblem::getConfigState() {
		return configState_;
	}

	Clasp::ClaspFacade::AsyncResult* JNAProblem::getAsyncResult() {
		return asyncResult_;
	}

	int* JNAProblem::getAssignment() {
		return assignment_;
	}

	Clasp::ClaspFacade* JNAProblem::getFacade() {
		return facade_;
	}

	Clasp::Cli::ClaspCliConfig* JNAProblem::getConfig() {
		return config_;
	}

	std::string JNAProblem::getConfigErrorMessage() {
		return configErrorMessage_;
	}

	bool JNAProblem::interrupt() {
		return facade_->terminate(SIGINT); 
	}

	bool JNAProblem::onModel(const Clasp::Solver& s, const Clasp::Model& m) {
		const Clasp::SymbolTable& index = s.symbolTable();
		delete[] assignment_;
		assignment_ = new int[index.size()];
		assignment_[0] = index.size();
		int i = 1;
		for (Var v = 1; v < index.size(); ++v)
		{
			assignment_[i] = (m.value(v) == value_false ? -1:1)*v;
			i++;
		}
		return true;
	}

}

// C functions for the JNA library interface

using namespace JNA;

void* initConfig(const char* params) {
	JNAProblem* jnaProblem = new JNAProblem();
	// Init the configuration
	Clasp::Cli::ClaspCliConfig* config = new Clasp::Cli::ClaspCliConfig();
	jnaProblem->setConfig(config);
	try {
		const char** it = &params;
		config->setConfig(it, it + 1, Problem_t::SAT);
		jnaProblem->setConfigState(c_CONFIGURED);
	} catch (std::exception& e) {
		jnaProblem->setConfigState(c_ERROR);
		jnaProblem->setConfigErrorMessage("Error while parsing configuration: " + std::string(e.what()));
	}
	return jnaProblem;
}

void initProblem(void* jnaProblemPointer, const char* problem) {
	JNAProblem* jnaProblem = reinterpret_cast<JNA::JNAProblem*>(jnaProblemPointer);
	// Init the facade
	Clasp::ClaspFacade* facade = new Clasp::ClaspFacade();
	jnaProblem->setFacade(facade);
	std::istringstream problemAsStream (problem);
	// Parse the problem
	facade->startSat(*jnaProblem->getConfig()).parseProgram(problemAsStream);
	facade->prepare();
	// Start the solve, passing in the event handler (which JNAProblem implements)
	Clasp::ClaspFacade::AsyncResult* asyncResult = new Clasp::ClaspFacade::AsyncResult(facade->solveAsync(jnaProblem));
	jnaProblem->setAsyncResult(asyncResult);
}

void solveProblem(void* jnaProblemPointer, double timeoutTime) {
	JNAProblem* jnaProblem = reinterpret_cast<JNA::JNAProblem*>(jnaProblemPointer);
	Clasp::ClaspFacade::AsyncResult* asyncResult = jnaProblem->getAsyncResult();

	if (asyncResult->waitFor(timeoutTime)) { // returns true immediately when the problem is solved. Otherwise returns false (and continues solving the problem)
		Clasp::ClaspFacade::Result result = asyncResult->get();
		if (result.sat()) {
			jnaProblem->setResultState(r_SAT);
		} else if (result.unsat()) {
			jnaProblem->setResultState(r_UNSAT);
		} else if (result.interrupted()) {
			jnaProblem->setResultState(r_INTERRUPTED);
		}
	} else { // times up
		jnaProblem->setResultState(r_TIMEOUT);
	} 
	asyncResult->cancel();
}

void destroyProblem(void* jnaProblemPointer) {
	JNA::JNAProblem* jnaProblem = reinterpret_cast<JNA::JNAProblem*>(jnaProblemPointer);
	delete jnaProblem;
}

bool interrupt(void* jnaProblemPointer) {
	JNA::JNAProblem* jnaProblem = reinterpret_cast<JNA::JNAProblem*>(jnaProblemPointer);
	return jnaProblem->interrupt();
}

int getResultState(void* jnaProblemPointer) {
	JNA::JNAProblem* jnaProblem = reinterpret_cast<JNA::JNAProblem*>(jnaProblemPointer);
	return jnaProblem->getResultState();
}

int* getResultAssignment(void* jnaProblemPointer) {
	JNA::JNAProblem* jnaProblem = reinterpret_cast<JNA::JNAProblem*>(jnaProblemPointer);
	return jnaProblem->getAssignment();
}

int getConfigState(void* jnaProblemPointer) {
	JNA::JNAProblem* jnaProblem = reinterpret_cast<JNA::JNAProblem*>(jnaProblemPointer);
	return jnaProblem->getConfigState();	
}

const char* getConfigErrorMessage(void* jnaProblemPointer) {
	JNA::JNAProblem* jnaProblem = reinterpret_cast<JNA::JNAProblem*>(jnaProblemPointer);
	return jnaProblem->getConfigErrorMessage().c_str();
}