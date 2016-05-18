//
// Created by pcernek on 6/25/15.
//

#include "AddRemoveListsTest.h"

TEST_F(AddRemoveListsTest, addOnce) {
  append(1);
  EXPECT_EQ(1, _list[0]);
  EXPECT_EQ(0, _listPos[1]);
  EXPECT_EQ(1, _listSize);
  EXPECT_EQ(TRUE, _isInList[1]);
  for (int i=2; i <= iNumVars; i++) {
    EXPECT_EQ(FALSE, _isInList[i]);
  }
}

TEST_F(AddRemoveListsTest, addTwice) {
  append(1);
  append(2);
  EXPECT_EQ(1, _list[0]);
  EXPECT_EQ(2, _list[1]);
  EXPECT_EQ(0, _listPos[1]);
  EXPECT_EQ(1, _listPos[2]);
  EXPECT_EQ(2, _listSize);
  EXPECT_EQ(TRUE, _isInList[1]);
  EXPECT_EQ(TRUE, _isInList[2]);
  for (int i=3; i <= iNumVars; i++) {
    EXPECT_EQ(FALSE, _isInList[i]);
  }
}


TEST_F(AddRemoveListsTest, addRemoveOnce) {
  append(1);
  remove(1);
  EXPECT_EQ(0, _listSize);
  for (int i=1; i <= iNumVars; i++) {
    EXPECT_EQ(FALSE, _isInList[i]);
  }
}

TEST_F(AddRemoveListsTest, addRemoveTwice) {
  append(1);
  remove(1);
  append(2);
  remove(2);
  EXPECT_EQ(0, _listSize);
  for (int i=1; i <= iNumVars; i++) {
    EXPECT_EQ(FALSE, _isInList[i]);
  }
}

TEST_F(AddRemoveListsTest, removeEmptyFail) {
  remove(1);
}

AddRemoveListsTest::AddRemoveListsTest() {
  iNumVars = 6;
  initLists();
  populateLists();
}

void AddRemoveListsTest::initLists() {
  _list     = new UINT32[iNumVars + 1];
  _listPos  = new UINT32[iNumVars + 1];
  _listSize = 0;
  _isInList = new BOOL[iNumVars + 1];

  for (int i=1; i <= iNumVars; i++) {
    _isInList[i] = FALSE;
  }
}

void AddRemoveListsTest::populateLists() {
  }

void AddRemoveListsTest::append(int id) {
  AddToList2(id, _list, _listPos, &_listSize, _isInList);
}

void AddRemoveListsTest::remove(int id) {
  RemoveFromList2(id, _list, _listPos, &_listSize, _isInList);
}
