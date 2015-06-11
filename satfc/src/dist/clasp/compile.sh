#!/bin/bash
# Compiles the clasp JNA library .so file for SATFC.

#tar -xzvf libjnaclasp-3.tar.gz
cd libjnaclasp-3/jna
echo "Building clasp ..."
./build.sh $1 $2 # These arguments are TBB_INCLUDE and TBB_LIB. Depending on how you have tbb installed, these arguments may not be necessary. DO NOT USE RELATIVE PATHS
echo "Moving clasp library ..."
mkdir -p ../../jna
mv libjnaclasp.so ../../jna
cd ../../
#rm -Rf libjnaclasp-3
