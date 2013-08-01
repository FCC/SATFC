#!/bin/bash
GLUE=glueminisat-2.2.7

cd jna
make slibs
make clean
mv libglueminisat_standard.so ../libglueminisat.so
cd ../
