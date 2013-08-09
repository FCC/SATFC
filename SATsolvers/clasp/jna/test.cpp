
#include <string>
#include <iostream>

#include "jna_clasp.h"

#include "example2.cpp"

using namespace std;
using namespace JNA;

int main()
{
//	cout << "START TEST" << endl;
	string params1 = "-n 8";

	char params[1024];
	strcpy(params, params1.c_str());

	JNAConfig* conf = new JNAConfig();
	conf->configure(params);
	cout << conf->getStatus() << endl;
	cout << conf->getErrorMessage() << endl;
	
	cout << conf->getClaspErrorMessage() << endl;

	cout << "NumModels: " << conf->getConfig()->enumerate.numModels << endl;

	string prostr = "p cnf 5 5\n1 -2 0\n1 -2 3 0\n-1 -3 4 0\n2 5 0\n1 -5 -4 0";
	JNAProblem problem(prostr);
//	cout << "END TEST" << endl;
	
	example2();
}
