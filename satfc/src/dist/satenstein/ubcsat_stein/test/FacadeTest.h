//
// Created by pcernek on 7/28/15.
//

#ifndef SATENSTEIN_TESTJNA_H
#define SATENSTEIN_TESTJNA_H

#include <gtest/gtest.h>

extern "C" {
  #include "ubcsat-facade.hpp"
};

class JNATest : public ::testing::Test {

protected:
  UBCSATState* _state;

  const char*_dummyCNF;
  const char*_notSoRandomKsatCNF;
  const char* _miniUnsatCNF;
  const char* _facCNF;

  const char* _defaultSteinParams = "-alg satenstein -cutoff max -seed 1";
  const char* _params1 = "-alg satenstein -adaptive 0 -alpha 1.3 -clausepen 0 -heuristic 5 -maxinc 10 -novnoise 0.3 -performrandomwalk 1 -pflat 0.15  -promisinglist 0 -randomwalk 4 -rfp 0.07 -rho 0.8 -sapsthresh -0.1 -scoringmeasure 1 -selectclause 1  -singleclause 1 -tabusearch 0  -varinfalse 1";
  const char* _params2 = "-alg satenstein -adaptivenoisescheme 1 -adaptiveprom 0 -adaptpromwalkprob 0 -adaptwalkprob 0 -alpha 1.126 -decreasingvariable 3 -dp 0.05 -heuristic 2 -novnoise 0.5 -performalternatenovelty 1 -phi 5 -promdp 0.05 -promisinglist 0 -promnovnoise 0.5 -promphi 5 -promtheta 6 -promwp 0.01 -rho 0.17 -scoringmeasure 3 -selectclause 1 -theta 6 -tiebreaking 1 -updateschemepromlist 3 -wp 0.03 -wpwalk 0.3 -adaptive 1 -clausepen 1 -performrandomwalk 0 -singleclause 0 -smoothingscheme 1 -tabusearch 0 -varinfalse 1";
  const char* _params3 = "-alg satenstein -adaptivenoisescheme 2 -adaptiveprom 0 -adaptpromwalkprob 0 -adaptwalkprob 0 -alpha 1.126 -c 0.0001 -decreasingvariable 3 -dp 0.05 -heuristic 2 -novnoise 0.5 -performalternatenovelty 1 -phi 5 -promdp 0.05 -promisinglist 0 -promnovnoise 0.5 -promphi 5 -promtheta 6 -promwp 0.01 -ps 0.033 -rho 0.8 -s 0.001 -scoringmeasure 3 -selectclause 1 -theta 6 -tiebreaking 3 -updateschemepromlist 3 -wp 0.04  -wpwalk 0.3 -adaptive 0 -clausepen 1 -performrandomwalk 0 -singleclause 0 -smoothingscheme 1 -tabusearch 0 -varinfalse 1";

  JNATest();

  virtual ~JNATest();
};

#endif //SATENSTEIN_TESTJNA_H
