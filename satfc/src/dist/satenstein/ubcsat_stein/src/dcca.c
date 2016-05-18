//
// Created by pcernek on 5/28/15.
//

#include "dcca.h"

BOOL*aCSchanged;

UINT32 *aCSDvars;
UINT32 iNumCSDvars;
UINT32 *aCSDvarsPos;

BOOL*aNVchanged;

UINT32 *aNVDvars;
UINT32 *aNVDvarsPos;
UINT32 iNumNVDvars;

UINT32 *aSDvars;
UINT32 iNumSDvars;
UINT32 *aSDvarsPos;

UINT32 iAvgClauseWeightThreshold;

FLOAT fDCCAp;
FLOAT fDCCAq;

BOOL* aIsCSDvar;
BOOL* aIsNVDvar;
BOOL* aIsSDvar;

void AddDCCA() {

  ALGORITHM *pCurAlg;

  pCurAlg = CreateAlgorithm("dcca","",0,
                            "Double Configuration Checking with Aspiration",
                            "Luo, Cai, Wu, Su [AAAI 2014]",
                            "PickDCCA",
                            "DefaultProcedures,VarPenScore,ClausePenaltyINT,ConfChecking,InitDCCA,Flip+TrackChanges+FCL,VarLastChange,PenClauseList,VarsShareClauses",
                            "default","default");

  AddParmUInt(&pCurAlg->parmList,"-avgweightthreshold",
             "average clause weight threshold [default %s]",
             "on the diversification step, if the average clause weight exceeds~this threshold, smoothing is performed",
             "",&iAvgClauseWeightThreshold,300);
  AddParmFloat(&pCurAlg->parmList,"-DCCAp","DCCA p param [default %s]","weight of current clause score in SWT smoothing [default %s]","",&fDCCAp,0.3);
  AddParmFloat(&pCurAlg->parmList,"-DCCAq","DCCA q param [default %s]","weight of average clause score in SWT smoothing [default %s]","",&fDCCAq,0.0);

  CreateTrigger("PickDCCA",ChooseCandidate,PickDCCA,"","");

  // TODO: Consider moving this to ubcsat-triggers.c
  CreateTrigger("CreateCSDvars",CreateData,CreateCSDvars,"","");
  CreateTrigger("InitCSDvars",PostInit,InitCSDvars,"","");
  CreateTrigger("UpdateCSDvars",UpdateStateInfo,UpdateCSDvars,"","");
  CreateContainerTrigger("CSDvars","CreateCSDvars,InitCSDvars,UpdateCSDvars");

  CreateTrigger("CreateNVDvars",CreateData,CreateNVDvars,"","");
  CreateTrigger("InitNVDvars",PostInit,InitNVDvars,"","");
  CreateTrigger("UpdateNVDvars",UpdateStateInfo,UpdateNVDvars,"","");
  CreateContainerTrigger("NVDvars","CreateNVDvars,InitNVDvars,UpdateNVDvars");

  CreateTrigger("CreateSDvars",CreateData,CreateSDvars,"","");
  CreateTrigger("InitSDvars",PostInit,InitSDvars,"","");
  CreateTrigger("UpdateSDvars",UpdateStateInfo,UpdateSDvars,"","");
  CreateContainerTrigger("SDvars","CreateSDvars,InitSDvars,UpdateSDvars");

  CreateContainerTrigger("ConfChecking","CSDvars,NVDvars,SDvars");

  CreateTrigger("InitDCCA",PostParameters,InitDCCA,"","");
}

void InitDCCA() {
  bPen = TRUE;
}

void PickDCCA() {

  PickCSDvar();

  if (iFlipCandidate == 0) {
    PickNVDvar();
  }

  if (iFlipCandidate == 0) {
    PickSDvar();
  }

  if (iFlipCandidate == 0) {
    PickDCCADiversify();
  }

}

void PickCSDvar() {
  PickBestOldestVar(aCSDvars, iNumCSDvars);
}

void PickNVDvar() {
  PickBestOldestVar(aNVDvars, iNumNVDvars);
}

void PickSDvar() {
  PickBestOldestVar(aSDvars, iNumSDvars);
}

void PickDCCADiversify() {
  UpdateClauseWeightsSWT();
  PickBestVarInRandUNSATClause();
}

void CreateCSDvars() {
  aCSDvars = AllocateRAM((iNumVars+1) * sizeof(UINT32));
  aCSDvarsPos = AllocateRAM((iNumVars+1) * sizeof(UINT32));
  aIsCSDvar = AllocateRAM((iNumVars+1) * sizeof(BOOL));

  aCSchanged = AllocateRAM((iNumVars+1) * sizeof(BOOL));
}

void CreateNVDvars() {
  aNVDvars = AllocateRAM((iNumVars+1) * sizeof(UINT32));
  aNVDvarsPos = AllocateRAM((iNumVars+1) * sizeof(UINT32));
  aIsNVDvar = AllocateRAM((iNumVars+1) * sizeof(BOOL));

  aNVchanged = AllocateRAM((iNumVars+1) * sizeof(BOOL));
}

void CreateSDvars() {
  aSDvars = AllocateRAM((iNumVars+1) * sizeof(UINT32));
  aSDvarsPos = AllocateRAM((iNumVars+1) * sizeof(UINT32));
  aIsSDvar = AllocateRAM((iNumVars+1) * sizeof(BOOL));
}

void InitCSDvars() {
  bPerformClauseConfChecking = TRUE;
  InitDecVarLists(aCSDvars, aCSDvarsPos, &iNumCSDvars, aIsCSDvar);
  InitVarConfiguration(aCSchanged);
}

void InitNVDvars() {
  bPerformNeighborConfChecking = TRUE;
  InitDecVarLists(aNVDvars, aNVDvarsPos, &iNumNVDvars, aIsNVDvar);
  InitVarConfiguration(aNVchanged);
}

/**
 * PRE: Requires that clause penalties have been computed.
 *
 * POST: Populates the list of Significant Decreasing (SD) variables,
 *  in accordance with the "aspiration mechanism" described by
 *  Cai and Su, 2013.
 */
void InitSDvars() {
  UINT32 iVar;
  SINT32 avgClausePenalty = iTotalPenaltyINT / iNumClauses;

  iNumSDvars = 0;

  for (iVar = 1; iVar <= iNumVars; iVar++) {
    aIsSDvar[iVar] = FALSE;
    /* NOTE: DCCA uses positive variable scores, whereas UBCSAT uses
     *  negative variable scores and positive clause penalties. Thus
     *  the DCCA paper defines "significant decreasing" as
     *  VarScore > AvgClausePen, but here we use VarScore < -AvgClausePen. */
    if (GetScore(iVar) < -avgClausePenalty) {
      AddToList2(iVar, aSDvars, aSDvarsPos, &iNumSDvars, aIsSDvar);
    }
  }
}

/**
 * PRE: Flipping variable iFlipCandidate has caused the state of
 *  the given clause to change.
 *
 * POST: All variables that appear in the clause as literals are
 *  are marked as variables whose Clause State-based configuration
 *  has changed since their last flip.
 * POST: The clause state-based configuration of iFlipCandidate is reset.
 */
void UpdateCSchanged(UINT32 toggledClause) {
  UINT32 litIndex;

  for (litIndex = 0; litIndex < aClauseLen[toggledClause]; litIndex++) {
    UINT32 curVar = GetVar(toggledClause, litIndex);
    aCSchanged[curVar] = TRUE;
    /* CHECK: This should not be necessary, since every time a clause changes configuration,
     *  the score of all the variables therein also changes.*/
//    UpdateChangeShallow(curVar);
  }

  aCSchanged[iFlipCandidate] = FALSE;
}

/**
 * PRE: Variable iFlipCandidate has just been flipped.
 *
 * POST: All variables that appear in at least one clause with
 *  iFlipCandidate are marked has having had their neighbor-based
 *  configuration changed.
 * POST: The neighbor-based configuration of iFlipCandidate is reset.
 */
void UpdateNVchanged(UINT32 flippedVar) {
  UINT32 i;

  for (i = 0; i < aNumVarsShareClause[flippedVar]; i++) {
    UINT32 neighborVar = pVarsShareClause[flippedVar][i];
    aNVchanged[neighborVar] = TRUE;
    UpdateChangeShallow(neighborVar);
  }

  aNVchanged[flippedVar] = FALSE;
}

/**
 * PRE: The scores of certain variables have changed, and these variables
 *  have been marked as such by being added to aChangeList.
 * PRE: The Clause State-based configuration of certain variables
 *  has changed.
 * PRE: If using clause weights, these have been updated.
 *
 * POST: The list of variables that are Clause State-based configuration
 *  Decreasing (CSD) is updated.
 */
void UpdateCSDvars() {
  UpdateConfigurationDecreasing(aCSchanged, aCSDvars, aCSDvarsPos, &iNumCSDvars, aIsCSDvar);
}

/**
 * PRE: The scores of certain variables have changed, and these variables
 *  have been marked as such by being added to aChangeList.
 * PRE: The Neighbor Variable-based configuration of certain variables
 *  has changed.
 *
 * POST: The list of variables that are Neighbor Variable-based configuration
 *  Decreasing (NVD) is updated.
 */
void UpdateNVDvars() {
  UpdateConfigurationDecreasing(aNVchanged, aNVDvars, aNVDvarsPos, &iNumNVDvars, aIsNVDvar);
}

/**
 * PRE: The scores of certain variables have changed, and these variables
 *  have been marked as such by being added to aChangeList.
 *
 * POST: The list of variables that are Significant Decreasing (SD)
 *  is updated.
 */
void UpdateSDvars() {
  UINT32 i;
  SINT32 iAvgClausePen = iTotalPenaltyINT / iNumClauses;

  // cycle through all the variables whose scores have changed this step
  for(i = 0; i < iNumChanges; i++) {
    UINT32 iVar = aChangeList[i];

    // add vars that have become "significant decreasing"
    /* NOTE: DCCA uses positive variable scores, whereas UBCSAT uses
     *  negative variable scores and positive clause penalties. Thus
     *  the DCCA paper defines "significant decreasing" as
     *  VarScore > AvgClausePen, but here we use VarScore < -AvgClausePen. */
    if (!aIsSDvar[iVar] && GetScore(iVar) < -iAvgClausePen ) {
      AddToList2(iVar, aSDvars, aSDvarsPos, &iNumSDvars, aIsSDvar);
    }
      // remove vars that are no longer "configuration changed decreasing"
    else if (aIsSDvar[iVar] &&  GetScore(iVar) >= -iAvgClausePen) {
      RemoveFromList2(iVar, aSDvars, aSDvarsPos, &iNumSDvars, aIsSDvar);
    }
  }
}

void UpdateConfigurationDecreasing(BOOL *aConfChanged, UINT32 *aConfDecVars, UINT32 *aConfDecVarsPos,
                                   UINT32 *pNumConfDecVars, BOOL *isConfDecreasing)
{
  UINT32 i;

  // cycle through all the variables whose scores have changed this step
  for(i = 0; i < iNumChanges; i++) {
    UINT32 var = aChangeList[i];

    // add vars that have become "configuration changed decreasing"
    if (!isConfDecreasing[var] && aConfChanged[var] && isDecreasing(var)) {
      AddToList2(var, aConfDecVars, aConfDecVarsPos, pNumConfDecVars, isConfDecreasing);
    }
    // remove vars that are no longer "configuration changed decreasing"
    else if (isConfDecreasing[var] && !(isDecreasing(var) && aConfChanged[var]) ) {
      RemoveFromList2(var, aConfDecVars, aConfDecVarsPos, pNumConfDecVars, isConfDecreasing);
    }
  }
}

/**
 * Find the variable with the best score, breaking ties
 *  in favor of the least-recently flipped variable.
 *
 * PRE: Variable scores and ages have been updated for this step.
 *
 * POST: iFlipCandidate is set to the highest-scoring variable in
 *  the given list. If multiple variables are tied with the highest
 *  score, iFlipCandidate is set to the variable least recently flipped
 *  among them.
 */
void PickBestOldestVar(UINT32 *varList, UINT32 listSize) {
  UINT32 iVar;
  SINT32 iBestScore = bPen ? iTotalPenaltyINT : iNumClauses;
  UINT32 iBestVarAge = iStep;
  UINT32 i;

  for (i = 0; i < listSize; i ++) {
    iVar = varList[i];
    if (GetScore(iVar) < iBestScore ||
        (GetScore(iVar) == iBestScore && aVarLastChange[iVar] < iBestVarAge) )
    {
      iFlipCandidate = iVar;
      iBestScore = GetScore(iVar);
      iBestVarAge = aVarLastChange[iVar];
    }
  }
}

/**
 * Picks an UNSAT clause at random, then picks the variable with the
 *  best score from that clause. If two variables are tied for the
 *  best score, pick the variable that was flipped the least recently.
 */
// TODO: Consider moving to ubcsat-triggers.c
void PickBestVarInRandUNSATClause() {
  UINT32 i;
  SINT32 iScore;
  UINT32 iClause;
  UINT32 iClauseLen;
  UINT32 iVar;
  UINT32 iBestVarAge;
  LITTYPE *pLit;

  if (iNumFalse) {
    iClause = aFalseList[RandomInt(iNumFalse)];
    iClauseLen = aClauseLen[iClause];
  }
  else {
    iFlipCandidate = 0;
    return;
  }

  iBestScore = bPen ? iTotalPenaltyINT : iNumClauses;
  iBestVarAge = iStep;

  /* Find var with best score. */
  pLit = pClauseLits[iClause];
  for (i = 0; i < iClauseLen; i++) {
    iVar = GetVarFromLit(*pLit);
    iScore = GetScore(iVar);

    if (iScore < iBestScore ||
        (iScore == iBestScore && aVarLastChange[iVar] < iBestVarAge) )
    {
      iFlipCandidate = iVar;
      iBestScore = iScore;
      iBestVarAge = aVarLastChange[iVar];
    }
    pLit++;
  }
}

void UpdateClauseWeightsSWT() {
  IncrementUNSATClauseWeights();
  if (iTotalPenaltyINT / iNumClauses > iAvgClauseWeightThreshold) {
    SmoothSWT();
  }
}

/**
 * The SWT smoothing scheme (Cai & Su, "Local Search for Boolean
 *  satisfiability with configuration checking and subscore", 2013).
 *
 * PRE: As with any other smoothing scheme (as far as I know),
 *  assumes bPen == TRUE (since smoothing of clause weights
 *  is meaningless if clause weights are not being used)
 *
 * POST: The clause weights of all currently UNSAT clauses are smoothed.
 * POST: The sum total of all clause penalties is updated accordingly.
 */
void SmoothSWT() {
  UINT32 iFalseClauseIndex, i;
  UINT32 iClause, iVar;
  UINT32 oldClausePen, newClausePen;
  SINT32 penChange;
  LITTYPE *pLit;

  UINT32  iAvgClausePenalty = iTotalPenaltyINT / iNumClauses;

  for (iFalseClauseIndex = 0; iFalseClauseIndex < iNumFalse; iFalseClauseIndex++) {
    iClause = aFalseList[iFalseClauseIndex];
    oldClausePen = aClausePenaltyINT[iClause];
    newClausePen = (UINT32) ((fDCCAp + FLT_EPSILON) * aClausePenaltyINT[iClause]) +
                   (UINT32) ((fDCCAq + FLT_EPSILON) * iAvgClausePenalty);
    if (newClausePen < 1) {
      newClausePen = 1;
    }
    aClausePenaltyINT[iClause] = newClausePen;
    penChange = (newClausePen - oldClausePen);
    iTotalPenaltyINT += penChange;

    /* If decreasing this clause's penalty causes the penalty to go down to 1,
     *  this clause is no longer considered to be penalized. */
    if (newClausePen == 1 && penChange < 0) {
      RemoveFromList1(iClause, aPenClauseList, aPenClauseListPos, &iNumPenClauses);
    }
    /**
     * Else if increasing this clause's penalty causes the penalty to exceed
     *  1, this clause is added to the list of penalized clauses. */
    else if (oldClausePen == 1 && newClausePen >= 2) {
      AddToList1(iClause, aPenClauseList, aPenClauseListPos, &iNumPenClauses);
    }

    /* All variables in this clause have their scores updated in accordance with the change in
     *  clause penalty, for flipping any of these variables would make the clause SAT */
    pLit = pClauseLits[iClause];
    for (i = 0; i < aClauseLen[iClause]; i++) {
      iVar = GetVarFromLit(*pLit);
      UpdateScore(iVar, -penChange);

      /* Update decreasing promising variables. */
      // TODO: Move this out of here for increased modularity, so that this function does not depend on aDecPromVars.
      if (bPromisingList && !aIsDecPromVar[iVar] && (aVarPenScore[iVar] < 0) && (aVarLastChange[iVar] < iStep - 1)) {
        AddToList2(iVar, aDecPromVarsList, aDecPromVarsListPos, &iNumDecPromVars, aIsDecPromVar);
      }
      pLit++;
    }
  }
}

void IncrementUNSATClauseWeights() {
  ScaleSparrow();
}
