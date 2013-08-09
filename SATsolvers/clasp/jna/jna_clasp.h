// Guillaume Saulnier-Comte

#ifndef JNA_CLASP_H
#define JNA_CLASP_H

#include <string>

#include "clasp_options.h"
#include "clasp/clasp_facade.h"

using namespace Clasp;
namespace JNA {

class JNAConfig : public ProgramOptions::AppOptions {
public:
	JNAConfig();
	// status of the ClaspConfig object
	enum Conf_Status { c_not_configured, c_valid, c_error };

	Conf_Status getStatus(); //return the status of the JNA Config
	std::string getErrorMessage();
	std::string getClaspErrorMessage();
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
	JNAProblem(std::string problem);
	Format format() const { return DIMACS; }
	bool	read(ApiPtr api, uint32 properties);
	void	addMinimize(Clasp::MinimizeBuilder&, ApiPtr) {}
	void	getAssumptions(Clasp::LitVec&) {}
	bool	getStatus(); // returns the status of the problem after solve has been called
private:
	std::string problem_;
	bool status_;
};

// Callback to set up the required information (i.e. results)

class JNAResults : public Clasp::ClaspFacade::Callback {
public:
	// called when a state is entered or left
	void state(Event e, ClaspFacade& f) {};

	// Some operation triggered an important event.
	/*
	* \param s The solver that triggered the event.
	* \param e An event that is neither event_state_enter nor event_state_exit.
	*/
	void event(const Solver& s, Event e, ClaspFacade& f);

	//! Some configuration option is unsafe/unreasonable w.r.t the current problem.
	void warning(const char* msg);

	bool getInterrupt();
	bool setInterrupt();
	bool unsetInterrupt();
private:
	bool interupt_;

};



}
#endif
