//
// Created by pcernek on 7/27/15.
//

#ifndef SATENSTEIN_JNA_UBCSAT_H
#define SATENSTEIN_JNA_UBCSAT_H

#include <stdio.h>
#include <string>
#include <iostream>

extern "C" {
#include <ubcsat.h>
#include <ubcsat-globals.h>
#include <ubcsat-internal.h>
#include <ubcsat-triggers.h>
};

using namespace ubcsat;

typedef struct ubcsat_state {
  int resultState;
  int errorState;
  int* assignment;
  const char* errorMessage;
  volatile int terminateRun = 0;

  ubcsat_state();
} UBCSATState;

extern "C" {

  /* Public interface (used in JNA bridge) */

  void initLibrary();

  void* initConfig(const char* params);

  int initProblem(void* jnaProblemPointer, const char* problem);

  int initAssignment(void* jnaProblemPointer, const long* assignment, int sizeOfAssignment);

  int solveProblem(void* jnaProblemPointer, double timeoutTime);

  void destroyProblem(void* jnaProblemPointer);

  void interrupt(void* jnaProblemPointer);

  int* getResultAssignment(void* jnaProblemPointer);

  int getResultState(void* jnaProblemPointer);

  const char* getErrorMessage(void* jnaProblemPointer);

  /* Less public interface (used in Java tests) */

  int getNumVars();

  int getNumClauses();

  int getVarAssignment(int varNumber);

  int runInitData();

};

#endif //SATENSTEIN_JNA_UBCSAT_H
