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

#include "algorithms.h"

void AddAlgorithms() {

  AddGSat();  
  AddGWSat();
  AddGSatTabu();

  AddHSat();
  AddHWSat();

  AddWalkSat();
  AddWalkSatTabu();

  AddNovelty();
  AddNoveltyTabu();
  AddNoveltyPlus();
  AddNoveltyPlusTabu();
  AddNoveltyPlusPlus();
  AddNoveltyPlusPlusTabu();
  AddNoveltyPlusPlusPrime();
  AddNoveltyPromising();
  AddNoveltyPromisingTabu();
  AddNoveltyPlusPromising();
  AddNoveltyPlusPromisingTabu();
  AddNoveltyPlusPlusPromising();
  AddNoveltyPlusPlusPromisingTabu();
  AddNoveltyPlusPlusPrimePromising();
  AddNoveltyPlusPlusPrimePromisingTabu();
  AddNoveltyPlusFC(); 

  AddAdaptNoveltyPlus();

 // AddAdaptNovelty();

  AddRNovelty();
  AddRNoveltyPlus();

  AddVW1();
  AddVW2();

  AddSAPS();

  AddPAWS();

  AddDDFW();

  AddSparrow();

  AddSatenstein();
  AddAdaptG2WSatPlus();

  AddRoTS();
  AddIRoTS();

  AddSAMD();

 
  AddRandom();

  AddDCCA();
}

