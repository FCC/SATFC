//
// Created by pcernek on 6/1/15.
//

#ifndef SATENSTEIN_DCCA_H
#define SATENSTEIN_DCCA_H

#include "ubcsat.h"

#include <float.h>

/***** lists of "configuration changed decreasing" vars ******/
/*
 * aCSchanged[v] = whether the clause configuration of variable v has changed
 *                 since the last time v was flipped
 * aCSDvars[i]   = the ith variable for which aCSchanged == TRUE and
 *                 isDecreasing(v) == TRUE
 */
extern BOOL*   aCSchanged;
extern UINT32* aCSDvars;
extern UINT32  iNumCSDvars;
extern UINT32* aCSDvarsPos;
extern BOOL*   aIsCSDvar;

/*
 * aNVchanged[v] = whether the neighbor configuration of variable v has changed
 *                 since the last time v was flipped
 * aNVDvars[i]   = the ith variable for which aNVchanged == TRUE and
 *                 isDecreasing(v) == TRUE
 */
extern BOOL*   aNVchanged;
extern UINT32* aNVDvars;
extern UINT32* aNVDvarsPos;
extern UINT32  iNumNVDvars;
extern BOOL*   aIsNVDvar;

/**
 * aSDvars[i]     = whether the negative score of variable i exceeds
 *                  the average clause weight
 */
extern UINT32* aSDvars;
extern UINT32  iNumSDvars;
extern UINT32* aSDvarsPos;
extern BOOL*   aIsSDvar;

extern UINT32 iAvgClauseWeightThreshold;
extern FLOAT fDCCAp;
extern FLOAT fDCCAq;

void InitDCCA();
void PickDCCA();

void CreateCSDvars();
void CreateNVDvars();
void CreateSDvars();

void InitCSDvars();
void InitNVDvars();
void InitSDvars();

void UpdateCSDvars();
void UpdateNVDvars();
void UpdateSDvars();

void UpdateCSchanged(UINT32 toggledClause);
void UpdateNVchanged(UINT32 flippedVar);
void UpdateConfigurationDecreasing(BOOL *aConfChanged, UINT32 *aConfDecVars, UINT32 *aConfDecVarsPos,
                                   UINT32 *pNumConfDecVars, BOOL *isConfDecreasing);

void UpdateClauseWeightsSWT();

void IncrementUNSATClauseWeights();

void SmoothSWT();

void PickDCCADiversify();

void PickBestVarInRandUNSATClause();

void PickCSDvar();
void PickNVDvar();
void PickSDvar();

void PickBestOldestVar(UINT32 *varList, UINT32 listSize);

#endif //SATENSTEIN_DCCA_H
