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

#include "ubcsat.h"

#include "satenstein-types.h"

void PickSatenstein();
void PickSatensteinW();
void NoveltyPromisingProm(BOOL trackLastChanged);
void NoveltyProm(BOOL trackLastChanged);
void CreateDecPromVarsW();
void InitDecPromVarsW();
void UpdateDecPromVarsW();
UINT32 TieBreaking();

void EnableDisableTrigger();
BOOL CheckIfFreebie(UINT32);

UINT32 iHeuristic;
UINT32 iScoringMeasure;
UINT32 iSmoothingScheme;
UINT32 iDecStrategy;
UINT32 iRandomStep;
UINT32 iTieBreaking;
UINT32 iBestPromVar;
UINT32 iSecondBestPromVar;
UINT32 iLeastRecentlyFlippedPromVar;

UINT32 iTabuCutoff;

void Smooth();
void UpdateClauseWeight();

UINT32 updateDecPromVarsNovelty(BOOL trackLastChanged);

void PerformHeuristic();

void PerformPickPromisingVar();

void PerformRandomWalk();

void PerformScoringMeasureAllVars();

void PerformScoringMeasureVarsInFalseClauses();

void PerformScoringMeasure();

void PerformSmoothing();

UINT32 iUpdateSchemePromList;
UINT32 iAdaptiveNoiseScheme;
BOOL *aIsDecPromVar;
BOOL bNoise;
BOOL bPromNoise;
BOOL bPromisingList;
BOOL bPerformNoveltyAlternate;
BOOL bSingleClause;
BOOL bPerformRandomWalk;
BOOL bTabu;
BOOL bVarInFalse;
PROBABILITY iRWp;
PROBABILITY iRDp;
PROBABILITY iRWpWalk;
PROBABILITY iRFp;

PROBABILITY iPromNovNoise;
BOOL bPerformNovelty;
UINT32 *aDecPromVarsListW;
UINT32 iNumDecPromVarsW;

PROBABILITY iPromDp;
PROBABILITY iPromWp;

void AddSatenstein() {

  ALGORITHM *pCurAlg;

  pCurAlg = CreateAlgorithm("satenstein","",FALSE,
    "Generalized local search algorithm",
    "Yet to be published",
    "PickSatenstein,InitRSAPS,PostFlipRSAPS,PostFlipSAPS,PostFlipPAWS",
    "DefaultProcedures,AdaptPromNoveltyNoise,Flip+TrackChanges+FCL,Flip+FalseClauseList,DecPromVars,FalseClauseList,VarLastChange,MakeBreak,AdaptNoveltyPlusNoise,VarLastSatisfied,FlipCounts,LookAhead,EnableDisableTrigger,VarInFalse,VarScore,VW2Weights,Flip+MBPFL+FCL+VIF,ClauseVarFlipCounts,AutoVW2Weights,Flip+TrackChanges+FCL,DecPromPenVars,ClausePenaltyINT,VarPenScore,WeightedList,VarsShareClauses,Flip+MBPINT+FCL+VIF,PenClauseList,SpecialUpdate,SpecialUpdateMakeBreak",
    "default","default");

  CopyParameters(pCurAlg, "novelty+", "", FALSE, 0);

  //CopyParameters(pCurAlg,"novelty+p","",FALSE,3);
  //CopyParameters(pCurAlg,"novelty++","",FALSE,6);
  //CopyParameters(pCurAlg,"novelty++p","",FALSE,9);
  //CopyParameters(pCurAlg,"novelty++'","",FALSE,12);
  CopyParameters(pCurAlg, "walksat-tabu", "", FALSE, 3);
  CopyParameters(pCurAlg, "vw2", "", FALSE, 4);


  CreateTrigger("EnableDisableTrigger", PostParameters, EnableDisableTrigger, "", "");
  AddParmProbability(&pCurAlg->parmList, "-dp", "diversification probability [default %s]",
                     "with probability dp, select the least recently flipped~variable from an unsat clause", "", &iDp,
                     0.05);
  AddParmProbability(&pCurAlg->parmList, "-promdp", "diversification probability [default %s]",
                     "with probability promdp, select the least recently flipped~variable from the promising list", "",
                     &iPromDp, 0.05);
  AddParmProbability(&pCurAlg->parmList, "-promwp", "diversification probability [default %s]",
                     "with probability promwp, select a random variable from the promising list", "", &iPromWp, 0.01);

  AddParmProbability(&pCurAlg->parmList, "-rwp", "random walk probability [default %s]",
                     "with probability rwp, randomly pick a variable from an unsat clause", "", &iRWp, 0.01);
  AddParmProbability(&pCurAlg->parmList, "-rwpwalk", "random walk probability [default %s]",
                     "with probability rwpwalk, randomly pick a variable from an unsat clause", "", &iRWpWalk, 0.50);
  AddParmProbability(&pCurAlg->parmList, "-rdp", "random walk probability [default %s]",
                     "with probability rdp, select the least recently flipped~variable from an unsat clause", "", &iRDp,
                     0.05);
  AddParmProbability(&pCurAlg->parmList, "-rfp", "random walk probability [default %s]",
                     "with probability Dp, select the least number of times flipped~variable from an unsat clause", "",
                     &iRFp, 0.05);
  AddParmBool(&pCurAlg->parmList, "-adaptive", "Adaptive strategy [default %s]",
              "if true tunes noise parameter adaptively", "", &bNoise, TRUE);
  AddParmBool(&pCurAlg->parmList, "-adaptiveprom", "Adaptive strategy [default %s]",
              "if true tunes noise parameter for promising list adaptively", "", &bPromNoise, FALSE);

  AddParmBool(&pCurAlg->parmList, "-promisinglist", "Promising list [default %s]",
              "if true performs module 2 of SATenstein-LS", "", &bPromisingList, TRUE);
  AddParmBool(&pCurAlg->parmList, "-singleclause", "Single Clause [default %s]",
              "if true performs module 3 of SATenstein-LS", "", &bSingleClause, TRUE);
  AddParmBool(&pCurAlg->parmList, "-performrandomwalk", "Perform random-walk [default %s]",
              "if true perform module 1 of SATenstein-LS", "", &bPerformRandomWalk, FALSE);
  AddParmBool(&pCurAlg->parmList, "-performalternatenovelty", "Perform Alternate Novelty [default %s]",
              "if true perform Novelty with additional flatmove", "", &bPerformNoveltyAlternate, FALSE);

  AddParmBool(&pCurAlg->parmList, "-tabusearch", "Perform random-walk [default %s]",
              "if true cheks tabu-status of a variable", "", &bTabu, FALSE);
  AddParmBool(&pCurAlg->parmList, "-clausepen", "Perform random-walk [default %s]",
              "if true assigns clause penalty to each of the clauses", "", &bPen, FALSE);
  AddParmBool(&pCurAlg->parmList, "-varinfalse", "Perform random-walk [default %s]",
              "if true looks only at variables found in false clause", "", &bVarInFalse, TRUE);

  AddParmUInt(&pCurAlg->parmList, "-heuristic", "Heuristic choice [default %s]",
              "selects among a list of available heuristic options", "", &iHeuristic, 1);
  AddParmUInt(&pCurAlg->parmList, "-randomwalk", "Random step [default %s]",
              "specifies option for module 1 in SATenstein-LS", "", &iRandomStep, 1);
  AddParmUInt(&pCurAlg->parmList, "-tiebreaking", "Tie Breaking [default %s]", "specifies how tiebreaking is performed",
              "", &iTieBreaking, 1);
  AddParmUInt(&pCurAlg->parmList, "-updateschemepromlist", "Updation scheme for promising list [default %s]",
              "specifies the way promising list is updated", "", &iUpdateSchemePromList, 1);
  AddParmUInt(&pCurAlg->parmList, "-adaptivenoisescheme", "Updation scheme for promising list [default %s]",
              "specifies the way promising list is updated", "", &iAdaptiveNoiseScheme, 1);

  AddParmUInt(&pCurAlg->parmList, "-scoringmeasure", "Scoring measure [default %s]",
              "selects among a list of available scoring measures", "", &iScoringMeasure, 1);
  AddParmUInt(&pCurAlg->parmList, "-smoothingscheme", "Smoothing scheme [default %s]",
              "selects among a list of smoothingscheme", "", &iSmoothingScheme, SMOOTH_SAPS);
  AddParmUInt(&pCurAlg->parmList, "-decreasingvariable", "Decreasing variable selection choice [default %s]",
              "selects among a list of available options for selecting a decreasing variable", "", &iDecStrategy, 2);
  AddParmUInt(&pCurAlg->parmList, "-phi", "Parameter for adaptive tuning [default %s]", "parameter for adaptive tuning",
              "", &iInvPhi, 5);
  AddParmUInt(&pCurAlg->parmList, "-theta", "Parameter for adaptive tuning [default %s]",
              "parameter for adaptive tuning", "", &iInvTheta, 6);
  AddParmUInt(&pCurAlg->parmList, "-promphi", "Parameter for adaptive tuning [default %s]",
              "parameter for adaptive tuning", "", &iPromInvPhi, 5);
  AddParmUInt(&pCurAlg->parmList, "-promtheta", "Parameter for adaptive tuning [default %s]",
              "parameter for adaptive tuning", "", &iPromInvTheta, 6);

  AddParmBool(&pCurAlg->parmList, "-adaptwalkprob", "Perform random-walk [default %s]",
              "if true picks up a clause randomly and performs random-walk", "", &bAdaptWalkProb, FALSE);
  AddParmBool(&pCurAlg->parmList, "-adaptpromwalkprob", "Perform random-walk [default %s]",
              "if true picks up a clause randomly and performs random-walk", "", &bAdaptPromWalkProb, FALSE);

  AddParmFloat(&pCurAlg->parmList, "-alpha", "scaling parameter alpha [default %s]",
               "when a local minimum is encountered,~multiply all unsatisfied cluase penalties by FL", "", &fAlpha,
               1.3f);
  AddParmFloat(&pCurAlg->parmList, "-rho", "smoothing parameter rho [default %s]",
               "when smoothing occurs, smooth penalties by a factor of FL", "", &fRho, 0.8f);
  AddParmProbability(&pCurAlg->parmList, "-ps", "smooth probabilty [default %s]",
                     "when a local minimum is encountered,~smooth penalties with probability PR", "", &iPs, 0.40);
  AddParmProbability(&pCurAlg->parmList, "-promnovnoise", "smooth probabilty [default %s]",
                     "when a local minimum is encountered,~smooth penalties with probability PR", "", &iPromNovNoise,
                     0.50);

  AddParmFloat(&pCurAlg->parmList, "-sapsthresh", "threshold for detecting local minima [default %s]",
               "the algorithm considers a local minima to occur when no~improvement greater than FL is possible~the default reflects the value used in SAPS 1.0",
               "", &fPenaltyImprove, -1.0e-01f);
  AddParmUInt(&pCurAlg->parmList, "-maxinc", "frequency of penalty reductions [default %s]",
              "reduce (smooth) all clause penalties by 1~after every INT increases", "", &iPAWSMaxInc, 10);
  AddParmProbability(&pCurAlg->parmList, "-pflat", "flat move probabilty [default %s]",
                     "when a local minimum is encountered,~take a 'flat' (sideways) step with probability PR", "",
                     &iPAWSFlatMove, 0.15);

  /******* Sparrow parameters *********/
  AddParmFloat(&pCurAlg->parmList,"-sparrowc1","sparrow score adjustment parameter [default %s]","adjusts the importance of the score","",&fSparrowC1,2.0);
  AddParmUInt(&pCurAlg->parmList,"-sparrowc2","sparrow age polynomial parameter [default %s]","adjusts the influence of the age","",&iSparrowC2,4);
  AddParmFloat(&pCurAlg->parmList,"-sparrowc3","sparrow age threshold parameter [default %s]","threshold for age calculation","",&fSparrowC3,100000.0);

  /******* DCCA parameters *********/
  AddParmUInt(&pCurAlg->parmList,"-avgweightthreshold",
              "average clause weight threshold [default %s]",
              "on the diversification step, if the average clause weight exceeds~this threshold, smoothing is performed",
              "",&iAvgClauseWeightThreshold,300);
  AddParmFloat(&pCurAlg->parmList,"-DCCAp","DCCA p param [default %s]","weight of current clause score in SWT smoothing [default %s]","",&fDCCAp,0.3);
  AddParmFloat(&pCurAlg->parmList,"-DCCAq","DCCA q param [default %s]","weight of average clause score in SWT smoothing [default %s]","",&fDCCAq,0.0);


  CreateTrigger("PickSatenstein", ChooseCandidate, PickSatenstein, "", "");


  pCurAlg = CreateAlgorithm("satenstein", "", TRUE,
                            "Generalized local search algorithm",
                            "Yet to be published",
                            "PickSatensteinW",
                            "DefaultProceduresW,Flip+TrackChanges+FCL+W,DecPromVarsW,FalseClauseList,VarLastChange,MakeBreakW",
                            "default_w", "default");

  CopyParameters(pCurAlg, "satenstein", "", FALSE, 0);
  CreateTrigger("PickSatensteinW", ChooseCandidate, PickSatensteinW, "", "");

  CreateTrigger("CreateDecPromVarsW", CreateStateInfo, CreateDecPromVarsW, "CreateTrackChangesW", "");
  CreateTrigger("InitDecPromVarsW", InitStateInfo, InitDecPromVarsW, "InitTrackChangesW", "");
  CreateTrigger("UpdateDecPromVarsW", UpdateStateInfo, UpdateDecPromVarsW, "UpdateTrackChangesW", "");
  CreateContainerTrigger("DecPromVarsW",
                         "CreateDecPromVarsW,InitDecPromVarsW,UpdateDecPromVarsW,MakeBreakW,AdaptNoveltyPlusNoiseW,FlipCounts,LookAhead,EnableDisableTrigger,VW2Weights,VarInFalse,VarScoreW,VarLastChange");

}

/* This part of code manages all conflicting triggers of SATenstein-LS.
   Once the parameter configuration for a given instantiation of SATenstein-LS
   is obtained. This part of the code figures out triggers that need to 
   be disabled and enabled.
*/

void EnableDisableTrigger() {

  BOOL bNotThreeSat;
  int j;
  if (bNoise) {
    ActivateTriggers("AdaptNoveltyPlusNoise,InitAdaptNoveltyPlusNoise");
  }
  else {
    DeActivateTriggers("AdaptNoveltyPlusNoise,InitAdaptNoveltyPlusNoise");
  }

  if (bPromNoise) {
    ActivateTriggers("AdaptPromNoveltyNoise,InitAdaptPromNoveltyNoise");
  }
  else {
    DeActivateTriggers("AdaptPromNoveltyNoise,InitAdaptPromNoveltyNoise");
  }

//Trigger VW2Weight is required only iHeuristic is 8 


  if ((((iHeuristic == 8) || (iHeuristic == 16)) && ((bPromisingList) || (bSingleClause))) ||
      (bPromisingList && iDecStrategy == 4) || (bPerformRandomWalk && iRandomStep == 5) ||
      ((iTieBreaking == 4) && (bPromisingList && ((iDecStrategy == 1) || (iDecStrategy == 4))))) {
    if (bNoise) {
      ActivateTriggers("AutoVW2Weights");
      DeActivateTriggers("VW2Weights");
    }
    else {
      ActivateTriggers("VW2Weights");
      DeActivateTriggers("AutoVW2Weights");
    }
  } else {
    DeActivateTriggers("VW2Weights,AutoVW2Weights");
  }

  //VarlastSatisfied is only used if iHeuristic is 17 or 18
  if ((iHeuristic == 17) || (iHeuristic == 18)) {
    //printf("I am activating");
    ActivateTriggers("VarLastSatisfied");
  } else {
    DeActivateTriggers("VarLastSatisfied");
  }

  //Lookahead is never required if promising variant is not used

  if ((iHeuristic > 9) && ((bPromisingList) || (bSingleClause))) {
    ActivateTriggers("LookAhead");
  } else {
    DeActivateTriggers("LookAhead");
  }


  if (iHeuristic == H_PICK_DCCA_DIVERSIFY || iHeuristic == H_PICK_SPARROWPROBDIST) {
    bPen = TRUE;
  }

  //DecPromVar is used if promising list
  if ((bPromisingList) || ((bSingleClause) && (iHeuristic > 9))) {
    if (bPen) {
      ActivateTriggers("DecPromPenVars");
      DeActivateTriggers("DecPromVars");
    }
    else {
      ActivateTriggers("DecPromVars");
      DeActivateTriggers("DecPromPenVars");
    }
  } else {
    DeActivateTriggers("DecPromVars,DecPromPenVars");
  }


  if ((bPromisingList) || ((bSingleClause) && (iHeuristic > 9))) {
    if (!bPen) {
      ActivateTriggers("VarScore");
      DeActivateTriggers("VarPenScore");
    }
    else {
      ActivateTriggers("VarPenScore");
      DeActivateTriggers("VarScore");
    }
  } else {
    DeActivateTriggers("VarScore,VarPenScore");
  }

  /**
   * Choose a flipping scheme
   */
  if (((bPerformRandomWalk) && (iRandomStep == 2)) || (bPromisingList) || ((bSingleClause) && (iHeuristic > 9))) {
    ActivateTriggers("Flip+TrackChanges+FCL");
    DeActivateTriggers("Flip+FalseClauseList");
  }
  else {
    DeActivateTriggers("Flip+TrackChanges+FCL");
    ActivateTriggers("Flip+FalseClauseList");
  }

  /*SAPS and PAWS need some disjoint set of triggers that other algorithms don't need
  so they are treated separately. */
  if (bPen && (!bPromisingList) && (!bSingleClause)) {
    DeActivateTriggers("SpecialUpdate,SpecialUpdateMakeBreak");
    if (iSmoothingScheme == SMOOTH_PAWS) {

      ActivateTriggers("VarInFalse,MakeBreak,Flip+TrackChanges+FCL,PostFlipPAWS");
      DeActivateTriggers("Flip+FalseClauseList,Flip+MBPFL+FCL+VIF,PostFlipSAPS,PostFlipRSAPS");
    }

    if (iSmoothingScheme == SMOOTH_SAPS) {
      ActivateTriggers("VarInFalse,MakeBreak,Flip+TrackChanges+FCL");
      DeActivateTriggers("Flip+FalseClauseList,,Flip+MBPINT+FCL+VIF,PostFlipPAWS");
      if (bNoise) {
        DeActivateTriggers("PostFlipSAPS,AdaptNoveltyPlusNoise,InitAdaptNoveltyPlusNoise");
        ActivateTriggers("InitRSAPS,PostFlipRSAPS");
      }
      else
        DeActivateTriggers("PostFlipRSAPS,InitRSAPS");
    }
  }
  else {

    DeActivateTriggers("Flip+MBPINT+FCL+VIF,PostFlipPAWS,Flip+MBPFL+FCL+VIF,PostFlipSAPS,PostFlipRSAPS");
    if (iDecStrategy != PICK_GNOVELTYPLUS && iHeuristic != H_PICK_SPARROWPROBDIST && iHeuristic != H_PICK_DCCA_DIVERSIFY) {
      DeActivateTriggers("PenClauseList");
    }

    if ((!bSingleClause && !bPen && !bPromisingList) || ((bPerformRandomWalk) && (iRandomStep == 2))) {
      if (((bPerformRandomWalk) && (iRandomStep == 2)) || (bVarInFalse)) {
        DeActivateTriggers("SpecialUpdateMakeBreak");
      } else {
        DeActivateTriggers("SpecialUpdate");
      }
    }
    else {
      DeActivateTriggers("SpecialUpdate,SpecialUpdateMakeBreak");
    }

  }


  if ((bPerformRandomWalk && iRandomStep == 4) || (iTieBreaking == 3) || ((bPromisingList) && (iDecStrategy == 5)) ||
      (((iHeuristic == 7) || (iHeuristic >= 14)) && ((bPromisingList) || (bSingleClause))) ||
      ((!bSingleClause) && (iScoringMeasure == 3))) {
    ActivateTriggers("FlipCounts");
  } else {
    DeActivateTriggers("FlipCounts");
  }

  if (bPen && (bPromisingList || bSingleClause) && (iSmoothingScheme == SMOOTH_PAWS)) {
    bNotThreeSat = FALSE;
    for (j = 0; j < iNumClauses; j++) {
      if (aClauseLen[j] != 3) {
        bNotThreeSat = TRUE;
      }
    }

    if (bNotThreeSat == TRUE) {
      iPs = -1;
    }
    else {
      iPs = 1717986918;
    }

  }

/*If Promising list is not true
  and heuristic is 10,11,12,13 and 15
  then updatescheme is hard coded back to 14 */

  if (!bPromisingList &&
      ((iHeuristic == 10) || (iHeuristic == 11) || (iHeuristic == 12) || (iHeuristic == 13) || (iHeuristic == 15))) {
    iUpdateSchemePromList = 2;
  }


  if ((iSelectClause == 3) || (iSelectClause == 4) || (iSelectClause == 5) || (iSelectClause == 6)) {
    ActivateTriggers("ClauseVarFlipCounts");
  }
  else {
    // TODO: This is a temporary measure, because ClauseVarFlipCounts are apparently a key part of our Flipping scheme
//    DeActivateTriggers("ClauseVarFlipCounts");
  }

  /**
   * If we are performing configuration checking as a strategy for selecting promising variables, of which DCCA
   *  is the only currently implemented version as of June 2015, then we must hard code the update scheme for
   *  promising lists based on configuration.
   */
  if (bPromisingList && iDecStrategy == PICK_DCCA) {
    bPerformClauseConfChecking = TRUE;
    bPerformNeighborConfChecking = TRUE;
  }
  else { // TODO: Make this more flexible so that clause and neighbor configuration checking can be run independently
    bPerformClauseConfChecking = FALSE;
    bPerformNeighborConfChecking = FALSE;
  }

  if (iHeuristic == H_PICK_SPARROWPROBDIST) {
    ActivateTriggers("InitSparrow,CreateSparrowWeights,PenClauseList,TrackPenChanges");
    DeActivateTriggers("UpdateDecPromVars");
  }

  if (iDecStrategy == PICK_DCCA) {
    ActivateTriggers("ConfChecking");
    DeActivateTriggers("UpdateDecPromVars");
  }

  if (iHeuristic == H_PICK_DCCA_DIVERSIFY) {
    ActivateTriggers("PenClauseList,TrackPenChanges");
    DeActivateTriggers("UpdateDecPromVars");
  }


  if (bPromisingList && iDecStrategy == PICK_GNOVELTYPLUS) {
    iUpdateSchemePromList = UPDATE_GNOVELTYPLUS;
    ActivateTriggers("UpdateVarLastChange");
    DeActivateTriggers("UpdateDecPromVars");
  }

}


void PickSatenstein() {

  iTabuCutoff = 0;

  if (bTabu) {
    if (iStep > iTabuTenure) {
      iTabuCutoff = iStep - iTabuTenure;
      if (iVarLastChangeReset > iTabuCutoff) {
        iTabuCutoff = iVarLastChangeReset;
      }
    } else {
      iTabuCutoff = 1;
    }
  }

  if (bPerformRandomWalk) {
    PerformRandomWalk();
  }

  if (iFlipCandidate != 0) {
    return;
  }

  if (bPromisingList) {

    if (iNumDecPromVars > 0) {
      PerformPickPromisingVar();
    }

    if (iFlipCandidate != 0) {
      return;
    }

    if (!(iHeuristic == H_PICK_SPARROWPROBDIST || iHeuristic == H_PICK_DCCA_DIVERSIFY)) {
      if (bPen && iPs != -1) {
        UpdateClauseWeight();
        if (RandomProb(iPs)) {
          Smooth();
        }
      }
    }

    PerformHeuristic();
  }

  else if (bSingleClause) {
    if (bPen && iPs != -1) {
        UpdateClauseWeight();
        if (RandomProb(iPs))
          Smooth();
      }

    PerformHeuristic();
  }

  else {

    if (bPen) {
      PerformSmoothing();
    }
    // TODO: This last part of the function should probably not be in an else block. Verify with Ashique.
    else {
      // This part of code is never executed in our implementation for the paper
      // I will make a cleaner version by removing it.
      PerformScoringMeasure();
    }

  }
}

void PerformSmoothing() {
  switch (iSmoothingScheme) {

    case SMOOTH_SAPS:
      PickSAPS();
      break;

    case SMOOTH_PAWS:
      PickPAWS();
      break;

    default:
      break;
  }
}

void PerformScoringMeasure() {
  // TODO: Make these the same function with different arguments to reduce code duplication
  if (!bVarInFalse) {
    PerformScoringMeasureAllVars();
  }
  else {
    PerformScoringMeasureVarsInFalseClauses();
  }
}

void PerformScoringMeasureVarsInFalseClauses() {
  UINT32 j;
  UINT32 iVar;
  SINT32 iScore = 0;
  SINT32 iBestScore;

  switch (iScoringMeasure) {
            case 1:
              iNumCandidates = 0;
              iBestScore = iNumClauses;
              /* check score of all variables */

              for (j = 0; j < iNumVarsInFalseList; j++) {
                iVar = aVarInFalseList[j];

                /* use cached value of score */

                if (!bTabu) {
                  iScore = aBreakCount[iVar] - aMakeCount[iVar];

                  /* build candidate list of best vars */

                  if (iScore <= iBestScore) {
                    if (iScore < iBestScore) {
                      iNumCandidates = 0;
                      iBestScore = iScore;
                    }
                    aCandidateList[iNumCandidates++] = iVar;
                  }
                }

                else {
                  if (aVarLastChange[j] < iTabuCutoff) {
                    iScore = aBreakCount[iVar] - aMakeCount[iVar];

                    /* build candidate list of best vars */
                    if (iScore <= iBestScore) {
                      if (iScore < iBestScore) {
                        iNumCandidates = 0;
                        iBestScore = iScore;
                      }
                      aCandidateList[iNumCandidates++] = iVar;
                    }
                  }
                }
              }
              /* select flip candidate uniformly from candidate list */
              if (iNumCandidates > 1)
                iFlipCandidate = TieBreaking();
              else
                iFlipCandidate = aCandidateList[0];
              break;

            case 2:
              iNumCandidates = 0;
              iBestScore = iNumClauses;

              /* check score of all variables */
              for (j = 0; j < iNumVarsInFalseList; j++) {
                iVar = aVarInFalseList[j];
                /* use cached value of score */

                if (!bTabu) {
                  iScore = -aMakeCount[iVar];
                  /* build candidate list of best vars */
                  if (iScore <= iBestScore) {
                    if (iScore < iBestScore) {
                      iNumCandidates = 0;
                      iBestScore = iScore;
                    }
                    aCandidateList[iNumCandidates++] = iVar;
                  }
                }
                else {
                  if (aVarLastChange[j] < iTabuCutoff) {
                    iScore = -aMakeCount[iVar];

                    /* build candidate list of best vars */
                    if (iScore <= iBestScore) {
                      if (iScore < iBestScore) {
                        iNumCandidates = 0;
                        iBestScore = iScore;
                      }
                      aCandidateList[iNumCandidates++] = iVar;
                    }
                  }
                }
              }

              /* select flip candidate uniformly from candidate list */
              if (iNumCandidates > 1)
                iFlipCandidate = TieBreaking();
              else
                iFlipCandidate = aCandidateList[0];
              break;

            case 3:
              iNumCandidates = 0;
              iBestScore = iNumClauses;
              /* check score of all variables */

              for (j = 0; j < iNumVarsInFalseList; j++) {
                iVar = aVarInFalseList[j];
                /* use cached value of score */

                if (!bTabu) {
                  iScore = aBreakCount[iVar] - aMakeCount[iVar];

                  /* build candidate list of best vars */

                  if (iScore <= iBestScore) {
                    if ((iScore < iBestScore) || (aVarLastChange[iVar] < aVarLastChange[*aCandidateList])) {
                      iNumCandidates = 0;
                      iBestScore = iScore;
                    }
                    aCandidateList[iNumCandidates++] = iVar;
                  }
                }

                else {
                  if (aVarLastChange[j] < iTabuCutoff) {
                    iScore = aBreakCount[iVar] - aMakeCount[iVar];

                    /* build candidate list of best vars */
                    if (iScore <= iBestScore) {
                      if ((iScore < iBestScore) || (aVarLastChange[j] < aVarLastChange[*aCandidateList])) {
                        iNumCandidates = 0;
                        iBestScore = iScore;
                      }
                      aCandidateList[iNumCandidates++] = iVar;
                    }
                  }
                }
              }
              /* select flip candidate uniformly from candidate list */
              if (iNumCandidates > 1)
                iFlipCandidate = TieBreaking();
              else
                iFlipCandidate = aCandidateList[0];
              break;

    default:
      break;
  }
}

void PerformScoringMeasureAllVars() {
  UINT32 j;
  SINT32 iScore = 0;
  SINT32 iBestScore;

  switch (iScoringMeasure) {
            case 1:
              iNumCandidates = 0;
              iBestScore = iNumClauses;
              /* check score of all variables */

              for (j = 1; j <= iNumVars; j++) {

                /* use cached value of score */

                if (!bTabu) {
                  iScore = aBreakCount[j] - aMakeCount[j];

                  /* build candidate list of best vars */

                  if (iScore <= iBestScore) {
                    if (iScore < iBestScore) {
                      iNumCandidates = 0;
                      iBestScore = iScore;
                    }
                    aCandidateList[iNumCandidates++] = j;
                  }
                }

                else {
                  if (aVarLastChange[j] < iTabuCutoff) {
                    iScore = aBreakCount[j] - aMakeCount[j];

                    /* build candidate list of best vars */
                    if (iScore <= iBestScore) {
                      if (iScore < iBestScore) {
                        iNumCandidates = 0;
                        iBestScore = iScore;
                      }
                      aCandidateList[iNumCandidates++] = j;
                    }
                  }
                }
              }
              /* select flip candidate uniformly from candidate list */
              if (iNumCandidates > 1)
                iFlipCandidate = TieBreaking();
              else
                iFlipCandidate = aCandidateList[0];
              break;

            case 2:
              iNumCandidates = 0;
              iBestScore = iNumClauses;

              /* check score of all variables */
              for (j = 1; j <= iNumVars; j++) {
                /* use cached value of score */

                if (!bTabu) {
                  iScore = -aMakeCount[j];
                  /* build candidate list of best vars */
                  if (iScore <= iBestScore) {
                    if (iScore < iBestScore) {
                      iNumCandidates = 0;
                      iBestScore = iScore;
                    }
                    aCandidateList[iNumCandidates++] = j;
                  }
                }
                else {
                  if (aVarLastChange[j] < iTabuCutoff) {
                    iScore = -aMakeCount[j];

                    /* build candidate list of best vars */
                    if (iScore <= iBestScore) {
                      if (iScore < iBestScore) {
                        iNumCandidates = 0;
                        iBestScore = iScore;
                      }
                      aCandidateList[iNumCandidates++] = j;
                    }
                  }
                }
              }

              /* select flip candidate uniformly from candidate list */
              if (iNumCandidates > 1)
                iFlipCandidate = TieBreaking();
              else
                iFlipCandidate = aCandidateList[0];
              break;

            case 3:
              iNumCandidates = 0;
              iBestScore = iNumClauses;
              /* check score of all variables */

              for (j = 1; j <= iNumVars; j++) {

                /* use cached value of score */

                if (!bTabu) {
                  iScore = aBreakCount[j] - aMakeCount[j];

                  /* build candidate list of best vars */

                  if (iScore <= iBestScore) {
                    if ((iScore < iBestScore) || (aVarLastChange[j] < aVarLastChange[*aCandidateList])) {
                      iNumCandidates = 0;
                      iBestScore = iScore;
                    }
                    aCandidateList[iNumCandidates++] = j;
                  }
                }

                else {
                  if (aVarLastChange[j] < iTabuCutoff) {
                    iScore = aBreakCount[j] - aMakeCount[j];

                    /* build candidate list of best vars */
                    if ((iScore < iBestScore) || (aVarLastChange[j] < aVarLastChange[*aCandidateList])) {
                      if (iScore < iBestScore) {
                        iNumCandidates = 0;
                        iBestScore = iScore;
                      }
                      aCandidateList[iNumCandidates++] = j;
                    }
                  }
                }
              }
              /* select flip candidate uniformly from candidate list */
              if (iNumCandidates > 1)
                iFlipCandidate = TieBreaking();
              else
                iFlipCandidate = aCandidateList[0];
              break;


    default:
      break;
  }
}

void PerformRandomWalk() {
  UINT32 j;
  UINT32 iVar;
  UINT32 iClause;
  UINT32 iClauseLen;
  LITTYPE litPick;
  LITTYPE *pLit;

  switch (iRandomStep) {

      case 1:
        if (RandomProb(iRWp)) {
          if (iNumFalse) {
            iClause = SelectClause();
            iClauseLen = aClauseLen[iClause];
            litPick = (pClauseLits[iClause][RandomInt(iClauseLen)]);
            iFlipCandidate = GetVarFromLit(litPick);
          } else {
            iFlipCandidate = 0;
          }
        }

        break;

      case 2:
        if (RandomProb(iRWpWalk)) {
          if (iNumVarsInFalseList) {
            iFlipCandidate = aVarInFalseList[RandomInt(iNumVarsInFalseList)];
          } else {
            iFlipCandidate = 0;
          }


        }
        break;

      case 3:
        if (RandomProb(iRDp)) {
          if (iNumFalse) {

            iClause = SelectClause();
            iClauseLen = aClauseLen[iClause];

            pLit = pClauseLits[iClause];

            iFlipCandidate = GetVarFromLit(*pLit);

            pLit++;

            for (j = 1; j < iClauseLen; j++) {
              iVar = GetVarFromLit(*pLit);

              if (aVarLastChange[iVar] < aVarLastChange[iFlipCandidate]) {
                iFlipCandidate = iVar;
              }
              pLit++;
            }
          } else {
            iFlipCandidate = 0;
          }
        }

        break;

      case 4:
        if (RandomProb(iRFp)) {
          if (iNumFalse) {
            iClause = SelectClause();
            iClauseLen = aClauseLen[iClause];

            pLit = pClauseLits[iClause];

            iFlipCandidate = GetVarFromLit(*pLit);

            pLit++;

            for (j = 1; j < iClauseLen; j++) {
              iVar = GetVarFromLit(*pLit);

              if (aFlipCounts[iVar] < aFlipCounts[iFlipCandidate]) {
                iFlipCandidate = iVar;
              }

              pLit++;
            }
          } else {
            iFlipCandidate = 0;
          }
        }
        break;

      case 5:
        if (RandomProb(iRFp)) {
          if (iNumFalse) {
            iClause = SelectClause();
            iClauseLen = aClauseLen[iClause];

            pLit = pClauseLits[iClause];

            iFlipCandidate = GetVarFromLit(*pLit);

            pLit++;

            for (j = 1; j < iClauseLen; j++) {
              iVar = GetVarFromLit(*pLit);

              if (aVW2Weights[iVar] < aVW2Weights[iFlipCandidate]) {
                iFlipCandidate = iVar;
              }

              pLit++;
            }
          } else {
            iFlipCandidate = 0;
          }
        }
        break;

    default:
      break;
  }
}

void PerformPickPromisingVar() {

  UINT32 promVarIndex, i;
  UINT32 iVar;
  UINT32 iBestVarFlipCount;
  FLOAT fBestVWWeight;
  SINT32 iScore = 0;
  SINT32 iBestScore;
  BOOL FreebieExist;
  UINT32 iLastChange;

  switch (iDecStrategy) {

    case PICK_FREEBIE:/* Checks for freebie (breakcount 0) variables in the
                    promising decreasing variable stack. If there are
                    freebies then selects among them according to the
                    tie-breaking scheme. If there is not any free-bie
                    randomly picks one variable from the promising
                    decreasing variable list. */

      iNumCandidates = 0;
      FreebieExist = FALSE;

      i = -1;
      for (promVarIndex = 0; promVarIndex < iNumDecPromVars; promVarIndex++) {
        iVar = aDecPromVarsList[promVarIndex];
        iScore = bPen ? aVarPenScore[iVar] : aVarScore[iVar];
        if (iScore < 0) {
          if (CheckIfFreebie(iVar)) {
            if (iNumCandidates == 0) {
              FreebieExist = TRUE;
            }
            aCandidateList[iNumCandidates++] = iVar;
          }
        } else {
          i = promVarIndex;
          aIsDecPromVar[iVar] = FALSE;
          break;
        }
      }

      if (i != -1) {
        for (promVarIndex = i + 1; promVarIndex < iNumDecPromVars; promVarIndex++) {
          iVar = aDecPromVarsList[promVarIndex];
          iScore = aVarScore[iVar];
          if (iScore < 0) {
            aDecPromVarsListPos[iVar] = i;
            aDecPromVarsList[i++] = iVar;
            if (CheckIfFreebie(iVar)) {
              if (iNumCandidates == 0) {
                FreebieExist = TRUE;
              }
              aCandidateList[iNumCandidates++] = iVar;
            }
          } else {
            aIsDecPromVar[iVar] = FALSE;
          }
        }
        iNumDecPromVars = i;
      }

      if (FreebieExist == TRUE) {
        if (iNumCandidates > 1)
          iFlipCandidate = TieBreaking();
        else
          iFlipCandidate = aCandidateList[0];
      } else {
        if (iNumDecPromVars > 0) {
          if (iNumDecPromVars == 1)
            iFlipCandidate = aDecPromVarsList[0];
          else
            iFlipCandidate = aDecPromVarsList[RandomInt(iNumDecPromVars)];
        }
      }
      break;

    case PICK_BESTSCORE: /* Flip the variable with highest score
                    breaking ties in favor of the least
                    recently flipped variable. This strategy
                    is taken by G2WSAT, GNovelty+, adaptG2WSAT
                    preliminary version. */

      iBestScore = iNumFalse;  //CWBIG;    //BIG;
      iLastChange = iStep;
      i = -1;
      for (promVarIndex = 0; promVarIndex < iNumDecPromVars; promVarIndex++) {
        iVar = aDecPromVarsList[promVarIndex];
        iScore = GetScore(iVar);
        if (iScore < 0) {
          if (iScore < iBestScore) {
            iBestScore = iScore;
            iLastChange = aVarLastChange[iVar];
            iFlipCandidate = iVar;
          } else if (iScore == iBestScore) {
            if (aVarLastChange[iVar] < iLastChange) {
              iLastChange = aVarLastChange[iVar];
              iFlipCandidate = iVar;
            }
          }
        } else {
          i = promVarIndex;
          aIsDecPromVar[iVar] = FALSE;
          break;
        }
      }

      if (i != -1) {
        for (promVarIndex = i + 1; promVarIndex < iNumDecPromVars; promVarIndex++) {
          iVar = aDecPromVarsList[promVarIndex];
          if (!bPen)
            iScore = aVarScore[iVar];
          else
            iScore = aVarPenScore[iVar];
          if (iScore < 0) {
            aDecPromVarsListPos[iVar] = i;
            aDecPromVarsList[i++] = iVar;
            if (iScore < iBestScore) {
              iBestScore = iScore;
              iLastChange = aVarLastChange[iVar];
              iFlipCandidate = iVar;
            } else if (iScore == iBestScore) {
              if (aVarLastChange[iVar] < iLastChange) {
                iLastChange = aVarLastChange[iVar];
                iFlipCandidate = iVar;
              }
            }
          } else {
            aIsDecPromVar[iVar] = FALSE;
          }
        }
        iNumDecPromVars = i;
      }
      break;

    case PICK_OLDEST: /* Selects the least recently flipped variable
                    from the promising decreasing variable list.
                    This strategy is present in adaptG2WSAT0,
                    adaptG2WSAT+ and adaptG2WSAT+p. */
      iLastChange = iStep;
      i = -1;
      for (promVarIndex = 0; promVarIndex < iNumDecPromVars; promVarIndex++) {
        iVar = aDecPromVarsList[promVarIndex];
        iScore = bPen ? aVarPenScore[iVar] : aVarScore[iVar];
        if (iScore < 0) {
          if (aVarLastChange[iVar] < iLastChange) {
            iLastChange = aVarLastChange[iVar];
            iFlipCandidate = iVar;
          }
        }
        else {
          i = promVarIndex;
          aIsDecPromVar[iVar] = FALSE;
          break;
        }
      }

      if (i != -1) {
        for (promVarIndex = i + 1; promVarIndex < iNumDecPromVars; promVarIndex++) {
          iVar = aDecPromVarsList[promVarIndex];
          iScore = aVarScore[iVar];
          if (iScore < 0) {
            aDecPromVarsListPos[iVar] = i;
            aDecPromVarsList[i++] = iVar;
            if (aVarLastChange[iVar] < iLastChange) {
              iLastChange = aVarLastChange[iVar];
              iFlipCandidate = iVar;
            }
          }
          else {
            aIsDecPromVar[iVar] = FALSE;
          }
        }
        iNumDecPromVars = i;
      }

      break;

    case PICK_BEST_VW1:  /*Pick the variable that has been flipped least number of times
                    Any remaining ties are broken according to the tie-breaking
                    policy. */
      iNumCandidates = 0;
      fBestVWWeight = (FLOAT) iStep;
      i = -1;

      for (promVarIndex = 0; promVarIndex < iNumDecPromVars; promVarIndex++) {
        iVar = aDecPromVarsList[promVarIndex];
        iScore = bPen ? aVarPenScore[iVar] : aVarScore[iVar];
        if (iScore < 0) {
          if (aVW2Weights[iVar] <= fBestVWWeight) {
            if (aVW2Weights[iVar] < fBestVWWeight) {
              iNumCandidates = 0;
              fBestVWWeight = aVW2Weights[iVar];
            }
            aCandidateList[iNumCandidates++] = iVar;
          }
        }
        else {
          i = promVarIndex;
          aIsDecPromVar[iVar] = FALSE;
          break;
        }
      }

      if (i != -1) {
        for (promVarIndex = i + 1; promVarIndex < iNumDecPromVars; promVarIndex++) {
          iVar = aDecPromVarsList[promVarIndex];
          iScore = aVarScore[iVar];
          if (iScore < 0) {
            aDecPromVarsListPos[iVar] = i;
            aDecPromVarsList[i++] = iVar;
            if (aVW2Weights[iVar] <= fBestVWWeight) {
              if (aVW2Weights[iVar] < fBestVWWeight) {
                iNumCandidates = 0;
                fBestVWWeight = aVW2Weights[iVar];
              }
              aCandidateList[iNumCandidates++] = iVar;
            }

          }
          else {
            aIsDecPromVar[iVar] = FALSE;
          }
        }
        iNumDecPromVars = i;
      }


      if (iNumCandidates > 1)
        iFlipCandidate = TieBreaking();
      else
        iFlipCandidate = aCandidateList[0];

      break;

    case PICK_BEST_VW2:  /*Pick the variable that has been flipped least number of times
                    Any remaining ties are broken according to the tie-breaking
                    policy. */
      iNumCandidates = 0;
      iBestVarFlipCount = iStep;
      i = -1;

      for (promVarIndex = 0; promVarIndex < iNumDecPromVars; promVarIndex++) {
        iVar = aDecPromVarsList[promVarIndex];
        iScore = bPen ? aVarPenScore[iVar] : aVarScore[iVar];
        if (iScore < 0) {
          if (aFlipCounts[iVar] <= iBestVarFlipCount) {
            if (aFlipCounts[iVar] < iBestVarFlipCount) {
              iNumCandidates = 0;
              iBestVarFlipCount = aFlipCounts[iVar];
            }
            aCandidateList[iNumCandidates++] = iVar;
          }
        }
        else {
          i = promVarIndex;
          aIsDecPromVar[iVar] = FALSE;
          break;
        }
      }

      if (i != -1) {
        for (promVarIndex = i + 1; promVarIndex < iNumDecPromVars; promVarIndex++) {
          iVar = aDecPromVarsList[promVarIndex];
          iScore = aVarScore[iVar];
          if (iScore < 0) {
            aDecPromVarsListPos[iVar] = i;
            aDecPromVarsList[i++] = iVar;
            if (aFlipCounts[iVar] <= iBestVarFlipCount) {
              if (aFlipCounts[iVar] < iBestVarFlipCount) {
                iNumCandidates = 0;
                iBestVarFlipCount = aFlipCounts[iVar];
              }
              aCandidateList[iNumCandidates++] = iVar;
            }
          }
          else {
            aIsDecPromVar[iVar] = FALSE;
          }
        }
        iNumDecPromVars = i;
      }


      if (iNumCandidates > 1)
        iFlipCandidate = TieBreaking();
      else
        iFlipCandidate = aCandidateList[0];

      break;


    case PICK_RANDOM:  /* Select randomly a variable from the
                    promising decreasing variable. */
      i = -1;

      for (promVarIndex = 0; promVarIndex < iNumDecPromVars; promVarIndex++) {
        iVar = aDecPromVarsList[promVarIndex];
        iScore = bPen ? aVarPenScore[iVar] : aVarScore[iVar];
        if (iScore < 0) {
        } else {
          i = promVarIndex;
          aIsDecPromVar[iVar] = FALSE;
          break;
        }
      }

      if (i != -1) {
        for (promVarIndex = i + 1; promVarIndex < iNumDecPromVars; promVarIndex++) {
          iVar = aDecPromVarsList[promVarIndex];
          iScore = aVarScore[iVar];
          if (iScore < 0) {
            aDecPromVarsListPos[iVar] = i;
            aDecPromVarsList[i++] = iVar;
          } else {
            aIsDecPromVar[iVar] = FALSE;
          }
        }
        iNumDecPromVars = i;
      }

      if (iNumDecPromVars > 0) {
        if (iNumDecPromVars == 1)
          iFlipCandidate = aDecPromVarsList[0];
        else
          iFlipCandidate = aDecPromVarsList[RandomInt(iNumDecPromVars)];
      }


      break;
    case PICK_NOVELTY:
      NoveltyProm(FALSE);
      break;

    case PICK_NOVELTYPLUSPLUS:
      NoveltyProm(TRUE);
      if (iNumDecPromVars > 1) {
        if (RandomProb(iPromDp))
          iFlipCandidate = iLeastRecentlyFlippedPromVar;
      }
      break;
    case PICK_NOVELTYPLUS:
      NoveltyProm(FALSE);
      if (iNumDecPromVars > 1) {
        if (RandomProb(iPromWp))
          iFlipCandidate = aDecPromVarsList[RandomInt(iNumDecPromVars)];
      }
      break;
    case PICK_NOVELTYPLUSPLUSPRIME:
      NoveltyProm(FALSE);
      if (iNumDecPromVars > 1) {
        if (RandomProb(iPromDp)) {
          if (iNumDecPromVars == 2) {
            iFlipCandidate = aDecPromVarsList[RandomInt(iNumDecPromVars)];
          } else {
            iNumCandidates = 0;
            for (promVarIndex = 0; promVarIndex < iNumDecPromVars; promVarIndex++) {
              iVar = aDecPromVarsList[promVarIndex];
              if ((iVar != iBestPromVar) && (iVar != iSecondBestPromVar))
                aCandidateList[iNumCandidates++] = iVar;
            }
            if (iNumCandidates != 0) {
              if (iNumCandidates == 1)
                iFlipCandidate = aCandidateList[0];
              else
                iFlipCandidate = aCandidateList[RandomInt(iNumCandidates)];
            }
          }
        }
      }
      break;
    case PICK_NOVELTYPLUSP:
      NoveltyPromisingProm(FALSE);
      break;

    case PICK_GNOVELTYPLUS:
      PickGNoveltyPlusProm();
      break;

    case PICK_DCCA:
      PickCSDvar();

      if (iFlipCandidate == 0) {
        PickNVDvar();
      }

      if (iFlipCandidate == 0) {
        PickSDvar();
      }
      break;

    default:
      // This should never be reached
      break;

      }
}

void PerformHeuristic() {
  UINT32 j;
  UINT32 iVar;
  UINT32 iClause;
  UINT32 iClauseLen;
  LITTYPE *pLit;

  switch (iHeuristic) {

    case H_PICK_NOVELTY:
      if (bTabu)
        PickNoveltyTabu();
      else
        PickNovelty();

      break;

    case H_PICK_NOVELTYPLUS:
      if (bTabu)
        PickNoveltyPlusTabu();
      else
        PickNoveltyPlus();

      break;

    case H_PICK_NOVELTYPLUSPLUS:
      if (bTabu)
        PickNoveltyPlusPlusTabu();
      else
        PickNoveltyPlusPlus();

      break;

    case H_PICK_NOVELTYPLUSPLUSPRIME:
      if (bTabu)
        PickNoveltyPlusPlusPrimeTabu();
      else
        PickNoveltyPlusPlusPrime();
      break;

    case H_PICK_RNOVELTY:
      PickRNovelty();
      break;

    case H_PICK_RNOVELTYPLUS:
      PickRNoveltyPlus();
      break;

    case H_PICK_VW1:
      if (bTabu)
        PickVW1Tabu();
      else
        PickVW1();

      break;

    case H_PICK_VW2:
      if (bTabu)
        PickVW2Tabu();
      else {
        if (!bNoise)
          PickVW2();
        else
          PickVW2Automated();
      }
      break;

    case H_PICK_WALKSAT:
      if (bTabu)
        PickWalkSatTabu();
      else
        PickWalkSatSKC();
      break;

    case H_PICK_NOVELTY_PROMISING:
      if (bTabu)
        PickNoveltyPromisingTabu();
      else
        PickNoveltyPromising();
      break;

    case H_PICK_NOVELTYPLUS_PROMISING:
      if (bTabu)
        PickNoveltyPlusPromisingTabu();
      else
        PickNoveltyPlusPromising();
      break;

    case H_PICK_NOVELTYPLUSPLUS_PROMISING:
      if (bTabu)
        PickNoveltyPlusPlusPromisingTabu();
      else
        PickNoveltyPlusPlusPromising();
      break;

    case H_PICK_NOVELTYPLUSPLUSPRIME_PROMISING:
      if (bTabu)
        PickNoveltyPlusPlusPrimePromisingTabu();
      else
        PickNoveltyPlusPlusPrimePromising();

      break;

    case H_PICK_NOVELTYPLUSFC:
      PickNoveltyPlusFC();
      break;

    case H_PICK_NOVELTYPLUSPROMISINGFC:
      PickNoveltyPlusPromisingFC();
      break;

    case H_PICK_RANDOMPROB:
      if (RandomProb(iDp)) {
        if (iNumFalse) {
          iClause = SelectClause();
          iClauseLen = aClauseLen[iClause];

          pLit = pClauseLits[iClause];

          iFlipCandidate = GetVarFromLit(*pLit);

          pLit++;

          for (j = 1; j < iClauseLen; j++) {
            iVar = GetVarFromLit(*pLit);

            if (aVW2Weights[iVar] < aVW2Weights[iFlipCandidate]) {
              iFlipCandidate = iVar;
            }

            pLit++;
          }
        } else {
          iFlipCandidate = 0;
        }
      } else {

        /* otherwise, use regular novelty */
        if (!bTabu)
          PickNovelty();
        else
          PickNoveltyTabu();
      }

      break;

    case H_PICK_NOVELTYSATTIME:
      PickNoveltySattime();
      break;

    case H_PICK_NOVELTYPLUSSATTIME:
      PickNoveltyPlusSattime();
      break;

    case H_PICK_SPARROWPROBDIST:
      PickSparrowProbDist();

      if (bPromisingList && bPen) {
        ScaleSparrow();
        if (RandomProb(iPs)) {
          SmoothSparrow();
        }
      }

      break;

    case H_PICK_DCCA_DIVERSIFY:
      PickDCCADiversify();
      break;

    default:
      // Should never be reached
      break;
  }
}


void PickSatensteinW() {
/*Just to reduce the size of the file, and there is no need 
  to have a weighted version of Satenstein as it will only
  complicate things more. This portion is deleted now. 
  I have a preliminary version of Satenstein Weighted that works
  but after that so many things have been added to it. That has not 
  been incorporated into the weighted version. 
  */

  //TODO update Satenstein Weighted. 
}


void CreateDecPromVarsW() {

  aDecPromVarsListW = AllocateRAM((iNumVars + 1) * sizeof(UINT32));

}

void InitDecPromVarsW() {

  UINT32 j;

  iNumDecPromVarsW = 0;

  for (j = 1; j <= iNumVars; j++) {
    if (aVarScoreW[j] < FLOATZERO) {
      aDecPromVarsListW[iNumDecPromVarsW++] = j;
    }
  }
}

void UpdateDecPromVarsW() {

  UINT32 j, k;
  UINT32 iVar;

  for (j = 0; j < iNumChangesW; j++) {
    iVar = aChangeListW[j];
    if ((aVarScoreW[iVar] < FLOATZERO) && (aChangeOldScoreW[iVar] >= 0)) {
      aDecPromVarsListW[iNumDecPromVarsW++] = iVar;
    }
  }
  j = 0;
  k = 0;
  while (j < iNumDecPromVarsW) {
    iVar = aDecPromVarsListW[k];
    if ((aVarScoreW[iVar] >= 0) || (iVar == iFlipCandidate)) {
      iNumDecPromVarsW--;
    } else {
      aDecPromVarsListW[j++] = aDecPromVarsListW[k];
    }
    k++;
  }
}


UINT32 TieBreaking() {

  UINT32 j = 0;

  switch (iTieBreaking) {
    case 1:
      return aCandidateList[RandomInt(iNumCandidates)];

    case 2:
      iFlipCandidate = aCandidateList[0];
      for (j = 0; j < iNumCandidates; j++) {
        if (aVarLastChange[iFlipCandidate] > aVarLastChange[aCandidateList[j]]) {
          iFlipCandidate = aCandidateList[j];

        }
      }
      return iFlipCandidate;

    case 3:
      iFlipCandidate = aCandidateList[0];
      for (j = 0; j < iNumCandidates; j++) {
        if (aFlipCounts[iFlipCandidate] > aFlipCounts[aCandidateList[j]]) {
          iFlipCandidate = aCandidateList[j];

        }
      }
      return iFlipCandidate;

    case 4:
      iFlipCandidate = aCandidateList[0];
      for (j = 0; j < iNumCandidates; j++) {
        if (aVW2Weights[aCandidateList[j]] < aVW2Weights[iFlipCandidate]) {
          iFlipCandidate = aCandidateList[j];
        }
      }
      return iFlipCandidate;

    default:
      return 0;
  }

}


BOOL CheckIfFreebie(UINT32 iLookVar) {
  UINT32 i;
  SINT32 iScore = 0;
  LITTYPE *pClause;
  LITTYPE litWasTrue;
  UINT32 iNumOcc;

  if (iLookVar == 0) {
    return FALSE;
  }

  litWasTrue = GetTrueLit(iLookVar);

  iNumOcc = aNumLitOcc[litWasTrue];
  pClause = pLitClause[litWasTrue];

  for (i = 0; i < iNumOcc; i++) {
    if (aNumTrueLit[*pClause] == 1) {
      iScore++;
    }
    pClause++;
  }

  if (iScore == 0)
    return TRUE;
  else
    return FALSE;

}


void NoveltyProm(BOOL trackLastChanged) {
  UINT32 iYoungestVar = updateDecPromVarsNovelty(trackLastChanged);

  if (iNumDecPromVars == 1) {
    iFlipCandidate = iBestPromVar;
    return;
  }

  if (iNumDecPromVars > 1) {
    iFlipCandidate = iBestPromVar;
    if (iBestPromVar == iYoungestVar) {
      if (RandomProb(iPromNovNoise))
        iFlipCandidate = iSecondBestPromVar;
    }
  }

}

UINT32 updateDecPromVarsNovelty(BOOL trackLastChanged) {
  UINT32 i, j;
  UINT32 iLastChange = iStep;
  SINT32 iScore;
  SINT32 iBestScore = bPen ? iSumFalsePen : iNumFalse;
  SINT32 iSecondBestScore = bPen ? iSumFalsePen : iNumFalse;
  UINT32 iYoungestChange = 0;
  UINT32 iYoungestVar = 0;
  UINT32 iVar;
  i = -1;

  for (j = 0; j < iNumDecPromVars; j++) {
    iVar = aDecPromVarsList[j];
    iScore = bPen ? aVarPenScore[iVar] : aVarScore[iVar];
    if (iScore < 0) {
      if (aVarLastChange[iVar] > iYoungestChange) {
        iYoungestChange = aVarLastChange[iVar];
        iYoungestVar = iVar;
      }

      if (trackLastChanged) {
        if (aVarLastChange[iVar] < iLastChange) {
          iLastChange = aVarLastChange[iVar];
          iLeastRecentlyFlippedPromVar = iVar;
        }
      }

      if ((iScore < iBestScore) || ((iScore == iBestScore) && (aVarLastChange[iVar] < aVarLastChange[iBestPromVar]))) {
        iSecondBestPromVar = iBestPromVar;
        iBestPromVar = iVar;
        iSecondBestScore = iBestScore;
        iBestScore = iScore;
      } else if ((iScore < iSecondBestScore) ||
                 ((iScore == iSecondBestScore) && (aVarLastChange[iVar] < aVarLastChange[iSecondBestPromVar]))) {
        iSecondBestPromVar = iVar;
        iSecondBestScore = iScore;
      }


    } else {
      i = j;
      aIsDecPromVar[iVar] = FALSE;
      break;
    }
  }

  if (i != -1) {
    for (j = i + 1; j < iNumDecPromVars; j++) {
      iVar = aDecPromVarsList[j];
      iScore = bPen ? aVarPenScore[iVar] : aVarScore[iVar];
      if (iScore < 0) {
        aDecPromVarsListPos[iVar] = i;
        aDecPromVarsList[i++] = iVar;
        if (aVarLastChange[iVar] > iYoungestChange) {
          iYoungestChange = aVarLastChange[iVar];
          iYoungestVar = iVar;
        }

        if (trackLastChanged) {
          if (aVarLastChange[iVar] < iLastChange) {
            iLastChange = aVarLastChange[iVar];
            iLeastRecentlyFlippedPromVar = iVar;
          }
        }

        if ((iScore < iBestScore) ||
            ((iScore == iBestScore) && (aVarLastChange[iVar] < aVarLastChange[iBestPromVar]))) {
          iSecondBestPromVar = iBestPromVar;
          iBestPromVar = iVar;
          iSecondBestScore = iBestScore;
          iBestScore = iScore;
        } else if ((iScore < iSecondBestScore) ||
                   ((iScore == iSecondBestScore) && (aVarLastChange[iVar] < aVarLastChange[iSecondBestPromVar]))) {
          iSecondBestPromVar = iVar;
          iSecondBestScore = iScore;
        }

      } else {
        aIsDecPromVar[iVar] = FALSE;
      }
    }
    iNumDecPromVars = i;
  }
  return iYoungestVar;
}


void UpdateClauseWeight() {
  register int i, j;
  LITTYPE *pLit;
  UINT32 iClauseLen;
  register UINT32 iClause, iVar;

  for (i = 0; i < iNumFalse; i++) {
    iClause = aFalseList[i];
    if (++aClausePenaltyINT[iClause] == 2) {
      aWhereWeight[iClause] = iNumWeighted;
      aWeightedList[iNumWeighted] = iClause;
      iNumWeighted++;
    }
    iClauseLen = aClauseLen[iClause];
    pLit = pClauseLits[iClause];
    for (j = 0; j < iClauseLen; j++) {
      iVar = GetVarFromLit(*pLit);
      --aVarPenScore[iVar];
      if (!aIsDecPromVar[iVar] && (aVarPenScore[iVar] < 0) && (aVarLastChange[iVar] < iStep - 1)) {
        aDecPromVarsListPos[iVar] = iNumDecPromVars;
        aDecPromVarsList[iNumDecPromVars++] = iVar;
        aIsDecPromVar[iVar] = TRUE;
      }
      pLit++;
    }

  }
  iSumFalsePen += iNumFalse;
  iTotalWeight += iNumFalse;
}

void Smooth() {
  register int i, j;
  LITTYPE *pLit;
  register UINT32 iClause, iVar;
  UINT32 iClauseLen;
  UINT32 iStartNumWeighted;

  iStartNumWeighted = iNumWeighted;
  iTotalWeight -= iNumWeighted;

  for (j = 0; j < iStartNumWeighted; j++) {

    iClause = aWeightedList[j];

    if (--aClausePenaltyINT[iClause] == 1) {
      --iNumWeighted;
      aWeightedList[aWhereWeight[iClause]] = aWeightedList[iNumWeighted];
      aWhereWeight[aWeightedList[iNumWeighted]] = aWhereWeight[iClause];
    }

    if (aNumTrueLit[iClause] == 0) {
      --iSumFalsePen;
      iClauseLen = aClauseLen[iClause];
      pLit = pClauseLits[iClause];
      for (i = 0; i < iClauseLen; i++) {
        iVar = GetVarFromLit(*pLit);
        ++aVarPenScore[iVar];
        pLit++;
      }
    }
    if (aNumTrueLit[iClause] == 1) {
      iVar = aCritSat[iClause];
      --aVarPenScore[iVar];
      if (!aIsDecPromVar[iVar] && (aVarPenScore[iVar] < 0) && (aVarLastChange[iVar] < iStep - 1)) {
        aDecPromVarsListPos[iVar] = iNumDecPromVars;
        aDecPromVarsList[iNumDecPromVars++] = iVar;
        aIsDecPromVar[iVar] = TRUE;
      }

    }

  }
}

void NoveltyPromisingProm(BOOL trackLastChanged) {
  SINT32 iSecondBestLookAheadScore, iBestLookAheadScore;
  UINT32 iYoungestVar = updateDecPromVarsNovelty(trackLastChanged);

  if (iNumDecPromVars == 1) {
    iFlipCandidate = iBestPromVar;
    return;
  }

  if (iNumDecPromVars > 1) {
    iFlipCandidate = iBestPromVar;
    if (iFlipCandidate == iYoungestVar) {
      if (RandomProb(iPromNovNoise)) {
        iFlipCandidate = iSecondBestPromVar;
        return;
      }
    }

    /* If the best is older than then 2nd best, just choose the best */
    if (aVarLastChange[iSecondBestPromVar] >= aVarLastChange[iFlipCandidate]) {
      return;
    }

    /* otherwise, determine the 'look ahead' score for the 2nd best variable */

    iSecondBestLookAheadScore = bPen ? aVarPenScore[iSecondBestPromVar] + BestLookAheadPenScore(iSecondBestPromVar) :
                                          aVarScore[iSecondBestPromVar] + BestLookAheadScore(iSecondBestPromVar);

    if (iSecondBestLookAheadScore > iBestScore) {
      iBestLookAheadScore = iBestScore;
    } else {

      /* if the 'look ahead' score for the 2nd variable is better than the regular score
         for the best variable, calculate the look ahead score for the best variable */

      iBestLookAheadScore = bPen ? aVarPenScore[iFlipCandidate] + BestLookAheadPenScore(iFlipCandidate) :
                                      aVarScore[iFlipCandidate] + BestLookAheadScore(iFlipCandidate);
    }

    /* choose the variable with the best look ahead score */

    /* Note that this BREAKS TIES by selecting the 2nd best variable -- as in the paper and in the author's code */

    if (iBestLookAheadScore >= iSecondBestLookAheadScore) {
      iFlipCandidate = iSecondBestPromVar;
    }
  }

}

