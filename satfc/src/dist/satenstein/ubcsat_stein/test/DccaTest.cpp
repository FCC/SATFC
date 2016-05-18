//
// Created by pcernek on 6/5/15.
//

#include "DccaTest.h"

SINT32* aVarScore;
UINT32* aVarLastChange;
UINT32 iFlipCandidate;

TEST_F(DccaTest, PickCSDvar) {
  initConfChecking();
  iNumCSDvars = _numCSDvars;
  aCSDvars = _csdVarsList;

  PickDCCA();

  EXPECT_EQ(6, iFlipCandidate);
}

TEST_F(DccaTest, PickNVDvar) {
  initConfChecking();
  iNumCSDvars = 0;
  aCSDvars = _csdVarsList;

  iNumNVDvars = _numNVDvars;
  aNVDvars = _nvdVarsList;

  PickDCCA();

  EXPECT_EQ(1, iFlipCandidate);
}

TEST_F(DccaTest, PickSDvar) {
  initConfChecking();
  iNumCSDvars = 0;
  aCSDvars = _csdVarsList;

  iNumNVDvars = 0;
  aNVDvars = _nvdVarsList;

  iNumSDvars = _numSDvars;
  aSDvars = _sdVarsList;

  PickDCCA();

  EXPECT_EQ(4, iFlipCandidate);
}

TEST_F(DccaTest, PickBestVarInRandUNSATClause_noFalse) {
  iNumFalse = 0;
  PickBestVarInRandUNSATClause();
  EXPECT_EQ(0, iFlipCandidate);
}

TEST_F(DccaTest, PickBestVarInRandUNSATClause_oneFalse) {
  initSmoothing();
  initVarScores();
  initVarLastChange();
  iNumFalse = 1;
  iNumVars = 6;

  bPen = FALSE;

  pClauseLits = (LITTYPE **) AllocateRAM( 4 * sizeof(LITTYPE*) );
  pClauseLits[3] = (LITTYPE*) AllocateRAM( iNumVars * sizeof(LITTYPE) );
  pClauseLits[3][0] = GetNegLit(1);
  pClauseLits[3][1] = GetPosLit(2);
  pClauseLits[3][2] = GetPosLit(3);

  aClauseLen = (UINT32 *) AllocateRAM(iNumClauses * sizeof(UINT32));
  aClauseLen[3] = 3;

  PickBestVarInRandUNSATClause();
  EXPECT_EQ(1, iFlipCandidate);
}

TEST_F(DccaTest, SmoothSWTaboveThreshold) {
  initSmoothing();
  fDCCAp = 0.3;
  fDCCAq = 0.7;

  SmoothSWT();

  EXPECT_EQ(5, aClausePenaltyINT[0]);
  EXPECT_EQ(11, aClausePenaltyINT[1]);
  EXPECT_EQ(15, aClausePenaltyINT[2]);
  EXPECT_EQ(14, aClausePenaltyINT[3]);

  EXPECT_EQ(11, iTotalPenaltyINT / iNumClauses);
}

TEST_F(DccaTest, SmoothSWTaboveThresholdInv) {
  initSmoothing();
  fDCCAp = 0.7;
  fDCCAq = 0.3;
  iAvgClauseWeightThreshold = 10;

  SmoothSWT();

  EXPECT_EQ(5, aClausePenaltyINT[0]);
  EXPECT_EQ(10, aClausePenaltyINT[1]);
  EXPECT_EQ(15, aClausePenaltyINT[2]);
  EXPECT_EQ(17, aClausePenaltyINT[3]);
}

TEST_F(DccaTest, UpdateCSchangedToUNSAT) {
  initIndependentClauses();
  setupCSDvars();
  setToFalse(aCSchanged, iNumVars + 1);

  iFlipCandidate = 3; // clause 0 goes from SAT to UNSAT

  FlipTrackChangesFCL();

  EXPECT_TRUE(aCSchanged[1]);
  EXPECT_TRUE(aCSchanged[2]);
  EXPECT_FALSE(aCSchanged[3]);

  EXPECT_FALSE(aCSchanged[4]);
  EXPECT_FALSE(aCSchanged[5]);
  EXPECT_FALSE(aCSchanged[6]);
}

TEST_F(DccaTest, UpdateCSchangedAcrossClausesToUNSAT) {
  initLinkedClauses();
  setupCSDvars();
  setToFalse(aCSchanged, iNumVars + 1);

  iFlipCandidate = 3; // clause 0 goes from SAT to UNSAT

  FlipTrackChangesFCL();

  EXPECT_TRUE(aCSchanged[1]);
  EXPECT_TRUE(aCSchanged[2]);
  EXPECT_FALSE(aCSchanged[3]);

  EXPECT_FALSE(aCSchanged[4]);
}

TEST_F(DccaTest, UpdateCSchangedAcrossClausesToSAT) {
  initLinkedClauses();
  setupCSDvars();
  setToFalse(aCSchanged, iNumVars + 1);

  iFlipCandidate = 2; // clause 1 goes from UNSAT to SAT

  FlipTrackChangesFCL();

  EXPECT_TRUE(aCSchanged[1]);
  EXPECT_FALSE(aCSchanged[2]);
  EXPECT_FALSE(aCSchanged[3]);

  EXPECT_TRUE(aCSchanged[4]);
}

TEST_F(DccaTest, UpdateCSchangedToSAT) {
  initIndependentClauses();
  setupCSDvars();
  setToFalse(aCSchanged, iNumVars + 1);

  iFlipCandidate = 4; // clause 1 goes from UNSAT to SAT

  FlipTrackChangesFCL();

  EXPECT_FALSE(aCSchanged[1]);
  EXPECT_FALSE(aCSchanged[2]);
  EXPECT_FALSE(aCSchanged[3]);

  EXPECT_FALSE(aCSchanged[4]);
  EXPECT_TRUE(aCSchanged[5]);
  EXPECT_TRUE(aCSchanged[6]);
}

TEST_F(DccaTest, UpdateNVchangedSingleClause) {
  initIndependentClauses();
  setupNVDvars();
  setToFalse(aNVchanged, iNumVars + 1);

  iFlipCandidate = 2; // clause 1 goes from UNSAT to SAT

  FlipTrackChangesFCL();

  EXPECT_TRUE(aNVchanged[1]);
  EXPECT_FALSE(aNVchanged[2]);
  EXPECT_TRUE(aNVchanged[3]);

  EXPECT_FALSE(aNVchanged[4]);
  EXPECT_FALSE(aNVchanged[5]);
  EXPECT_FALSE(aNVchanged[6]);
}

TEST_F(DccaTest, UpdateNVchangedCrossClauses) {
  initLinkedClauses();
  setupNVDvars();
  setToFalse(aNVchanged, iNumVars + 1);

  iFlipCandidate = 4;

  FlipTrackChangesFCL();

  EXPECT_TRUE(aNVchanged[1]);
  EXPECT_TRUE(aNVchanged[2]);
  EXPECT_FALSE(aNVchanged[3]);

  EXPECT_FALSE(aNVchanged[4]);
}

TEST_F(DccaTest, UpdateCSDvarsAdd1) {
  initIndependentClauses();
  setupCSDvars();
  bPerformNeighborConfChecking = FALSE;

  EXPECT_TRUE(aIsCSDvar[4]);
  EXPECT_TRUE(aIsCSDvar[5]);
  EXPECT_TRUE(aIsCSDvar[6]);

  iFlipCandidate = 3;

  FlipTrackChangesFCL();
  UpdateCSDvars();

  EXPECT_TRUE(aIsCSDvar[4]);
  EXPECT_TRUE(aIsCSDvar[5]);
  EXPECT_TRUE(aIsCSDvar[6]);

  EXPECT_TRUE(aIsCSDvar[1]);
  EXPECT_TRUE(aIsCSDvar[2]);

}

TEST_F(DccaTest, UpdateCSDvarsRemove1) {
  initIndependentClauses();
  setupCSDvars();
  bPerformNeighborConfChecking = FALSE;

  EXPECT_TRUE(aIsCSDvar[4]);
  EXPECT_TRUE(aIsCSDvar[5]);
  EXPECT_TRUE(aIsCSDvar[6]);

  iFlipCandidate = 4;

  FlipTrackChangesFCL();
  UpdateCSDvars();

  EXPECT_FALSE(aIsCSDvar[4]);
  EXPECT_FALSE(aIsCSDvar[5]);
  EXPECT_FALSE(aIsCSDvar[6]);

}

TEST_F(DccaTest, UpdateCSDvarsNoChange) {
  initIndependentClauses();
  setupCSDvars();
  bPerformNeighborConfChecking = FALSE;

  EXPECT_FALSE(aIsCSDvar[1]);
  EXPECT_FALSE(aIsCSDvar[2]);
  EXPECT_FALSE(aIsCSDvar[3]);
  EXPECT_TRUE(aIsCSDvar[4]);
  EXPECT_TRUE(aIsCSDvar[5]);
  EXPECT_TRUE(aIsCSDvar[6]);

  iFlipCandidate = 2;

  FlipTrackChangesFCL();
  UpdateCSDvars();

  EXPECT_FALSE(aIsCSDvar[1]);
  EXPECT_FALSE(aIsCSDvar[2]);
  EXPECT_FALSE(aIsCSDvar[3]);
  EXPECT_TRUE(aIsCSDvar[4]);
  EXPECT_TRUE(aIsCSDvar[5]);
  EXPECT_TRUE(aIsCSDvar[6]);

}

void DccaTest::initConfChecking() {
  initVarScores();
  initVarLastChange();
  initCSDvars();
  initNVDvars();
  initSDvars();
}

void DccaTest::initIndependentClauses() {
  iNumClauses = 2;
  iNumVars = 6;
  iNumLits = 6;

  bPen = FALSE;

  CreateDefaultStateInfo();
  CreateFalseClauseList();
  CreateTrackChanges();

  pClauseLits = (LITTYPE **) AllocateRAM( 2 * sizeof(LITTYPE*) );
  pClauseLits[0] = (LITTYPE*) AllocateRAM( 3 * sizeof(LITTYPE) );
  pClauseLits[1] = (LITTYPE*) AllocateRAM( 3 * sizeof(LITTYPE) );

  pClauseLits[0][0] = GetPosLit(1);   aVarValue[1] = FALSE;
  pClauseLits[0][1] = GetPosLit(2);   aVarValue[2] = FALSE;
  pClauseLits[0][2] = GetPosLit(3);   aVarValue[3] = TRUE;

  pClauseLits[1][0] = GetPosLit(4);   aVarValue[4] = FALSE;
  pClauseLits[1][1] = GetPosLit(5);   aVarValue[5] = FALSE;
  pClauseLits[1][2] = GetPosLit(6);   aVarValue[6] = FALSE;

  aClauseLen = (UINT32 *) AllocateRAM(iNumClauses * sizeof(UINT32));
  aClauseLen[0] = 3;
  aClauseLen[1] = 3;

  CreateLitOccurence();
  InitDefaultStateInfo();
  InitFalseClauseList();
  InitTrackChanges();

  CreateVarScore();
  InitVarScore();
}

void DccaTest::initLinkedClauses() {
  iNumClauses = 2;
  iNumVars = 4;
  iNumLits = 6;

  bPen = FALSE;

  CreateDefaultStateInfo();
  CreateFalseClauseList();
  CreateTrackChanges();

  pClauseLits = (LITTYPE **) AllocateRAM( 2 * sizeof(LITTYPE*) );
  pClauseLits[0] = (LITTYPE*) AllocateRAM( 3 * sizeof(LITTYPE) );
  pClauseLits[1] = (LITTYPE*) AllocateRAM( 3 * sizeof(LITTYPE) );

  pClauseLits[0][0] = GetPosLit(1);   aVarValue[1] = FALSE;
  pClauseLits[0][1] = GetPosLit(2);   aVarValue[2] = FALSE;
  pClauseLits[0][2] = GetPosLit(3);   aVarValue[3] = TRUE;

  pClauseLits[1][0] = GetPosLit(4);   aVarValue[4] = FALSE;
  pClauseLits[1][1] = GetPosLit(2);
  pClauseLits[1][2] = GetPosLit(1);

  aClauseLen = (UINT32 *) AllocateRAM(iNumClauses * sizeof(UINT32));
  aClauseLen[0] = 3;
  aClauseLen[1] = 3;

  CreateLitOccurence();
  InitDefaultStateInfo();
  InitFalseClauseList();
  InitTrackChanges();

  CreateVarScore();
  InitVarScore();
}

void DccaTest::initVarValues(int numVars) {
  aVarValue = (UINT32*) AllocateRAM( numVars * sizeof(UINT32));
}

void DccaTest::initSmoothing() {
  iNumClauses = 4;
  UINT32 tempClausePenalties[] = {5, 10, 15, 20};
  aClausePenaltyINT = (UINT32 *) AllocateRAM( (iNumClauses) * sizeof(UINT32));
  copyArray(tempClausePenalties, aClausePenaltyINT, iNumClauses);
  iTotalPenaltyINT = sum(aClausePenaltyINT, iNumClauses);

  UINT32 tempFalseList[] = {3,1};
  aFalseList = (UINT32 *) AllocateRAM( (iNumClauses) * sizeof(UINT32));
  copyArray(tempFalseList, aFalseList, iNumClauses);
  iNumFalse = 2;
}

void DccaTest::initSDvars() {
  _numSDvars = 5;
  UINT32 tempSDvarsList[] = {1,2,3,4,5};
  _sdVarsList = (UINT32 *) AllocateRAM( _numSDvars * sizeof(UINT32));
  copyArray(tempSDvarsList, _sdVarsList, _numSDvars);
}

void DccaTest::initNVDvars() {
  _numNVDvars = 2;
  UINT32 tempNVDvarsList[] = {1,3};
  _nvdVarsList = (UINT32 *) AllocateRAM( _numNVDvars * sizeof(UINT32));
  copyArray(tempNVDvarsList, _nvdVarsList, _numNVDvars);
}

void DccaTest::initCSDvars() {
  _numCSDvars = 4;
  UINT32 tempCSDvarsList[] = {1,3,4,6};
  _csdVarsList = (UINT32 *) AllocateRAM( _numCSDvars * sizeof(UINT32));
  copyArray(tempCSDvarsList, _csdVarsList, _numCSDvars);
}

void DccaTest::initVarLastChange() {
  iNumVars = 6;
  UINT32 tempVarLastChange[] = {0,  1,  2,  3,  4,  5,  1};
  aVarLastChange = (UINT32 *) AllocateRAM( (iNumVars + 1) * sizeof(UINT32));
  copyArray(tempVarLastChange, aVarLastChange, iNumVars + 1);
}

void DccaTest::initVarScores() {
  iNumVars = 6;
  SINT32 tempVarScores[] =     {0, -3,  0, -3, -8,  2, -8};
  aVarScore = (SINT32 *) AllocateRAM( (iNumVars + 1) * sizeof(SINT32));
  copyArray(tempVarScores, aVarScore, iNumVars + 1);
}

template <typename T>
void DccaTest::copyArray(T *src, T *dst, UINT32 size) {
  for (int i = 0; i < size; i++) {
    dst[i] = src[i];
  }
}

UINT32 DccaTest::sum(UINT32* array, int size) {
  UINT32 total = 0;
  for (int i=0; i < size; i++) {
    total += array[i];
  }
  return total;
}

DccaTest::DccaTest() {
  iNumHeap = 0;
  iFlipCandidate = 0;
  ubcsat::iStep = 2;
}

DccaTest::~DccaTest() {
  FreeRAM();
}

void DccaTest::setToFalse(BOOL *array, UINT32 size) {
  for (int i = 0; i < size; i++) {
    array[i] = FALSE;
  }
}

void DccaTest::setupNVDvars() {
  CreateNVDvars();
  InitNVDvars();
  CreateVarsShareClauses();
}

void DccaTest::setupCSDvars() {
  CreateCSDvars();
  InitCSDvars();
}