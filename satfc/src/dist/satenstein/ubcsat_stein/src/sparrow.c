//
// Created by pcernek on 6/4/15.
//

/*

      ##  ##  #####    #####   $$$$$   $$$$   $$$$$$
      ##  ##  ##  ##  ##      $$      $$  $$    $$
      ##  ##  #####   ##       $$$$   $$$$$$    $$
      ##  ##  ##  ##  ##          $$  $$  $$    $$
       ####   #####    #####  $$$$$   $$  $$    $$
  ======================================================
  SLS SAT Solver from The University of British Columbia
  ======================================================
  ...... Developed & Maintained by Dave Tompkins .......
  ------------------------------------------------------
  ...... consult legal.txt for legal information .......
  ------------------------------------------------------
  .... project website: http://ubcsat.dtompkins.com ....
  ------------------------------------------------------
  source repository: https://github.com/dtompkins/ubcsat
  ------------------------------------------------------
  ..... contact us at ubcsat [@] googlegroups.com ......
  ------------------------------------------------------

*/

#include "sparrow.h"

#ifdef __cplusplus
namespace ubcsat {
#endif

/*
    Note: For consistency, we use the same weighted promising variable scheme as in gNovelty+, which has some quirks,
    especially during smoothing & scaling updates.
*/

FLOAT fSparrowC1;
UINT32 iSparrowC2;
FLOAT fSparrowC3;
FLOAT fInvSparrowC3;

FLOAT aSparrowPreCalc[11];

FLOAT *aSparrowWeights;


void AddSparrow() {

  ALGORITHM *pCurAlg;

  pCurAlg = CreateAlgorithm("sparrow","",0,
                            "Sparrow",
                            "Balint, Froehlich [SAT 10]",
                            "PickSparrow",
                            "DefaultProcedures,FlipSparrow,DecPromVars,FalseClauseList,VarLastChange,ClausePenaltyINT,PenClauseList,VarsShareClauses,CreateSparrowWeights,InitSparrow,VarPenScore",
                            "default","default");

  AddParmProbability(&pCurAlg->parmList,"-ps","smooth probabilty [default %s]","after a scaling step, ~smooth penalties with probability PR","",&iPs,0.347);

  AddParmFloat(&pCurAlg->parmList,"-c1","sparrow score adjustment parameter [default %s]","adjusts the importance of the score","",&fSparrowC1,2.0);
  AddParmUInt(&pCurAlg->parmList,"-c2","sparrow age polynomial parameter [default %s]","adjusts the influence of the age","",&iSparrowC2,4);
  AddParmFloat(&pCurAlg->parmList,"-c3","sparrow age threshold parameter [default %s]","threshold for age calculation","",&fSparrowC3,100000.0);

  CreateTrigger("InitSparrow",PostRead,InitSparrow,"","");

  CreateTrigger("PickSparrow",ChooseCandidate,PickSparrow,"","");
  CreateTrigger("FlipSparrow",FlipCandidate,FlipSparrow,"DecPromVars,FalseClauseList","DefaultFlip,UpdateFalseClauseList,UpdateVarPenScore");
  CreateTrigger("CreateSparrowWeights",CreateStateInfo,CreateSparrowWeights,"","");

  CreateTrigger("CreateSparrowScore",CreateStateInfo,CreateSparrowScore,"CreateClausePenaltyINT","");
  CreateTrigger("InitSparrowScore",InitStateInfo,InitSparrowScore,"InitClausePenaltyINT","");
  CreateContainerTrigger("SparrowScore","CreateSparrowScore,InitSparrowScore");


  pCurAlg = CreateAlgorithm("sparrow","sat11",0,
                            "Sparrow [SAT 11 Competition]",
                            "Balint, Froehlich [SAT 10]",
                            "PickSparrow,SparrowSat2011Settings",
                            "DefaultProcedures,FlipSparrow,DecPromVars,FalseClauseList,VarLastChange,ClausePenaltyINT,PenClauseList,VarsShareClauses,CreateSparrowWeights,VarPenScore",
                            "default","default");

  CreateTrigger("SparrowSat2011Settings",PostRead,SparrowSat2011Settings,"","");
}

void SparrowSat2011Settings() {
  // These settings provided by Adrian Balint for the SAT 2011 competition
  if (iMaxClauseLen < 4) {
    fSparrowC1 = 2.15;
    iSparrowC2 = 4;
    fSparrowC3 = 100000.0;
    iPs = FloatToProb(0.347);
  } else {
    if (iMaxClauseLen < 6) {
      fSparrowC1 = 2.85;
      iSparrowC2 = 4;
      fSparrowC3 = 75000.0;
      iPs = FloatToProb(1.0);
    } else {
      fSparrowC1 = 6.5;
      iSparrowC2 = 4;
      fSparrowC3 = 100000.0;
      iPs = FloatToProb(0.83);
    }
  }
  InitSparrow();
}

void InitSparrow() {
  SINT32 j;
  for (j=0;j <= 10; j++) {
    aSparrowPreCalc[j] = pow(fSparrowC1,-j);
  }
  fInvSparrowC3 = 1.0f / fSparrowC3;
}

void PickSparrow() {
  bPen = TRUE;

  iFlipCandidate = 0;

  if (iNumFalse==0) {
    return;
  }

  // first, try the greedy approach using promising variables
  // TODO: This should in principle be replaceable by an already-existing implementation
  if (iNumDecPromVars > 0 ) {
    PickGNoveltyPlusProm();
  }

  if (iFlipCandidate != 0) {
    return;
  }

  // if the greedy approach fails, then try choosing according to Sparrow's
  //  probability distribution method
  PickSparrowProbDist();

  // with a parametrized probability, perform either smoothing or scaling
  if (RandomProb(iPs)) {
    SmoothSparrow();
  } else {
    ScaleSparrow();
  }
}

void PickSparrowProbDist() {
  UINT32 iClause;
  UINT32 iClauseLen;
  UINT32 iVar;
  SINT32 iScoreAdjust;
  LITTYPE *pLit;
  UINT32 j;
  UINT32 k;

  FLOAT fScoreProb;
  FLOAT fAgeProb;
  FLOAT fBaseAgeProb;
  FLOAT fScoreSum;
  FLOAT fScorePos;

  fScoreSum = 0.0;

  // pick a false clause at random
  iClause = aFalseList[RandomInt(iNumFalse)];
  iClauseLen = aClauseLen[iClause];

  pLit = pClauseLits[iClause];

  for (j=0;j<iClauseLen;j++) {
    iVar = GetVarFromLit(*pLit);

    // note :: unlike in sparrow paper, negative score is "good"

    iScoreAdjust = GetScore(iVar);

    if (iScoreAdjust > 10) {
      iScoreAdjust = 10;
    }
    if (iScoreAdjust < 0) {
      iScoreAdjust = 0;
    }

    fScoreProb = aSparrowPreCalc[iScoreAdjust];
    fBaseAgeProb = (iStep - aVarLastChange[iVar]) * fInvSparrowC3;
    fAgeProb = 1.0;
    for (k=0; k < iSparrowC2; k++) {
      fAgeProb *= fBaseAgeProb;
    }
    fAgeProb += 1.0;

    aSparrowWeights[j] = fScoreProb * fAgeProb;
    fScoreSum += aSparrowWeights[j];

    pLit++;
  }

  fScorePos = RandomFloat()*fScoreSum;
  fScoreSum = 0.0;

  for (j=0;j<iClauseLen;j++) {
    fScoreSum += aSparrowWeights[j];
    if (fScorePos <= fScoreSum) {
      iFlipCandidate = GetVar(iClause,j);
      break;
    }
  }
}

/**
 * Decrease the clause weights of all SAT clauses by 1.
 */
void SmoothSparrow() {

  UINT32 j;
  UINT32 iVar;
  UINT32 iClause;
  UINT32 iLoopMax;

  /* Because iNumPenClauses can change, keep track of # Initial Clauses in list */
  iLoopMax = iNumPenClauses;

  /* Decrement the penalties of all currently SAT clauses. */
  for (j = 0; j < iLoopMax; j++) {

    iClause = aPenClauseList[j];

    if (aNumTrueLit[iClause] > 0) {
      aClausePenaltyINT[iClause]--;
      iTotalPenaltyINT--;

      /* If decrementing this clause's penalty causes the penalty to go down to 1,
       *  this clause is no longer considered to be penalized. */
      if (aClausePenaltyINT[iClause] == 1) {
        RemoveFromList1(iClause, aPenClauseList, aPenClauseListPos, &iNumPenClauses);
      }

      /* If this clause has only 1 true literal, decrementing this clause's score
       *  decreases the score of that literal's backing variable */
      if (aNumTrueLit[iClause] == 1) {
        iVar = aCritSat[iClause];
        UpdateScore(iVar, -1);

        /* Update decreasing promising variables. */
        // TODO: Move this out of here for increased modularity, so that this function does not depend on aDecPromVars.
        if ((!aIsDecPromVar[iVar]) && (aVarPenScore[iVar] < 0 ) && (aVarLastChange[iVar] < iStep - 1)) {
          AddToList2(iVar, aDecPromVarsList, aDecPromVarsListPos, &iNumDecPromVars, aIsDecPromVar);
        }
      }
    }
  }
}

/**
 * POST: Increments the weights of all UNSAT clauses, updating variable scores
 *  accordingly.
 * POST: aDecPromVars is updated accordingly.
 */
void ScaleSparrow() {
  UINT32 j;
  UINT32 k;
  UINT32 iVar;
  UINT32 iClause;
  LITTYPE *pLit;

  /* Since we are about to increment the penalties of all unsatisfied clauses,
   *  we can add that number to the sum of all clause penalties right away. */
  iTotalPenaltyINT += iNumFalse;

  /* Increment the penalty of all unsatisfied clauses. */
  for (j = 0; j < iNumFalse; j++) {

    iClause = aFalseList[j];
    aClausePenaltyINT[iClause]++;

    /* A clause penalty of 2 or greater earns this clause a spot on the list of penalized clauses. */
    if (aClausePenaltyINT[iClause] == 2) {
      AddToList1(iClause, aPenClauseList, aPenClauseListPos, &iNumPenClauses);
    }

    /* All variables in this clause have their scores decremented in accordance with the change in
     *  clause penalty, for flipping any of these variables would make the clause SAT */
    pLit = pClauseLits[iClause];
    for (k = 0; k < aClauseLen[iClause]; k++) {
      iVar = GetVarFromLit(*pLit);
      UpdateScore(iVar, -1);

      /* Update decreasing promising variables. */
      // TODO: Move this out of here for increased modularity, so that this function does not depend on aDecPromVars.
      if (bPromisingList && !aIsDecPromVar[iVar] && (aVarPenScore[iVar] < 0) && (aVarLastChange[iVar] < iStep - 1)) {
        AddToList2(iVar, aDecPromVarsList, aDecPromVarsListPos, &iNumDecPromVars, aIsDecPromVar);
      }
      pLit++;
    }
  }
}

void FlipSparrow() {

  UINT32 j;
  UINT32 k;
  UINT32 *pClause;
  UINT32 iVar;
  LITTYPE litWasTrue;
  LITTYPE litWasFalse;
  LITTYPE *pLit;

  SINT32 iPenalty;

  if (iFlipCandidate == 0) {
    return;
  }

  UpdateNeighborDecPromVarStatus();

  litWasTrue = GetTrueLit(iFlipCandidate);
  litWasFalse = GetFalseLit(iFlipCandidate);

  aVarValue[iFlipCandidate] = (UINT32) !aVarValue[iFlipCandidate];

  pClause = pLitClause[litWasTrue];
  for (j=0;j<aNumLitOcc[litWasTrue];j++) {
    iPenalty = aClausePenaltyINT[*pClause];
    aNumTrueLit[*pClause]--;
    if (aNumTrueLit[*pClause]==0) {

      aFalseList[iNumFalse] = *pClause;
      aFalseListPos[*pClause] = iNumFalse++;

      aVarPenScore[iFlipCandidate] -= iPenalty;

      pLit = pClauseLits[*pClause];
      for (k=0;k<aClauseLen[*pClause];k++) {
        iVar = GetVarFromLit(*pLit);
        aVarPenScore[iVar] -= iPenalty;

        pLit++;

      }
    }
    if (aNumTrueLit[*pClause]==1) {
      pLit = pClauseLits[*pClause];
      for (k=0;k<aClauseLen[*pClause];k++) {
        if (IsLitTrue(*pLit)) {
          iVar = GetVarFromLit(*pLit);
          aVarPenScore[iVar] += iPenalty;
          aCritSat[*pClause] = iVar;
          break;
        }
        pLit++;
      }
    }
    pClause++;
  }

  pClause = pLitClause[litWasFalse];
  for (j=0;j<aNumLitOcc[litWasFalse];j++) {
    iPenalty = aClausePenaltyINT[*pClause];
    aNumTrueLit[*pClause]++;
    if (aNumTrueLit[*pClause]==1) {

      aFalseList[aFalseListPos[*pClause]] = aFalseList[--iNumFalse];
      aFalseListPos[aFalseList[iNumFalse]] = aFalseListPos[*pClause];

      pLit = pClauseLits[*pClause];
      for (k=0;k<aClauseLen[*pClause];k++) {
        iVar = GetVarFromLit(*pLit);
        aVarPenScore[iVar] += iPenalty;

        pLit++;

      }
      aVarPenScore[iFlipCandidate] += iPenalty;
      aCritSat[*pClause] = iFlipCandidate;
    }
    if (aNumTrueLit[*pClause]==2) {
      aVarPenScore[aCritSat[*pClause]] -= iPenalty;
    }
    pClause++;
  }

  AddNeighboringDecPromVars();
}

void CreateSparrowWeights() {
  aSparrowWeights = (FLOAT *) AllocateRAM((iMaxClauseLen+1)*sizeof(FLOAT));
}

void CreateSparrowScore() {
  bPen = TRUE;
  aVarPenScore = (SINT32 *) AllocateRAM((iNumVars+1)*sizeof(UINT32));
  aCritSat = (UINT32 *) AllocateRAM(iNumClauses*sizeof(UINT32));
}

void InitSparrowScore() {

  UINT32 j;
  UINT32 k;
  UINT32 iVar;
  LITTYPE *pLit;

  for (j=1;j<=iNumVars;j++) {
    aVarPenScore[j] = 0;
  }

  for (j=0;j<iNumClauses;j++) {
    if (aNumTrueLit[j]==0) {
      for (k=0;k<aClauseLen[j];k++) {
        aVarPenScore[GetVar(j,k)] -= aClausePenaltyINT[j];
      }
    } else if (aNumTrueLit[j]==1) {
      pLit = pClauseLits[j];
      for (k=0;k<aClauseLen[j];k++) {
        if IsLitTrue(*pLit) {
          iVar = GetVarFromLit(*pLit);
          aVarPenScore[iVar] += aClausePenaltyINT[j];
          aCritSat[j] = iVar;
          break;
        }
        pLit++;
      }
    }
  }
}

#ifdef __cplusplus
}
#endif
