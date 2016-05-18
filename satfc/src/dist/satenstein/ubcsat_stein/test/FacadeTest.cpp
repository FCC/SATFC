//
// Created by pcernek on 7/28/15.
//

#include <chrono>
#include "FacadeTest.h"

TEST_F(JNATest, initProblem) {
  initProblem(_state, _dummyCNF);
  EXPECT_EQ(4, iNumVars);
  EXPECT_EQ(20, iNumClauses);
}

TEST_F(JNATest, initProblemParseComment) {
  EXPECT_TRUE( initProblem(_state, _facCNF) );

  EXPECT_EQ(29, iNumVars);
  EXPECT_EQ(109, iNumClauses);
}

TEST_F(JNATest, initAssignment) {
  initProblem(_state, _dummyCNF);
  long assignment[] = {1, 2, -3};

  EXPECT_TRUE( initAssignment(_state, assignment, 3) );
  RunProcedures(InitData);

  for (int i=0; i < sizeof(assignment) / sizeof(long); i++) {
    EXPECT_EQ(assignment[i] > 0, aVarValue[i + 1]);
  }
}

TEST_F(JNATest, solveDummyProblem) {
  initProblem(_state, _dummyCNF);
  ASSERT_TRUE( solveProblem(_state, 5) );
  ASSERT_EQ(1, _state->resultState);

  int* assignment = getResultAssignment(_state);

  EXPECT_EQ(TRUE, assignment[1]  > 0);
  EXPECT_EQ(FALSE, assignment[2] > 0);
  EXPECT_EQ(TRUE, assignment[3]  > 0);
  EXPECT_EQ(TRUE, assignment[4]  > 0);
}

TEST_F(JNATest, solveFACProblem) {
  initProblem(_state, _facCNF);
  ASSERT_TRUE( solveProblem(_state, 5) );
  ASSERT_EQ(1, _state->resultState);

  int* assignment = getResultAssignment(_state);

  EXPECT_EQ(FALSE, assignment[1]  > 0);
  EXPECT_EQ(FALSE, assignment[4]  > 0);
  EXPECT_EQ(FALSE, assignment[7]  > 0);
  EXPECT_EQ(TRUE, assignment[8]  > 0);
}

/**
 * Verify that state is properly reset within iterations of the same algorithm.
 */
TEST_F(JNATest, solveThreeDifferentProblems) {
  // Problem 1
  initProblem(_state, _dummyCNF);
  solveProblem(_state, 1);
  EXPECT_EQ(1, _state->resultState);
  int* assignment = getResultAssignment(_state);
  EXPECT_EQ(TRUE, assignment[1]  > 0);
  EXPECT_EQ(FALSE, assignment[2] > 0);
  EXPECT_EQ(TRUE, assignment[3]  > 0);
  EXPECT_EQ(TRUE, assignment[4]  > 0);
  destroyProblem(_state);

  // Problem 2
  const char params2[] = "-alg satenstein";
  _state = (UBCSATState *) initConfig(params2);
  bool initSecondProblemSuccess = (bool) initProblem(_state, _facCNF);
  EXPECT_TRUE(initSecondProblemSuccess);
  bool solveSecondProblemSuccess = (bool) solveProblem(_state, 2);
  EXPECT_TRUE(solveSecondProblemSuccess);
  EXPECT_EQ(1, _state->resultState);
  assignment = getResultAssignment(_state);
  EXPECT_EQ(FALSE, assignment[1]  > 0);
  EXPECT_EQ(FALSE, assignment[4]  > 0);
  EXPECT_EQ(FALSE, assignment[7]  > 0);
  EXPECT_EQ(TRUE, assignment[8]  > 0);
  destroyProblem(_state);

  // Problem 3
  const char params3[] = "-alg satenstein -seed 1";
  _state = (UBCSATState *) initConfig(params3);
  bool initThirdProblemSuccess = (bool) initProblem(_state, _notSoRandomKsatCNF);
  EXPECT_TRUE(initThirdProblemSuccess);
  bool solveThirdProblemSuccess = (bool) solveProblem(_state, 2);
  EXPECT_TRUE(solveThirdProblemSuccess);
  EXPECT_EQ(1, _state->resultState);
  assignment = getResultAssignment(_state);
  EXPECT_EQ(TRUE, assignment[1]  > 0);
  EXPECT_EQ(FALSE, assignment[2]  > 0);
  EXPECT_EQ(TRUE, assignment[3]  > 0);
  EXPECT_EQ(FALSE, assignment[4]  > 0);
}

/**
 * To verify that minute state inheritances are not accumulated.
 */
TEST_F(JNATest, oneHundredTimesSameInstance) {
  // pre-test cleanup
  for (int i=0; i < 100; i++) {
    if (_state) {
      destroyProblem(_state);
    }
    const char params[] = "-alg satenstein";
    _state = (UBCSATState *) initConfig(params);
    initProblem(_state, _notSoRandomKsatCNF);
    bool success = (bool) solveProblem(_state, 1);
    ASSERT_TRUE(success);
    ASSERT_EQ(1, _state->resultState);

    int* assignment = getResultAssignment(_state);

    EXPECT_EQ(TRUE, assignment[1]  > 0);
    EXPECT_EQ(FALSE, assignment[2]  > 0);
    EXPECT_EQ(TRUE, assignment[3]  > 0);
    EXPECT_EQ(FALSE, assignment[4]  > 0);
  }
}

/**
 * Ensures that there is no problem switching between configurations for a given algorithm.
 * Note, however, that there is no guarantee of the safety of switching algorithms for a
 *  given instantiation of the UBCSAT library.
 */
TEST_F(JNATest, differentConfigsSameAlgo) {
  destroyProblem(_state);
  _state = (UBCSATState *) initConfig(_params1);
  initProblem(_state, _dummyCNF);
  bool success1 = (bool) solveProblem(_state, 5);
  ASSERT_TRUE(success1);
  ASSERT_EQ(1, _state->resultState);

  int* assignment1 = getResultAssignment(_state);

  EXPECT_EQ(TRUE, assignment1[1]  > 0);
  EXPECT_EQ(FALSE, assignment1[2] > 0);
  EXPECT_EQ(TRUE, assignment1[3]  > 0);
  EXPECT_EQ(TRUE, assignment1[4]  > 0);


  destroyProblem(_state);
  _state = (UBCSATState *) initConfig(_params2);
  initProblem(_state, _dummyCNF);
  bool success2 = (bool) solveProblem(_state, 5);
  ASSERT_TRUE(success2);
  ASSERT_EQ(1, _state->resultState);

  int* assignment2 = getResultAssignment(_state);

  EXPECT_EQ(TRUE, assignment2[1]  > 0);
  EXPECT_EQ(FALSE, assignment2[2] > 0);
  EXPECT_EQ(TRUE, assignment2[3]  > 0);
  EXPECT_EQ(TRUE, assignment2[4]  > 0);


  destroyProblem(_state);
  _state = (UBCSATState *) initConfig(_params3);
  initProblem(_state, _dummyCNF);
  bool success3 = (bool) solveProblem(_state, 5);
  ASSERT_TRUE(success3);
  ASSERT_EQ(1, _state->resultState);

  int* assignment3 = getResultAssignment(_state);

  EXPECT_EQ(TRUE, assignment3[1]  > 0);
  EXPECT_EQ(FALSE, assignment3[2] > 0);
  EXPECT_EQ(TRUE, assignment3[3]  > 0);
  EXPECT_EQ(TRUE, assignment3[4]  > 0);

}

/**
 * Time out on an unsat problem with maximum number of tries
 */
TEST_F(JNATest, timeout) {
  std::chrono::time_point<std::chrono::system_clock> start, end;
  start = std::chrono::system_clock::now();
  ASSERT_TRUE( initProblem(_state, _miniUnsatCNF) );
  double timeout = 0.5;
  ASSERT_TRUE( solveProblem(_state, timeout) );
  end = std::chrono::system_clock::now();
  std::chrono::duration<float> elapsed_seconds = end-start;
  ASSERT_TRUE(elapsed_seconds.count() < timeout + 0.5);
  ASSERT_EQ(2, _state->resultState);
}

TEST_F(JNATest, timeInitProblem) {
  // read a large problem from file
  // XXX: Path string will need to be changed here to accommodate other test machines
  char *cnfBuffer = 0;
  long length;
  FILE * f = fopen ("res/unsat-srpk.cnf", "rb");

  if (f)
  {
    fseek (f, 0, SEEK_END);
    length = ftell (f);
    fseek (f, 0, SEEK_SET);
    cnfBuffer = (char *) malloc (length);
    if (cnfBuffer)
    {
      fread (cnfBuffer, 1, length, f);
    }
    fclose (f);
  }

  // time problem initialization
  std::chrono::time_point<std::chrono::system_clock> start, end;
  start = std::chrono::system_clock::now();

  ASSERT_TRUE( initProblem(_state, cnfBuffer) );

  end = std::chrono::system_clock::now();
  std::chrono::duration<float> elapsed_seconds = end-start;

  ASSERT_TRUE( elapsed_seconds.count() < 0.2);
  ASSERT_EQ( 10255, iNumVars);
  ASSERT_EQ( 180707, iNumClauses);
  free(cnfBuffer);

}

TEST_F(JNATest, reproduceJavaSegfault) {
  destroyProblem(_state);
  // ===== HERE BEGINS A REPLICATION OF THE JAVA TESTS ====
  // testTimeout
  _state = (UBCSATState *) initConfig(_params1);
  initProblem(_state, _facCNF);
  solveProblem(_state, 0.5);
  destroyProblem(_state);

  // testSameProblemHundredTimes
  _state = (UBCSATState *) initConfig(_defaultSteinParams);
  initProblem(_state, _facCNF);
  solveProblem(_state, 0.5);
  destroyProblem(_state);

  // back to advanced params
  _state = (UBCSATState *) initConfig(_params2);
  initProblem(_state, _facCNF);
  solveProblem(_state, 0.5);
  destroyProblem(_state);

  // and back to default
  _state = (UBCSATState *) initConfig(_defaultSteinParams);
  initProblem(_state, _facCNF);
  solveProblem(_state, 0.5);

}

JNATest::JNATest() {
  initLibrary();
  const char params[] = "-alg satenstein -cutoff max -seed 1";
  _state = (UBCSATState *) initConfig(params);

  _dummyCNF = "p cnf 4 20\n-1 2 3 0\n1 -2 4 0\n1 3 4 0\n2 3 -4 0\n-1 -2 -4 0\n1 -2 3 0\n2 -3 4 0\n-2 3 4 0\n-1 3 -4 0\n2 3 4 0\n1 2 -3 0\n1 -3 -4 0\n1 3 -4 0\n-1 2 4 0\n-1 -2 -3 0\n1 2 0\n-2 4 0\n-3 4 0\n2 3 0\n1 2 3 4 0\n";

  _facCNF = "p cnf 29 109\nc Factors encoded in variables 1-3 and 4-6\n2 3 0\n5 6 0\n-7 0\n8 0\n-18 10 12 0\n-18 -10 -12 0\n18 10 -12 0\n18 -10 12 0\n-10 -12 19 0\n10 12 -19 0\n10 -19 0\n12 -19 0\n-20 11 13 19 0\n-20 -11 -13 19 0\n-20 -11 13 -19 0\n-20 11 -13 -19 0\n20 -11 -13 -19 0\n20 11 13 -19 0\n20 11 -13 19 0\n20 -11 13 19 0\n-11 -13 21 0\n-11 -19 21 0\n-13 -19 21 0\n11 13 -21 0\n11 19 -21 0\n13 19 -21 0\n-22 14 21 0\n-22 -14 -21 0\n22 14 -21 0\n22 -14 21 0\n-14 -21 23 0\n14 21 -23 0\n14 -23 0\n21 -23 0\n-24 20 15 0\n-24 -20 -15 0\n24 20 -15 0\n24 -20 15 0\n-20 -15 25 0\n20 15 -25 0\n20 -25 0\n15 -25 0\n-26 22 16 25 0\n-26 -22 -16 25 0\n-26 -22 16 -25 0\n-26 22 -16 -25 0\n26 -22 -16 -25 0\n26 22 16 -25 0\n26 22 -16 25 0\n26 -22 16 25 0\n-22 -16 27 0\n-22 -25 27 0\n-16 -25 27 0\n22 16 -27 0\n22 25 -27 0\n16 25 -27 0\n-28 23 17 27 0\n-28 -23 -17 27 0\n-28 -23 17 -27 0\n-28 23 -17 -27 0\n28 -23 -17 -27 0\n28 23 17 -27 0\n28 23 -17 27 0\n28 -23 17 27 0\n-23 -17 29 0\n-23 -27 29 0\n-17 -27 29 0\n23 17 -29 0\n23 27 -29 0\n17 27 -29 0\n9 -1 -4 0\n-9 1 0\n-9 4 0\n10 -2 -4 0\n-10 2 0\n-10 4 0\n11 -3 -4 0\n-11 3 0\n-11 4 0\n12 -1 -5 0\n-12 1 0\n-12 5 0\n13 -2 -5 0\n-13 2 0\n-13 5 0\n14 -3 -5 0\n-14 3 0\n-14 5 0\n15 -1 -6 0\n-15 1 0\n-15 6 0\n16 -2 -6 0\n-16 2 0\n-16 6 0\n17 -3 -6 0\n-17 3 0\n-17 6 0\n9 -7 0\n-9 7 0\n18 -7 0\n-18 7 0\n24 -7 0\n-24 7 0\n26 -8 0\n-26 8 0\n28 -7 0\n-28 7 0\n29 -7 0\n-29 7 0\n";

  _notSoRandomKsatCNF = "c A SAT instance generated from a 5-CNF formula that had 80 clauses and 7 variables\np cnf 7 84\n1 -4 2 -5 -6 0\n-4 -3 5 -7 -1 0\n-6 2 3 -5 1 0\n4 -7 2 -5 1 0\n4 -5 1 3 7 0\n-2 1 6 3 4 0\n-7 2 6 5 -4 0\n-6 7 -2 1 5 0\n3 7 5 -1 -2 0\n-1 7 5 3 2 0\n-6 -1 3 -4 -5 0\n-7 5 -3 -2 4 0\n7 -1 -4 -3 2 0\n7 -3 1 4 5 0\n-4 -7 6 -2 5 0\n-7 4 -3 -1 -2 0\n2 3 -4 -5 6 0\n-3 4 6 7 -2 0\n1 -6 3 2 -5 0\n-7 -3 4 -6 2 0\n5 3 1 -4 -7 0\n4 3 5 -7 2 0\n7 4 -1 -5 -3 0\n3 1 2 -4 5 0\n-1 -7 -3 -4 -5 0\n-1 -3 4 -6 5 0\n4 -5 -6 2 -1 0\n-3 7 -2 -4 -6 0\n2 5 -6 -7 -1 0\n2 -7 -3 -6 -1 0\n-2 6 4 -1 -5 0\n5 -6 4 7 -2 0\n7 -5 2 3 -1 0\n-1 -2 7 6 -3 0\n7 1 -2 6 -4 0\n3 4 -5 7 1 0\n5 2 6 -4 -3 0\n4 3 2 -1 -6 0\n4 2 5 -6 -1 0\n-4 7 3 -2 6 0\n-7 -6 5 -3 2 0\n5 -1 -4 2 6 0\n4 5 -6 3 -2 0\n1 -6 -3 2 4 0\n-3 1 4 5 6 0\n1 -2 7 6 4 0\n3 -2 -6 -5 1 0\n-2 -1 4 6 7 0\n-6 -5 4 -2 7 0\n-5 6 4 -2 -7 0\n-4 -1 7 -3 -6 0\n-7 -4 -5 -2 -3 0\n-4 3 5 -6 -2 0\n7 -1 4 2 6 0\n-1 -2 -7 -5 6 0\n2 7 -4 -1 6 0\n-5 -2 1 4 6 0\n1 7 4 3 5 0\n-6 2 -4 3 -7 0\n-1 3 -7 -6 -5 0\n7 -4 1 -3 5 0\n-6 2 1 7 3 0\n-1 4 -3 -6 -2 0\n-2 -5 6 -3 -1 0\n4 1 -6 2 -3 0\n1 4 7 -6 -3 0\n-4 7 -1 -2 -5 0\n2 7 6 5 -4 0\n-2 -6 3 1 -5 0\n-1 3 5 -6 2 0\n-5 -1 7 -4 6 0\n3 -4 6 5 -2 0\n-1 -4 -6 2 -3 0\n-4 5 3 7 -1 0\n7 -5 -2 1 -4 0\n3 6 -7 1 2 0\n-6 1 2 7 3 0\n5 -3 6 4 2 0\n4 1 6 -5 -3 0\n-1 -6 -3 -5 -7 0\n1 0\n-2 0\n3 0\n-4 0";

  _miniUnsatCNF = "p cnf 4 8\n-1 2 4 0\n-2 3 4 0\n1 -3 4 0\n1 -2 -4 0\n2 -3 -4 0\n-1 3 -4 0\n1 2 3 0\n-1 -2 -3 0";

}

JNATest::~JNATest() {
  destroyProblem(_state);
}
