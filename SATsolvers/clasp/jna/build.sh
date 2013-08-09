#!/bin/bash

CLASP=../clasp-2.1.3
CURRENT=`pwd`

function static2shared ()
{
	mkdir tmp
	cd tmp
	cp $CURRENT/$1 ./
	name=`basename $1 .a`
	ar -x $name.a
	rm $name.a
	g++ -fPIC -shared *.o -o $name.so
	rm *.o
	mv $name.so ../
	cd ../
	rmdir tmp
}

function compileShared ()
{
	name=`basename $1`
	echo -n "Compiling shared library $name.so ..."
	g++ -fPIC -shared $CURRENT/$1/*.o -o $name.so
	echo  "	Done."
}

#configure clasp
echo "CLASP----------------------------------------------"
cd $CLASP
if [ "$1" == "1" ]
then
	./configure.sh --config=jna CXXFLAGS="-O3 -DNDEBUG -fPIC"
fi
cd build/jna
make
echo "---------------------------------------------------"
echo "JNA_CLASP------------------------------------------"
cd $CURRENT
# we now need to convert static libraries to shared libraries
#first create a templib folder
#static2shared $CLASP/build/jna/libclasp/lib/libclasp.a
#static2shared $CLASP/build/jna/libprogram_opts/lib/libprogram_opts.a
compileShared $CLASP/build/jna/libclasp
compileShared $CLASP/build/jna/libprogram_opts

cd $CURRENT
#use the FLAGS that were set in the clasp configure.sh
cp $CLASP/build/jna/.CONFIG ./CLASP_CONFIG
echo "PROJECT_ROOT := $CLASP" > PROJECT_ROOT

make




#make clean



rm CLASP_CONFIG
rm PROJECT_ROOT
echo "---------------------------------------------------"
