//
// Created by pcernek on 7/27/15.
//

#include <sstream>
#include "ubcsat-facade.hpp"
#include "ubcsat-globals.h"
#ifdef __MACH__
#include <mach/clock.h>
#include <mach/mach.h>
#endif
#ifdef __APPLE__
#include "fmemopen.h"
#endif

/**
 * This is a convenience function intended as a nominal attempt to deal with the horror of
 *  all the global state that can spill over from one configuration of UBCSAT to the next, in
 *  the event that two different algorithms of UBCSAT are run one after the other (e.g. first
 *  run -alg satenstein, then run -alg sparrow [aka "standalone" sparrow]).
 *
 * The intent is to reset statically-allocated global variables to 0 every time a configuration
 *  is destroyed. However, for this to work, we would need to guarantee that every such variable
 *  that occurs throughout the code is reset in this function. Since this kind of an approach is
 *  inherently unmaintainable, this function is meant as nothing more than a "best effort", and
 *  cannot guarantee that state will be completely cleaned up across different algorithms within
 *  the same instantiation of this library.
 *
 * Maintainers are invited to add such variables to this function as they are encountered.
 *
 * @author Paul Cernek
 */
void resetAllStaticallyAllocatedGlobalVars();

ubcsat_state::ubcsat_state() {
  resultState = 0;
  assignment = nullptr; // the first entry in this array contains its length
  errorState = 0;
  errorMessage = (char*) malloc(1024 * sizeof(char));
}

/* TODO: We can't currently do all this one-time initialization stuff
 *  because UBCSAT's memory model is not made to handle multiple different
 *  instances in one run, so the easiest way to de-allocate everything
 *  is to completely kill the library. In the future, if we're brave
 *  we might find a more efficient way of doing this.
 */
void initLibrary() {
//  InitSeed();
//  SetupUBCSAT();
//  AddAlgorithms();
//  AddParameters();
//  AddReports();
//  AddDataTriggers();
//  AddReportTriggers();
//  AddLocal();
}

// Use clock_gettime in linux, clock_get_time in OS X.
// see http://stackoverflow.com/questions/21665641/ns-precision-monotonic-clock-in-c-on-linux-and-os-x/21665642#21665642
void getMonotonicTime(struct timespec *ts){
#ifdef __MACH__
  clock_serv_t cclock;
  mach_timespec_t mts;
  host_get_clock_service(mach_host_self(), SYSTEM_CLOCK, &cclock);
  clock_get_time(cclock, &mts);
  mach_port_deallocate(mach_task_self(), cclock);
  ts->tv_sec = mts.tv_sec;
  ts->tv_nsec = mts.tv_nsec;
#else
  clock_gettime(CLOCK_MONOTONIC, ts);
#endif
}

double getElapsedTime(struct timespec *before, struct timespec *after){
  double deltat_s  = after->tv_sec - before->tv_sec;
  double deltat_ns = after->tv_nsec - before->tv_nsec;
  return deltat_s + deltat_ns*1e-9;
}

char** split(char *command, int* size) {
  char** ret = (char**) malloc(sizeof(char*) * MAXTOTALPARMS);
  const char* programName = "ubcsat";
  ret[0] = (char*) programName;
  char* t;
  int i;
  char* saveptr;
  for (i = 1, t = strtok_r(command, " ", &saveptr); t != NULL; ++i) {
    ret[i] = t;
    t = strtok_r(NULL, " ", &saveptr);
  }
  *size = i;
  return ret;
}

void* initConfig(const char* params) {
  InitSeed();
  SetupUBCSAT();
  AddAlgorithms();
  AddParameters();
  AddReports();
  AddDataTriggers();
  AddReportTriggers();
  AddLocal();

  fflush(stdout);

  int* numParams = (int*) malloc(sizeof(int));
  char* paramsMutable = (char *) malloc((strlen(params) + 1) * sizeof(char));
  strcpy(paramsMutable, params);
  char** args = split(paramsMutable, numParams);

  UBCSATState * ubcsat = new UBCSATState;

  ParseAllParameters(*numParams, args);
  ActivateAlgorithmTriggers();
  RandomSeed(ubcsat::iSeed);

  RunProcedures(PostParameters);
  ActivateTriggers((char *) "CheckTimeout");

  free(args);
  free(numParams);
  free(paramsMutable);

  fflush(stdout);
  return ubcsat;
}

int initProblem(void* ubcsatState, const char* problem) {
  // timing code
//  clock_t t1, t2;
//  t1 = clock();

  UINT32 j;
  UINT32 k;
  UINT32 bIsWCNF;
  float fDummy;
  SINT32 l;
  SINT32 iScanRet;
  long unsigned int w;


  LITTYPE *pData;
  LITTYPE *pNextLit;
  LITTYPE *pLastLit;

  FILE *filInput;

  bIsWCNF = FALSE;

  iNumClauses = 0;

  filInput = fmemopen((void *) problem, strlen(problem), "r");

  while (iNumClauses == 0) {
    fgets(sLine,MAXCNFLINELEN,filInput);
    if (strlen(sLine)==MAXCNFLINELEN-1) {
      ReportPrint1(pRepErr,"Unexpected Error: increase constant MAXCNFLINELEN [%u]\n",MAXCNFLINELEN);
      AbnormalExit();
    }

    if (strncmp(sLine,"p wcnf",6)==0)
      bIsWCNF = TRUE;

    if (sLine[0] =='p') {
      if (bWeighted) {
        if (bIsWCNF) {
          sscanf(sLine,"p wcnf %" SCAN32 " %" SCAN32 "",&iNumVars,&iNumClauses);
        } else {
          ReportPrint(pRepErr,"Warning! reading .cnf file and setting all weights = 1\n");
          sscanf(sLine,"p cnf %" SCAN32 " %" SCAN32 "",&iNumVars,&iNumClauses);
        }
      } else {
        if (bIsWCNF) {
          ReportPrint(pRepErr,"Warning! reading .wcnf file and ignoring all weights\n");
          sscanf(sLine,"p wcnf %" SCAN32 " %" SCAN32 "",&iNumVars,&iNumClauses);
        } else {
          sscanf(sLine,"p cnf %" SCAN32 " %" SCAN32 "",&iNumVars,&iNumClauses);
        }
      }
    } else {
      if (sLine[0] =='c') {

      } else {
        ReportPrint1(pRepErr,"Warning: Ignoring line in input file:\n   %s",sLine);
      }
    }
  }

  if ((iNumVars==0)||(iNumClauses==0)) {
    ReportPrint(pRepErr,"Error: invalid instance file\n");
    AbnormalExit();
  }

  iVARSTATELen = (iNumVars >> 3) + 1;
  if ((iNumVars & 0x07)==0) {
    iVARSTATELen--;
  }

  aClauseLen = (UINT32 *) AllocateRAM(iNumClauses * sizeof(UINT32));
  pClauseLits = (LITTYPE **) AllocateRAM(iNumClauses * sizeof(LITTYPE *));
  if (bWeighted) {
    aClauseWeight = (FLOAT *) AllocateRAM(iNumClauses * sizeof(FLOAT));
  }

  pLastLit = pNextLit = pData = 0;

  iNumLits = 0;
  iMaxClauseLen = 0;


  for (j=0;j<iNumClauses;j++) {

    if (bWeighted) {
      if (bIsWCNF) {
        iScanRet = fscanf(filInput,"%" SCAN64 ,&w);
        if (iScanRet != 1) {
          ReportHdrPrefix(pRepErr);
          ReportHdrPrint1(pRepErr,"Error reading clause weight at clause [%" P32 "]\n",j);
          ReportHdrPrint1(pRepErr,"  at or near: %s\n",sLine);
          aClauseWeight[j] = 1;
        }
        aClauseWeight[j] = w;
      } else {
        aClauseWeight[j] = 1;
      }
      fTotalWeight += aClauseWeight[j];
    } else {
      if (bIsWCNF) {
        fscanf(filInput,"%" SCAN64 ,&w);
      }
    }

    pClauseLits[j] = pNextLit;
    aClauseLen[j] = 0;

    do {
      iScanRet = fscanf(filInput,"%" SCANS32 ,&l);

      while (iScanRet != 1) {
        if (iScanRet==0) {
          fgets(sLine,MAXCNFLINELEN,filInput);

          if (sLine[0] =='c') {
            iScanRet = fscanf(filInput,"%" SCANS32 ,&l);
          } else {
            ReportPrint1(pRepErr,"Error reading instance at clause [%" P32 "]\n",j);
            ReportPrint1(pRepErr,"  at or near: %s\n",sLine);
            AbnormalExit();
          }
        } else {
          ReportPrint1(pRepErr,"Error reading instance. at clause [%" P32 "]\n",j);
          AbnormalExit();
        }
      }

      if (l) {

        if (pNextLit >= pLastLit) {
          pData = (LITTYPE *) AllocateRAM(LITSPERCHUNK * sizeof(LITTYPE));
          pNextLit = pData;
          pLastLit = pData + LITSPERCHUNK;
          for (k=0;k<aClauseLen[j];k++) {
            *pNextLit = pClauseLits[j][k];
            pNextLit++;
          }
          pClauseLits[j] = pData;
        }

        *pNextLit = SetLitFromFile(l);

        if (GetVarFromLit(*pNextLit) > iNumVars) {
          ReportPrint3(pRepErr,"Error: Invalid Literal [%" PS32 "] in clause [%" P32 "] exceeds total number of variables [%" P32 "]\n",l,j, iNumVars);
          AbnormalExit();
        }

        pNextLit++;
        aClauseLen[j]++;
        iNumLits++;
      }
    } while (l != 0);

    if (aClauseLen[j] > iMaxClauseLen) {
      iMaxClauseLen = aClauseLen[j];
    }

    if (aClauseLen[j] == 0) {
      ReportPrint1(pRepErr,"Error: Reading .cnf, clause [%" P32 "] is empty\n",j);
      AbnormalExit();
    }
  }

  AdjustLastRAM((pNextLit - pData) * sizeof(LITTYPE));

  CloseSingleFile(filInput);

  RunProcedures(PostRead);

  RunProcedures(CreateData);
  RunProcedures(CreateStateInfo);

  RunProcedures(PreStart);

  RunProcedures(PreRun);

  // timing code
//  t2 = clock();
//  float diff = (((float)t2 - (float)t1) / CLOCKS_PER_SEC ) * 1000;
//  printf("C PRINTF: time to init problem %fs\n", diff);
  fflush(stdout);

  return TRUE;

}

int initAssignment(void* ubcsatState, const long* assignment, int sizeOfAssignment) {

  UBCSATState* state = (UBCSATState *) ubcsatState;
  if (sizeOfAssignment > iNumVars) {
    std::ostringstream msg;
    msg << "Error: tried to give an assignment for " << sizeOfAssignment <<
        " variables, which is greater than the total number of variables (" << iNumVars << ")";
    state->errorMessage = msg.str().c_str();
    return FALSE;
  }

  for (UINT32 j=1;j<=iNumVars;j++) {
    aVarInit[j] = 2;
  }

  long varNumAndAssignment;
  long varNum;

  for(int i=0; i < sizeOfAssignment; i++) {
    varNumAndAssignment = assignment[i];

    int multiplier;
    if (varNumAndAssignment > 0) {
      multiplier = 1;
    }
    else if (varNumAndAssignment < 0) {
      multiplier = -1;
    }
    else {
      std::ostringstream msg;
      msg << "Error: tried to give an assignment for variable 0, but 0 is a disallowed variable id.";
      state->errorMessage = msg.str().c_str();
      return FALSE;
    }

    varNum = varNumAndAssignment * multiplier;

    if (varNum > iNumVars) {
      std::ostringstream msg;
      msg << "Error: tried to give an assignment for variable number " << varNumAndAssignment <<
      ", whose magnitude exceeds the total number of variables (" << iNumVars << ")";
      state->errorMessage = msg.str().c_str();
      return FALSE;
    }

    aVarInit[varNum] = (UINT32) (multiplier == 1);
  }

  return TRUE;
}

int solveProblem(void* ubcsatState, double timeoutTime) {

  // measure elapsed wall time
  struct timespec now, tmstart;
  getMonotonicTime(&tmstart);

  UBCSATState* state = (UBCSATState*) ubcsatState;
  iStep = 0;
  bSolutionFound = FALSE;
  bTerminateRun = FALSE;
  bRestart = TRUE;

  StartRunClock();

  while ((iStep < iCutoff) && (!bSolutionFound) && !bTerminateRun && !state->terminateRun) {

     // check walltime cutoff
     getMonotonicTime(&now);
     double seconds = getElapsedTime(&tmstart, &now);

     if (seconds > timeoutTime) {
       break;
     }

    iStep++;
    iFlipCandidate = 0;

    RunProcedures(PreStep);
    RunProcedures(CheckRestart);

    if (bRestart) {
      RunProcedures(InitData);
      RunProcedures(InitStateInfo);
      RunProcedures(PostInit);
      bRestart = FALSE;
    }
    else {
      RunProcedures(ChooseCandidate);
      RunProcedures(PreFlip);
      RunProcedures(FlipCandidate);
      RunProcedures(UpdateStateInfo);
      RunProcedures(PostFlip);
    }

    RunProcedures(PostStep);

    RunProcedures(StepCalculations);

    RunProcedures(CheckTerminate);
  }

  StopRunClock();

  if(bSolutionFound) {
    state->resultState = 1;
    state->assignment = (int *) AllocateRAM( (iNumVars + 1) * sizeof(int) );
    state->assignment[0] = iNumVars;
    int multiplier;
    for (int i = 1; i <= iNumVars; i++) {
      multiplier = 1;
      if (aVarValue[i] == FALSE) {
        multiplier = -1;
      }
      state->assignment[i] = multiplier * i;
    }
  }
  else if (state->resultState == 0) { // resultState has not been changed since initialization
    state->resultState = 2;
  }

  // printf("Reasons for returning:\n iStep < iCutoff is %d \n !bSolutionFound is %d\n!bTerminateRun is %d\n !state->terminateRun is %d\n", iStep < iCutoff, !bSolutionFound, !bTerminateRun, !state->terminateRun);
  // fflush(stdout);
  return TRUE;
}

void destroyProblem(void* ubcsatState) {
  UBCSATState * ubcsat = (UBCSATState *) ubcsatState;
  ResetTriggers();
  CleanExit();
  resetAllStaticallyAllocatedGlobalVars();
  delete ubcsat;
  fflush(stdout);
}

void interrupt(void* ubcsatState) {
  UBCSATState * ubcsat = (UBCSATState *) ubcsatState;
  ubcsat->resultState = 3;
  ubcsat->terminateRun = TRUE;
}

int getProblemState(void* ubcsatState) {
  UBCSATState * ubcsat = (UBCSATState *) ubcsatState;
  return ubcsat->errorState;
}

int* getResultAssignment(void* ubcsatState) {
  UBCSATState * ubcsat = (UBCSATState *) ubcsatState;
  return ubcsat->assignment;
}

int getResultState(void* ubcsatState) {
  UBCSATState* state = (UBCSATState *) ubcsatState;
  return state->resultState;
}

const char* getErrorMessage(void* ubcsatState) {
  UBCSATState* state = (UBCSATState *) ubcsatState;
  return state->errorMessage;
}

int getNumVars() {
  return iNumVars;
}

int getNumClauses() {
  return iNumClauses;
}

int getVarAssignment(int varNumber) {
  return aVarValue[varNumber];
}

int runInitData() {
  RunProcedures(InitData);
}

void resetAllStaticallyAllocatedGlobalVars() {
  // From algorithms.h
  bTabu = FALSE;
  bVarInFalse = FALSE;
  bPromisingList = FALSE;
  iTieBreaking = 0;
  bPerformNoveltyAlternate = FALSE;
  iUpdateSchemePromList = 0;
  iAdaptiveNoiseScheme = 0;
  iPromNovNoise = 0;
  iPromDp = 0;
  iPromWp = 0;
  iScoringMeasure = 0;

  iTabuTenureInterval = 0;
  iTabuTenureLow = 0;
  iTabuTenureHigh = 0;

  iWp = 0;
  iTabuTenure = 0;
  iWalkSATTabuClause = 0;
  iNovNoise = 0;
  iDp = 0;
  iLastAdaptStep = 0;
  iLastAdaptNumFalse = 0;
  fLastAdaptSumFalseW = 0;
  iInvPhi = 0;
  iInvTheta = 0;
  iPromInvPhi = 0;
  iPromInvTheta = 0;
  intNovNoise = 0;
  intDp = 0;
  bAdaptPromWalkProb = FALSE;
  iWpWalk = 0;
  bAdaptWalkProb = FALSE;
  fAlpha = 0;
  fRho = 0;
  fPenaltyImprove = 0;
  iPs = 0;
  iRPs = 0;
  iPAWSFlatMove = 0;
  iPAWSMaxInc = 0;
  iNumPenClauses = 0;

  // From ubcsat-globals.h
  bWeighted = FALSE;
  iNumRuns = 0;
  iCutoff = 0;
  fTimeOut = 0;
  fGlobalTimeOut = 0;
  iSeed = 0;
  iTarget = 0;
  fTargetW = 0;
  iFlipCandidate = 0;
  iFind = 0;
  iNumSolutionsFound = 0;
  iFindUnique = 0;
  iPeriodicRestart = 0;
  iProbRestart = 0;
  iStagnateRestart = 0;
  bRestart = FALSE;
  iRun = 0;
  iStep = 0;
  bTerminateAllRuns = FALSE;
  bSolutionFound = FALSE;
  bTerminateRun = FALSE;
  bSolveMode = FALSE;
  iBestScore = 0;
  fBestScore = 0;

  // from ubcsat-triggers.h
  iNumVars = 0;
  iNumClauses = 0;
  iNumLits = 0;
  iMaxClauseLen = 0;
  fTotalWeight = 0;
  iTotalWeight = 0;
  iVARSTATELen = 0;
  dRunTime = 0;
  iNumCandidates = 0;
  iMaxCandidates = 0;
  iInitVarFlip = 0;
  bVarInitGreedy = FALSE;
  iRandomVarInitPercentage = 0;
  iIgnoreStartingAssignmentPercentage = 0;
  iNumFalse = 0;
  fSumFalseW = 0;
  iSumFalsePen = 0;
  fSumClauseVarFlipCount = 0;
  fSumClauseVarFlipCount;
  iNumFalseList = 0;
  iNumVarsInFalseList = 0;
  iVarLastChangeReset = 0;
  bTrackChanges = FALSE;
  iNumChanges = 0;
  iNumChangesW = 0;
  bPen = FALSE;
  iNumDecPromVars = 0;
  iNumBestScoreList = 0;
  bClausePenaltyCreated = FALSE;
  bClausePenaltyFLOAT = FALSE;
  fBasePenaltyFL = 0;
  fTotalPenaltyFL = 0;
  iInitPenaltyINT = 0;
  iBasePenaltyINT = 0;
  iTotalPenaltyINT = 0;
  iNumNullFlips = 0;
  iNumLocalMins = 0;
  iNumLogDistValues = 0;
  iLogDistStepsPerDecade = 0;
  iBestNumFalse = 0;
  iBestStepNumFalse = 0;
  fBestSumFalseW = 0;
  iBestStepSumFalseW = 0;
  iStartNumFalse = 0;
  fStartSumFalseW = 0;
  fImproveMean = 0;
  fImproveMeanW = 0;
  iFirstLM = 0;
  iFirstLMStep = 0;
  fFirstLMW = 0;
  iFirstLMStepW = 0;
  fFirstLMRatio = 0;
  fFirstLMRatioW = 0;
  fTrajBestLMMean = 0;
  fTrajBestLMMeanW = 0;
  fTrajBestLMCV = 0;
  fTrajBestLMCVW = 0;
  iNoImprove = 0;
  iStartSeed = 0;
  fFlipCountsMean = 0;
  fFlipCountsCV = 0;
  fFlipCountsStdDev = 0;
  fUnsatCountsMean = 0;
  fUnsatCountsCV = 0;
  fUnsatCountsStdDev = 0;
  iVarFlipHistoryLen = 0;
  iAutoCorrMaxLen = 0;
  fAutoCorrCutoff = 0;
  iAutoCorrLen = 0;
  fAutoCorrOneVal = 0;
  fAutoCorrOneEst = 0;
  fBranchFactor = 0;
  fBranchFactorW = 0;
  iNumUpSteps = 0;
  iNumDownSteps = 0;
  iNumSideSteps = 0;
  iNumUpStepsW = 0;
  iNumDownStepsW = 0;
  iNumSideStepsW = 0;
  iNumRestarts = 0;
  bKnownSolutions = FALSE;
  fFDCRun = 0;
  iNumUniqueSolutions = 0;
  iLastUnique = 0;
  iNumDecPromVars = 0;
  iNumWeighted = 0;
  bPen = FALSE;
  bPerformClauseConfChecking = FALSE;
  bPerformNeighborConfChecking = FALSE;

}
