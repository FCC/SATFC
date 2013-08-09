// Guillaume Saulnier-Comte
#include <iostream>
#include <sstream>

#include "jna_clasp.h"
#include "clasp/reader.h"

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
	err_message_ = "No error.";
	// first we need to simulate the argc and argv that are given to the run function in clasp_app.h run().
	int argc = 1;
	char* argv[maxArgs];
	
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










}
