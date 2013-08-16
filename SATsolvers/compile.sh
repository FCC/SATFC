#!/bin/bash

cd clasp
tar -xvf libjnaclasp-2.1.3.tar.gz
cd libjnaclasp-2.1.3
./build.sh
mv libjnaclasp.so ../jna
cd ../
rm -Rf libjnaclasp-2.1.3
