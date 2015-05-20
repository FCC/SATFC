#!/bin/bash
#Compiles the clasp JNA library .so file for SATFC.

tar -xzvf libjnaclasp-3.tar.gz
cd libjnaclasp-3/jna
./build.sh $1 $2 # These arguments are TBB_INCLUDE and TBB_LIB. If you have tbb properly installed, these arguments are not necessary
mkdir -p ../../jna
mv libjnaclasp.so ../../jna
cd ../../
rm -Rf libjnaclasp-3
