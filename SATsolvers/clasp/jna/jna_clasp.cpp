// Guillaume Saulnier-Comte
#include <iostream>
#include <sstream>

#include "jna_clasp.h"
#include "clasp/reader.h"
//#include "program_opts/composite_value_parser.h"

namespace JNA {

// JNAConfig

JNAConfig::JNAConfig() : status_(c_not_configured) {}

JNAConfig::Conf_Status JNAConfig::getStatus()
{
	return status_;
}

std::string JNAConfig::getErrorMessage()
{
	return err_message_;
}

std::string JNAConfig::getClaspErrorMessage()
{
	return messages.error;
}

// call to configure the JNAConfig
void JNAConfig::configure(char* args, int maxArgs)
{
	if (status_ != c_not_configured)
	{
		config_.reset();
	}
	err_message_ = "";
	// first we need to simulate the argc and argv that are given to the run function in clasp_app.h run().
	int argc = 1;
	char* argv[maxArgs];
	// set the program executable
	char *prog = "jna_clasp";
	argv[0] = prog;
	char* arg = strtok(args, " ");
	while (arg && argc < maxArgs)
	{
		argv[argc] = arg;
		argc++;
		arg = strtok(0, " ");
	}

	// too many arguments present
	if (argc >= maxArgs)
	{
		status_ = c_error;
		err_message_ = "Too many arguments were given, call the constructor with a higer value of maxArgs! (defaul=128)";
	}

	// call the init options to set up the configuration
	bool value = parse(argc, argv, "jna_clasp", Clasp::parsePositional);

	// set the error function
	if (value)
	{
		status_ = c_valid;
	}
	else
	{
		status_ = c_error;
		err_message_ = "Parsing of the command line arguments failed!  Please test with the clasp executable.";
	}
}

ClaspConfig* JNAConfig::getConfig()
{
	return &config_;
}

// JNAProblem

JNAProblem::JNAProblem(std::string problem)
{
	problem_ = problem;
}

bool JNAProblem::read(ApiPtr p, uint32 properties) {
	std::istringstream istr(problem_);
	status_ = Clasp::parseDimacs(istr, *(p.ctx)); 
	return status_;
}

bool JNAProblem::getStatus()
{
	return status_;
}


// JNAResults

JNAResult::JNAResult() : interrupt_(false), state_(r_UNKNOWN) {}

void JNAResult::state(Event e, ClaspFacade& f)
{
	if (interrupt_)
	{
		setState(JNAResult::r_INTERRUPT);
		f.terminate();
	}
}

// save the last result in a string object.
void JNAResult::event(const Solver& s, Event e, ClaspFacade&f)
{
	if (e == Clasp::ClaspFacade::event_model) {
		assignment_.clear();
		std::ostringstream ostr;
		const Clasp::SymbolTable& index = s.sharedContext()->symTab();
		for (Var v = 1; v < index.size(); ++v)
		{
			ostr << (s.value(v) == value_false ? "-":"") << v << ";";
		}
		assignment_ = ostr.str();
		assignment_.erase(assignment_.size()-1);
		state_ = r_SAT;
	}
}

void JNAResult::warning(const char* msg)
{
	warning_.assign(msg);
}

bool JNAResult::getInterrupt()
{
	return interrupt_;
}

void JNAResult::setInterrupt()
{
	interrupt_ = true;
}

void JNAResult:: unsetInterrupt()
{
	interrupt_ = false;
}

std::string JNAResult::getWarning()
{
	return warning_;
}

std::string JNAResult::getAssignment()
{
	return assignment_;
}

JNAResult::Result_State JNAResult::getState()
{
	return state_;
}

void JNAResult::setState(JNAResult::Result_State state)
{
	state_ = state;
}

void solve(JNAProblem &problem, JNAConfig &config, JNAResult &result)
{
	Clasp::ClaspFacade libclasp;
	libclasp.solve(problem, *(config.getConfig()), &result);
	if (result.getState() != JNAResult::r_SAT)
	{
		result.setState(JNAResult::r_UNSAT);
	}
}

} // end JNA namespace

// C functions for the JNA library interface

void* createConfig(const char* _params, int _params_strlen, int _maxArgs)
{
	char args[_params_strlen];
	strcpy(args, _params);
	JNA::JNAConfig* config = new JNA::JNAConfig();
	config->configure(args, _maxArgs);
	return config;
}

void destroyConfig(void* _config)
{
	delete reinterpret_cast<JNA::JNAConfig*>(_config);
}

int getConfigStatus(void* _config)
{
	JNA::JNAConfig* config = reinterpret_cast<JNA::JNAConfig*>(_config);
	return config->getStatus();
}

const char* getConfigErrorMessage(void* _config)
{
	JNA::JNAConfig* config = reinterpret_cast<JNA::JNAConfig*>(_config);
	return config->getErrorMessage().c_str();
}

const char* getConfigClaspErrorMessage(void* _config)
{
	JNA::JNAConfig* config = reinterpret_cast<JNA::JNAConfig*>(_config);
	return config->getClaspErrorMessage().c_str();
}

void* createProblem(const char* _problem)
{
	std::string problem_str(_problem);
	JNA::JNAProblem* problem = new JNA::JNAProblem(problem_str);
	return problem;
}

void destroyProblem(void* _problem)
{
	delete reinterpret_cast<JNA::JNAProblem*>(_problem);
}

int getProblemStatus(void* _problem)
{
	JNA::JNAProblem* problem = reinterpret_cast<JNA::JNAProblem*>(_problem);
	return problem->getStatus();
}

void* createResult()
{
	JNA::JNAResult* result = new JNA::JNAResult();
	return result;
}

void destroyResult(void* _result)
{
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	delete result;
}

int getResultInterrupt(void* _result)
{
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	return result->getInterrupt();
}

void setResultInterrupt(void* _result)
{
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	result->setInterrupt();
}

void unsetResultInterrupt(void* _result)
{
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	result->unsetInterrupt();
}

int getResultState(void* _result)
{
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	return result->getState();
}

const char* getResultWarning(void* _result)
{
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	return result->getWarning().c_str();
}

const char* getResultAssignment(void* _result)
{
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	return result->getAssignment().c_str();
}

void jnasolve(void* _problem, void* _config, void* _result)
{
	JNA::JNAProblem* problem = reinterpret_cast<JNA::JNAProblem*>(_problem);
	JNA::JNAConfig* config = reinterpret_cast<JNA::JNAConfig*>(_config);
	JNA::JNAResult* result = reinterpret_cast<JNA::JNAResult*>(_result);
	JNA::solve(*problem, *config, *result);
}
