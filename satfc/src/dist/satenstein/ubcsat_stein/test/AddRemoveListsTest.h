//
// Created by pcernek on 6/25/15.
//

#ifndef SATENSTEIN_ADDREMOVELISTSTEST_H
#define SATENSTEIN_ADDREMOVELISTSTEST_H

#include <gtest/gtest.h>

extern "C" {
  #include "ubcsat.h"
};

class AddRemoveListsTest : public ::testing::Test {

protected:
  AddRemoveListsTest();

  void initLists();
  void populateLists();

  void append(int id);
  void remove(int id);

  UINT32* _list;
  UINT32* _listPos;
  UINT32 _listSize;
  BOOL* _isInList;

};


#endif //SATENSTEIN_ADDREMOVELISTSTEST_H
