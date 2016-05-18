/*

      ##  ##  #####    #####   $$$$$   $$$$   $$$$$$    
      ##  ##  ##  ##  ##      $$      $$  $$    $$      
      ##  ##  #####   ##       $$$$   $$$$$$    $$      
      ##  ##  ##  ##  ##          $$  $$  $$    $$      
       ####   #####    #####  $$$$$   $$  $$    $$      
  ======================================================
  SLS SAT Solver from The University of British Columbia
  ======================================================
  ...Developed by Dave Tompkins (davet [@] cs.ubc.ca)...
  ------------------------------------------------------
  .......consult legal.txt for legal information........
  ......consult revisions.txt for revision history......
  ------------------------------------------------------
  ... project website: http://www.satlib.org/ubcsat ....
  ------------------------------------------------------
  .....e-mail ubcsat-help [@] cs.ubc.ca for support.....
  ------------------------------------------------------

*/

#include "paws.h"

UINT32 iPAWSMaxInc;
PROBABILITY iPAWSFlatMove;

UINT32 iPawsSmoothCounter;


/***** Trigger PenClauseList *****/

void CreatePenClauseList();
void InitPenClauseList();

UINT32 *aPenClauseList;
UINT32 *aPenClauseListPos;
UINT32 iNumPenClauses;

void CreatePenClauseList() {
  aPenClauseList = AllocateRAM(iNumClauses*sizeof(UINT32));
  aPenClauseListPos = AllocateRAM(iNumClauses*sizeof(UINT32));
}

void InitPenClauseList() {
  iNumPenClauses = 0;
  iPawsSmoothCounter = 0;
}



void AddPAWS() {

  ALGORITHM *pCurAlg;

  pCurAlg = CreateAlgorithm("paws","",FALSE,
    "PAWS: Pure Additive Weighting Scheme",
    "Thornton, Pham, Bain, Ferreira [AAAI 04]",
    "PickPAWS,PostFlipPAWS",
    "DefaultProcedures,Flip+MBPINT+FCL+VIF,PenClauseList,VarLastChange",
    "default","default");
  
  AddParmUInt(&pCurAlg->parmList,"-maxinc","frequency of penalty reductions [default %s]","reduce (smooth) all clause penalties by 1~after every INT increases","",&iPAWSMaxInc,10);
  AddParmProbability(&pCurAlg->parmList,"-pflat","flat move probabilty [default %s]","when a local minimum is encountered,~take a 'flat' (sideways) step with probability PR","",&iPAWSFlatMove,0.15);

  CreateTrigger("PickPAWS",ChooseCandidate,PickPAWS,"","");
  CreateTrigger("PostFlipPAWS",PostFlip,PostFlipPAWS,"","");

  CreateTrigger("CreatePenClauseList",CreateStateInfo,CreatePenClauseList,"","");
  CreateTrigger("InitPenClauseList",InitStateInfo,InitPenClauseList,"","");
  CreateContainerTrigger("PenClauseList","CreatePenClauseList,InitPenClauseList");

}

void PickPAWS() {
  
  UINT32 j;
  UINT32 iVar;
  SINT32 iScore;
  SINT32 iBestScore;
  UINT32 iLoopEnd;
  UINT32 iTabuCutoff;
  iNumCandidates = 0;
  iBestScore = 0x7FFFFFFF;
     if(bTabu)
  {
      if (iStep > iTabuTenure) {
       iTabuCutoff = iStep - iTabuTenure;
      if (iVarLastChangeReset > iTabuCutoff) {
       iTabuCutoff = iVarLastChangeReset;
     }
   } else {
    iTabuCutoff = 1;
   }
  }  
  /* look at all variables that appear in false clauses */
  if(bVarInFalse){
  if(!bTabu){
  for (j=0;j<iNumVarsInFalseList;j++) {
    iVar = aVarInFalseList[j];

    /* use cached value of breakcount - makecount */
    switch(iScoringMeasure){

    case 1:
    iScore = aBreakPenaltyINT[iVar] - aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    if (iScore <= iBestScore) {
      if (iScore < iBestScore) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];
      
      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }
     
     break;

     case 2:
        iScore =  -aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    
     if (iScore <= iBestScore) {
      if (iScore < iBestScore) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }




     break;
     case 3: 
     iScore = aBreakPenaltyINT[iVar] - aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    if (iScore <= iBestScore) {
      if ((iScore < iBestScore)||(aVarLastChange[iVar]<aVarLastChange[*aCandidateList])) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }

     break;

     
    }
  }
 }
 // If Tabu 
 else{
   for (j=0;j<iNumVarsInFalseList;j++) {
    iVar = aVarInFalseList[j];
    if (aVarLastChange[j] < iTabuCutoff) { 
    /* use cached value of breakcount - makecount */
    switch(iScoringMeasure){

    case 1:
    iScore = aBreakPenaltyINT[iVar] - aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    if (iScore <= iBestScore) {
      if (iScore < iBestScore) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }

     break;

     case 2:
        iScore =  -aMakePenaltyINT[iVar];

    /* build candidate list of best vars */


     if (iScore <= iBestScore) {
      if (iScore < iBestScore) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }




     break;
     case 3:
     iScore = aBreakPenaltyINT[iVar] - aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    if (iScore <= iBestScore) {
      if ((iScore < iBestScore)||(aVarLastChange[iVar]<aVarLastChange[*aCandidateList])) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }

     break;


    }
  }
}
}
 
}
  else{
  if(!bTabu){
  for (j=0;j<iNumVars;j++) {
    iVar = j;

    /* use cached value of breakcount - makecount */
    switch(iScoringMeasure){

    case 1:
    iScore = aBreakPenaltyINT[iVar] - aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    if (iScore <= iBestScore) {
      if (iScore < iBestScore) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }

     break;

     case 2:
        iScore =  -aMakePenaltyINT[iVar];

    /* build candidate list of best vars */


     if (iScore <= iBestScore) {
      if (iScore < iBestScore) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }




     break;
     case 3:
     iScore = aBreakPenaltyINT[iVar] - aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    if (iScore <= iBestScore) {
      if ((iScore < iBestScore)||(aVarLastChange[iVar]<aVarLastChange[*aCandidateList])) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }

     break;


    }
  }
 }
 // If Tabu
 else{
   for (j=0;j<iNumVars;j++) {
    iVar = j;
    if (aVarLastChange[j] < iTabuCutoff) {
    /* use cached value of breakcount - makecount */
    switch(iScoringMeasure){

    case 1:
    iScore = aBreakPenaltyINT[iVar] - aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    if (iScore <= iBestScore) {
      if (iScore < iBestScore) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }

     break;

     case 2:
        iScore =  -aMakePenaltyINT[iVar];

    /* build candidate list of best vars */


     if (iScore <= iBestScore) {
      if (iScore < iBestScore) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }




     break;
     case 3:
     iScore = aBreakPenaltyINT[iVar] - aMakePenaltyINT[iVar];

    /* build candidate list of best vars */

    if (iScore <= iBestScore) {
      if ((iScore < iBestScore)||(aVarLastChange[iVar]<aVarLastChange[*aCandidateList])) {
        iNumCandidates = 0;
        iBestScore = iScore;
      }

      /* using the "Monte Carlo" method */

      iLoopEnd = iNumCandidates + aMakeCount[iVar];

      if (iLoopEnd >= iMaxCandidates) {
        ReportPrint1(pRepErr,"Unexpected Error: increase iMaxCandidates [%u]\n",iMaxCandidates);
        AbnormalExit();
      }

      while (iNumCandidates < iLoopEnd) {
        aCandidateList[iNumCandidates++] = iVar;
      }
     }

     break;


    }
  }
}
}

}

  iFlipCandidate = 0;

  if (iBestScore < 0) {

    /* select flip candidate uniformly from candidate list */
    //iFlipCandidate = TieBreaking();
     
    if (iNumCandidates > 1) {
     // iFlipCandidate = aCandidateList[RandomInt(iNumCandidates)];
        iFlipCandidate = TieBreaking();
    } else {
      iFlipCandidate = *aCandidateList;
    }
    
  } else {
    
    if (iBestScore == 0) {

      /* with probability (iPAWSFlatMove) flip candidate from candidate list,
         otherwise it's a null flip */
    
      if (RandomProb(iPAWSFlatMove)) {
       // iFlipCandidate = aCandidateList[RandomInt(iNumCandidates)]; 
        
        if (iNumCandidates > 1) {
          //iFlipCandidate = aCandidateList[RandomInt(iNumCandidates)]; 
           iFlipCandidate = TieBreaking();
        } else {
          iFlipCandidate = *aCandidateList;
        }
       
      }
    }
  }
}

void SmoothPAWS() {
  
  UINT32 j;
  UINT32 k;
  UINT32 iClause;
  UINT32 iLoopMax;
  LITTYPE *pLit;

  /* Because iNumPenClauseList can change, keep track of # Initial Clauses in list */

  iLoopMax = iNumPenClauses;

  /* Each clause penalty is going down by one, so total is going down by # clauses */

  iTotalPenaltyINT -= iNumPenClauses;

  for (j=0;j<iLoopMax;j++) {
    
    iClause = aPenClauseList[j];

    /* decrease the clause penalty by one */

    aClausePenaltyINT[iClause]--;

    /* if clause penalty is equal to one, remove it from the list of penalized clauses. */

    /* Note that moving the 'last' penalty to the current location j
       doesn't prevent that other penalty from being adjusted as j loops
       all the way to iLoopMax */

    if (aClausePenaltyINT[iClause]==1) {
      RemoveFromList1(iClause, aPenClauseList, aPenClauseListPos, &iNumPenClauses);
    }

    /* For all false clauses, the 'make' score for each variable in the clause
       has to be reduced by one */

    if (aNumTrueLit[iClause]==0) { 
      pLit = pClauseLits[iClause];
      for (k=0;k<aClauseLen[iClause];k++) {
        aMakePenaltyINT[GetVarFromLit(*pLit)]--;
        pLit++;
      }
    }

    /* For critically satisfied clauses, the 'break' score for that critical variable
       has to be reduced by one */

    if (aNumTrueLit[iClause]==1) {
      // aCritSat[iClause] returns the critical variable for this clause
      aBreakPenaltyINT[aCritSat[iClause]]--;
    }
  }
}

void ScalePAWS() {
  UINT32 j;
  UINT32 k;
  UINT32 iClause;
  LITTYPE *pLit;

  /* for each false clause, increae the clause penalty by 1 */

  iTotalPenaltyINT += iNumFalse;

  for(j=0;j<iNumFalse;j++) {
    
    iClause = aFalseList[j];

    aClausePenaltyINT[iClause]++;

    if (aClausePenaltyINT[iClause]==2) {

      aPenClauseList[iNumPenClauses] = iClause;
      aPenClauseListPos[iClause] = iNumPenClauses++;

    }

    /* update cached values */

    pLit = pClauseLits[iClause];
    for (k=0;k<aClauseLen[iClause];k++) {
      aMakePenaltyINT[GetVarFromLit(*pLit)]++;
      pLit++;
    }
  }
}


void PostFlipPAWS() {

  if (iFlipCandidate)
    return;

  /* if a 'null flip' */

  /* Scale penalties */

  ScalePAWS();

  /* smooth every iPAWSMaxInc Null Flips */

  iPawsSmoothCounter++;
  if (iPawsSmoothCounter > iPAWSMaxInc) {
    SmoothPAWS();
    iPawsSmoothCounter = 0;
  }

}

