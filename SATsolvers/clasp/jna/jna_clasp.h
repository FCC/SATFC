// Guillaume Saulnier-Comte

#ifndef JNA_CLASP_H
#define JNA_CLASP_H

#include <string>

#include "clasp_options.h"
#include "clasp/clasp_facade.h"
#include "clasp/solve_algorithms.h"

using namespace Clasp;
namespace JNA {

class JNAConfig : public ProgramOptions::AppOptions {
public:
	JNAConfig();

	// status of the ClaspConfig object
	enum Conf_Status { c_not_configured=0, c_valid=1, c_error=2 };

	Conf_Status getStatus(); //return the status of the JNA Config
	std::string getErrorMessage(); //return error message of ClaspConfig
	std::string getClaspErrorMessage(); //return error message of ClaspOptions
	void configure(char* args, int maxArgs=128); // use the args to configure config_

	ClaspConfig* getConfig();

private:

	// -------------------------------------------------------------------------------------------
	// AppOptions interface
	void	printHelp(const ProgramOptions::OptionContext& root)	{};
	void	printVersion(const ProgramOptions::OptionContext& root)	{};
	//HelpOpt	initHelpOption() const; //use the virtual implementation
	void	initOptions(ProgramOptions::OptionContext& root) {
		clasp_.initOptions(root, config_);
	}
	bool	validateOptions(const ProgramOptions::OptionContext& root, const ProgramOptions::ParsedOptions& vm, ProgramOptions::Messages& m) {
		return clasp_.validateOptions(root, vm, m);
        }
        // -------------------------------------------------------------------------------------------

	ClaspConfig	config_;
	ClaspOptions	clasp_;
	Conf_Status	status_;
	std::string	err_message_;
};


// JNAProblem holder
class JNAProblem : public Clasp::Input {
public:
	JNAProblem(std::string problem);// problem mus be a string in dimacs format
	Format format() const { return DIMACS; }
	bool	read(ApiPtr api, uint32 properties);
	void	addMinimize(Clasp::MinimizeBuilder&, ApiPtr) {}
	void	getAssumptions(Clasp::LitVec&) {}

	bool	getStatus(); // returns the output of the read function after solve has been called on the problem
private:
	std::string problem_;
	bool status_;
};

// Callback to set up the required information (i.e. results)

class JNAResult : public Clasp::ClaspFacade::Callback {
public:
	JNAResult();	

	// redefine the event to make it easier while writing code
	typedef Clasp::ClaspFacade::Event Event;

	void state(Event e, ClaspFacade& f) {};

	// Some operation triggered an important event.
	/*
	* \param s The solver that triggered the event.
	* \param e An event that is neither event_state_enter nor event_state_exit.
	*/
	void event(const Solver& s, Event e, ClaspFacade& f);

	//! Some configuration option is unsafe/unreasonable w.r.t the current problem.
	void warning(const char* msg);

	enum Result_State { r_UNSAT=0, r_SAT=1, r_UNKNOWN=3 };

	// will return the state of the call to solve.  if r_SAT, assignment_ contains the true litterals.
	Result_State getState();

	// used by the solve function to set the result to unsat, set the state of the solver.
	void setState(Result_State state);

	std::string getWarning();

	std::string getAssignment();
private:
	Result_State state_;
	std::string warning_; // warning message if any.
	std::string assignment_; // contains the true literals.
};

class JNAFacade : public Clasp::ClaspFacade {
public:
	JNAFacade();
	bool interrupt();
};

// solves the problem given the config storing the state of the call and the assignment in result if the problem is SAT.
void solve(JNAFacade& facade, JNAProblem& problem, JNAConfig& config, JNAResult& result);

}// end JNA namespace

// JNA Library
extern "C" {
	// Configuration of clasp
	void* createConfig(const char* _params, int _params_strlen, int _maxArgs);
	void destroyConfig(void* _config);
	int getConfigStatus(void* _config); //return the status of the JNA Config
	const char* getConfigErrorMessage(void* _config); //return error message of ClaspConfig
	const char* getConfigClaspErrorMessage(void* _config); //return error message of ClaspOptions

	// Configuration of the problem instance
	void* createProblem(const char* _problem);
	void destroyProblem(void* _problem);
	int getProblemStatus(void* _problem);

	// functions of the result class
	void* createResult();
	void destroyResult(void* _result);
        int getResultInterrupt(void* _result);
        void setResultInterrupt(void* _result);
        void unsetResultInterrupt(void* _result);
        int getResultState(void* _result);
        const char* getResultWarning(void* _result);
        const char* getResultAssignment(void* _result);

	// creates and destroys facades -> handles interrupts
	void* createFacade();
	void destroyFacade(void* _facade);
	int interrupt(void* _facade);

	// solves the problem given the configuration.  Results are stored into results.
	void jnasolve(void* _facade, void* _problem, void* _config, void* _result);
}
#endif
