#!/bin/bash -e
# Compiles the UBCSAT/SATenstein JNA library .so file for SATFC.

cd ubcsat_stein
rm -rf build
mkdir build
cd build
echo "Building satenstein ..."
cmake ..
make jnasatenstein
echo "Moving satenstein library ..."
mkdir -p ../../jna
if [ -f libjnasatenstein.dylib ]
  then
    mv libjnasatenstein.dylib libjnasatenstein.so
fi
mv libjnasatenstein.so ../../jna
cd ../..
