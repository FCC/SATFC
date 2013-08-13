
#include <string>
#include <iostream>

#include "jna_clasp.h"

using namespace std;
using namespace JNA;

//static string params = "--outf 2 -q /ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/TestCNF/StationPackingSAT3s.cnf --eq=0 --trans-ext=all --sat-prepro=0 --sign-def=2 --del-max=10000 --strengthen=local,1 --del-init-r=800,20000 --loops=no --init-watches=0 --heuristic=Berkmin --del-cfl=F,100 --restarts=L,256 --del-algo=basic,0 --deletion=3,66,3.0 --berk-max=256 --del-grow=1.0,100.0,F,128 --update-act --del-glue=4,0 --update-lbd=2 --reverse-arcs=3 --otfs=0 --berk-huang --del-on-restart=50 --contraction=120 --counter-restarts=3 --local-restarts --lookahead=no --save-progress=10 --counter-bump=180";
//static string Sparams = "--seed=1";
static string Sparams = "--seed=1 -n 1";
static char Sargs[1024];
//static string prostr = "p cnf 5 5\n1 -2 0\n1 -2 3 0\n-1 -3 4 0\n2 5 0\n1 -5 -4 0";
//static string prostr = "p cnf 5 3\n1 0\n-2 0\n-1 4 0";
static string Sprostr = "p cnf 5 3\n1 0\n-2 0\n-1 4 0";

void init()
{
	strcpy(Sargs, Sparams.c_str());
}

void example1()
{
	init();
	cout << "START TEST" << endl;

cout << "args: \"" << Sargs << "\"" << endl;
	JNAConfig* conf = new JNAConfig();
	conf->configure(Sargs);
	cout << conf->getStatus() << endl;
	cout << conf->getErrorMessage() << endl;
	cout << conf->getClaspErrorMessage() << endl;
	cout << "NumModels: " << conf->getConfig()->enumerate.numModels << endl;
	cout << "Problem:\n" << Sprostr << "\n--------------------" << endl;

	JNAProblem problem(Sprostr);
	
	JNAResult result;

	Clasp::ClaspFacade libclasp;
	libclasp.solve(problem, *(conf->getConfig()), &result);

	cout << "Assignment:\n" << result.getAssignment() << endl;

	cout << "END TEST" << endl;
	delete conf;
}

void example2()
{
	init();
	cout << "START TEST" << endl;

int i = 0;
i++;
cout << "args: \"" << Sargs << "\"" << endl;
//	JNAConfig* conf = (JNAConfig*) createConfig(Sparams.c_str(), Sparams.length(), 128);
//	JNAConfig* conf = (JNAConfig*) testSomeShit(Sargs);
//	JNAConfig* conf = (JNAConfig*) createConfig1();
	JNAConfig* conf = new JNAConfig();
	conf->configure(Sargs);
	cout << "Conf Status: " << getConfigStatus(conf) << endl;
	cout << "Conf Error: " << getConfigErrorMessage(conf) << endl;
	cout << "Clasp Error: " << getConfigClaspErrorMessage(conf) << endl;

	// set rand
	
	JNAFacade *facade = (JNAFacade*) createFacade();

	JNAProblem* problem = (JNAProblem*) createProblem(Sprostr.c_str());

	JNAResult* result = (JNAResult*) createResult();

	jnasolve(facade, problem, conf, result);
	cout << "Result state: " << getResultState(result) << endl;

	cout << "Assignment:\n" << getResultAssignment(result) << endl;
	
	destroyFacade(facade);
	destroyConfig(conf);
	destroyConfig(problem);
	destroyConfig(result);
	cout << "END TEST" << endl;
}

void example3()
{
	init();
	JNAConfig* conf;
	conf = new JNAConfig();
	conf->configure(Sargs);
	cout << conf->getStatus() << endl;
	cout << conf->getErrorMessage() << endl;
	cout << conf->getClaspErrorMessage() << endl;
	delete conf;
cout << "-----------------------" << endl;
	init();
	conf = new JNAConfig();
	conf->getConfig()->reset();
	conf->configure(Sargs);
	cout << conf->getStatus() << endl;
	cout << conf->getErrorMessage() << endl;
	cout << conf->getClaspErrorMessage() << endl;
	delete conf;
}

void example4()
{
	JNAConfig* conf;
	conf = new JNAConfig();
	conf->configure(Sargs);
	cout << conf->getStatus() << endl;
	cout << conf->getErrorMessage() << endl;
	cout << conf->getClaspErrorMessage() << endl;
	delete conf;
}

int main()
{
	//example1();
	//example1();
	example2();
	example2();
	//example3();
	//example4();
}
