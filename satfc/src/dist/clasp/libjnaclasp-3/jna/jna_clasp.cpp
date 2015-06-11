// Guillaume Saulnier-Comte
// Neil Newman
#include <iostream>
#include <sstream>

#include "jna_clasp.h"
#include <clasp/solver.h>
#include <clasp/enumerator.h>

/*
 * At the time of writing, we are using clasp 3.0.5, and the most recent version is 3.1.2.
 * There is a bug in the clasp 3.0.5 source code that is fixed in 3.1.2 that I had to backport
 * The bug is in clasp_facade.cpp in the wait() method, right before the return, where join() is called.
 * I'm just writing this here to document that the clasp source code is not an exact 3.0.5 match
 */

namespace JNA {

	JNAProblem::JNAProblem() {
		resultState_ = r_UNKNOWN;
		configState_ = c_UNCONFIGURED;
		assignment_ = NULL;
		facade_ = NULL;
		config_ = NULL;
		configAllocated_ = false;
		asyncResult_ = NULL;
	}

	JNAProblem::~JNAProblem() {
		delete[] assignment_;
		assignment_ = NULL;
		if (asyncResult_ != NULL && asyncResult_->running()) {
			asyncResult_->cancel();
		}
		delete asyncResult_;
		asyncResult_ = NULL;
		delete facade_;
		facade_ = NULL;
		if (config_ != NULL && configAllocated_) {
			config_->releaseConfig(configKey_);	
			configAllocated_ = false;
		}
		delete config_;
		config_ = NULL;
	}

	void JNAProblem::setConfigKey(Clasp::Cli::ConfigKey configKey) {
		configKey_ = configKey;
		configAllocated_ = true;
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
		return asyncResult_->cancel(); 
	}

	bool JNAProblem::onModel(const Clasp::Solver& s, const Clasp::Model& m) {
		const Clasp::SymbolTable& index = s.symbolTable();
		delete[] assignment_;
		assignment_ = new int[index.size()];
		assignment_[0] = index.size();
		int i = 1;
		for (Var v = 1; v < index.size(); ++v)
		{
			assignment_[i] = (s.value(v) == value_false ? -1:1)*v;
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
	Clasp::Cli::ConfigKey key = config->allocConfig();
	jnaProblem->setConfigKey(key);
	try {
		config->appendConfig(key, "SATFC-Config", params);	
		config->init(0, key);	
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
	} else if (asyncResult->cancel()) { // times up - try to shut down the problem
		jnaProblem->setResultState(r_TIMEOUT);
	} 
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