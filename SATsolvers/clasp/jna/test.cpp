
#include <string>
#include <iostream>

#include "jna_clasp.h"

using namespace std;
using namespace JNA;

int main()
{
	cout << "START TEST" << endl;
	string params1 = "-n 1 -q";

	char params[1024];
	strcpy(params, params1.c_str());

	JNAConfig* conf = new JNAConfig();
	conf->configure(params);
	cout << conf->getStatus() << endl;
	cout << conf->getErrorMessage() << endl;
	
	cout << conf->getClaspErrorMessage() << endl;

	cout << "NumModels: " << conf->getConfig()->enumerate.numModels << endl;

//	string prostr = "p cnf 5 5\n1 -2 0\n1 -2 3 0\n-1 -3 4 0\n2 5 0\n1 -5 -4 0";
	string prostr = "p cnf 5 3\n1 0\n-2 0\n-1 4 0";

	cout << "Problem:\n" << prostr << "\n-----" << endl;

	JNAProblem problem(prostr);
	
	JNAResult result;

	Clasp::ClaspFacade libclasp;
	libclasp.solve(problem, *(conf->getConfig()), &result);

	cout << "Assignment:\n" << result.getAssignment() << endl;

	cout << "END TEST" << endl;
}
