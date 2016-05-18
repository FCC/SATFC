//
// Created by pcernek on 6/5/15.
//

#ifndef SATENSTEIN_TESTDCCA_H
#define SATENSTEIN_TESTDCCA_H

#include <gtest/gtest.h>

extern "C" {
  #include <ubcsat.h>
  #include <ubcsat-globals.h>
  #include <dcca.h>
};

class DccaTest : public ::testing::Test {

protected:

  UINT32 _numCSDvars;
  UINT32*_csdVarsList;

  UINT32 _numNVDvars;
  UINT32* _nvdVarsList;

  UINT32 _numSDvars;
  UINT32* _sdVarsList;


  DccaTest();
  ~DccaTest();

  template <typename T>
  void copyArray(T *src, T *dst, UINT32 size);

  static UINT32 sum(UINT32 *pInt, int size);

  void initVarScores();

  void initVarLastChange();

  void initCSDvars();

  void initNVDvars();

  void initSDvars();

  void initConfChecking();

  void initSmoothing();

  void initIndependentClauses();

  void initVarValues(int numVars);

  void setToFalse(BOOL *array, UINT32 size);

  void initLinkedClauses();

  void setupNVDvars();

  void setupCSDvars();
};


#endif //SATENSTEIN_TESTDCCA_H
