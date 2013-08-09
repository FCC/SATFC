// Guillaume Saulnier-Comte

#ifndef JNA_CLASP_H
#define JNA_CLASP_H

#include "clasp_options.h"

using namespace Clasp;
namespace JNA {

class JNAConfig : public ProgramOptions::AppOptions {
public:
	JNAConfig();
	enum Conf_Status { c_not_configured, c_valid, c_error };
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

	ClaspConfig		config_;
	ClaspOptions		clasp_;
	Conf_Status		status_;

};



}

#endif
