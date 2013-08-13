#!/bin/bash

CLASP=../clasp-2.1.3
CURRENT=`pwd`

#configure clasp
echo -n "Cleaning previous make... "
make clean
echo "Done."
echo "CLASP----------------------------------------------"
cd $CLASP
./configure.sh --config=jna CXXFLAGS="-O3 -DNDEBUG -fPIC"
#./configure.sh --config=jna CXXFLAGS="-g -fPIC"
cd build/jna
make clean

# copy my modified headers and code to handle interrupts
mv $CLASP/libclasp/clasp/solve_algorithms.h $CLASP/libclasp/clasp/solve_algorithms.h.bak
mv $CLASP/libclasp/src/solve_algorithms.cpp $CLASP/libclasp/src/solve_algorithms.cpp.bak
cp $CURRENT/solve_algorithms.h $CLASP/libclasp/clasp/solve_algorithms.h
cp $CURRENT/solve_algorithms.cpp $CLASP/libclasp/src/solve_algorithms.h
make
mv $CLASP/libclasp/clasp/solve_algorithms.h.bak $CLASP/libclasp/clasp/solve_algorithms.h
mv $CLASP/libclasp/src/solve_algorithms.cpp.bak $CLASP/libclasp/src/solve_algorithms.cpp
echo "---------------------------------------------------"
echo "JNA_CLASP------------------------------------------"
cd $CURRENT
# copy the libs
cp $CLASP/build/jna/libclasp/lib/libclasp.a ./
cp $CLASP/build/jna/libprogram_opts/lib/libprogram_opts.a ./

cd $CURRENT
#use the FLAGS that were set in the clasp configure.sh
cp $CLASP/build/jna/.CONFIG ./CLASP_CONFIG
echo "PROJECT_ROOT := $CLASP" > PROJECT_ROOT

make

#make clean

rm CLASP_CONFIG
rm PROJECT_ROOT
echo "---------------------------------------------------"
