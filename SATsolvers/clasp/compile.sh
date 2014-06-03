#!/bin/bash
#Compiles the clasp JNA library .so file for SATFC.

tar -xvf libjnaclasp-2.1.3.tar.gz
cd libjnaclasp-2.1.3
./build.sh
mkdir -p ../jna
mv libjnaclasp.so ../jna
cd ../
rm -Rf libjnaclasp-2.1.3
