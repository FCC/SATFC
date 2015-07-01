// Guillaume Saulnier-Comte

#ifndef JNA_CLASP_H
#define JNA_CLASP_H

#include <string>

#include <clasp/cli/clasp_options.h>
#include "clasp/clasp_facade.h"

using namespace Clasp;
namespace JNA {

	enum Result_State { r_UNSAT=0, r_SAT=1, r_TIMEOUT=2, r_INTERRUPTED=3, r_UNKNOWN=4 };
	enum Config_State { c_UNCONFIGURED=0, c_CONFIGURED=1, c_ERROR=2 };

	/*
	 * A holder class for all of the variables that we need to solve a problem
	 * Extends EventHandler so that we can override the onEvent style functions (e.g. to save the assignment on completion)
	 * This class is meant to be for a single use: every new SAT problem should construct a new instance of this class and destroy it upon completion
	 */
	class JNAProblem : public Clasp::EventHandler {
		public:
			JNAProblem();
			~JNAProblem();

			// Getters and setters
			Result_State getResultState();
			Config_State getConfigState();
			int* getAssignment();
			std::string getConfigErrorMessage();
			Clasp::ClaspFacade* getFacade();
			Clasp::Cli::ClaspCliConfig* getConfig();
			Clasp::ClaspFacade::AsyncResult* getAsyncResult();
			void setFacade(Clasp::ClaspFacade* facade);
			void setAsyncResult(Clasp::ClaspFacade::AsyncResult* asyncResult);
			void setConfig(Clasp::Cli::ClaspCliConfig* config);
			void setResultState(Result_State state);
			void setConfigState(Config_State configState);
			void setConfigErrorMessage(std::string message);

			// Interrupt the clasp facade: should return false
			bool interrupt();

			// EventHandler functions - onModel is where we can pick up the assignment variables
			bool onModel(const Clasp::Solver& s, const Clasp::Model& m);
		private:
			int* assignment_;
			Result_State resultState_;
			Config_State configState_;
			Clasp::ClaspFacade* facade_;
			Clasp::ClaspFacade::AsyncResult* asyncResult_; // note: asyncResult is not thread safe
			Clasp::Cli::ClaspCliConfig* config_;
			std::string configErrorMessage_;
	};

}

// JNA Library
extern "C" {

	void* initConfig(const char* params);

	void initProblem(void* jnaProblemPointer, const char* problem);
	
	void solveProblem(void* jnaProblemPointer, double timeoutTime);
	
	void destroyProblem(void* jnaProblemPointer);

	bool interrupt(void* jnaProblemPointer);

	int getResultState(void* jnaProblemPointer);

	int getConfigState(void* jnaProblemPointer);

	int* getResultAssignment(void* jnaProblemPointer);

	const char* getConfigErrorMessage(void* jnaProblemPointer);
}
#endif
