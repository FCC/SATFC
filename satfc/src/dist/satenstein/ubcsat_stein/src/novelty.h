//
// Created by pcernek on 6/12/15.
//

#ifndef SATENSTEIN_NOVELTY_H
#define SATENSTEIN_NOVELTY_H


#include "ubcsat.h"


extern UINT32 SelectClause();

extern UINT32 iNumDecPromVars;
extern UINT32 *aDecPromVarsList;


void PickNovelty();
void PickNoveltyW();
void PickNoveltyTabu();
void PickNoveltyTabuW();

void PickNoveltyPlus();
void PickNoveltyPlusW();
void PickNoveltyPlusTabu();
void PickNoveltyPlusTabuW();

void PickNoveltyPlusPlus();
void PickNoveltyPlusPlusW();
void PickNoveltyPlusPlusTabu();
void PickNoveltyPlusPlusTabuW();

void PickNoveltyPlusPlusPrime();
void PickNoveltyPlusPlusPrimeW();
void PickNoveltyPlusPlusPrimeTabu();
void PickNoveltyPlusPlusPrimeTabuW();

void PickNoveltyPromising();
void PickNoveltyPromisingW();
void PickNoveltyPromisingTabu();
void PickNoveltyPromisingTabuW();

void PickNoveltyPlusPromising();
void PickNoveltyPlusPromisingW();
void PickNoveltyPlusPromisingTabu();
void PickNoveltyPlusPromisingTabuW();


void PickNoveltyPlusPlusPromising();
void PickNoveltyPlusPlusPromisingW();
void PickNoveltyPlusPlusPromisingTabu();
void PickNoveltyPlusPlusPromisingTabuW();

void PickNoveltyPlusPlusPrimePromising();
void PickNoveltyPlusPlusPrimePromisingW();
void PickNoveltyPlusPlusPromisingTabu();

void PickNoveltyPlusFC();
void PickNoveltyPlusPromisingFC();

void PickNoveltySattime();
void PickNoveltyPlusSattime();

void InitSimulatedFlip();
void CreateSimulatedFlip();
void InitLookAhead();
void CreateLookAhead();

#endif //SATENSTEIN_NOVELTY_H
