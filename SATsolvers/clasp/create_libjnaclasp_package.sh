#!/bin/bash

if [ -d "libjnaclasp-2.1.3" ]
then
	echo "libjnaclasp-2.1.3 directory already exists! Please clean first."
	exit
fi
mkdir libjnaclasp-2.1.3
cd libjnaclasp-2.1.3
echo "#!/bin/bash" > build.sh 
echo "tar -xvf clasp-2.1.3-source.tar.gz" >> build.sh
echo "cd jna" >> build.sh
echo "./build.sh" >> build.sh
echo "mv libjnaclasp.so ../" >> build.sh
echo "cd ../" >> build.sh
echo "rm -Rf clasp-2.1.3" >> build.sh
chmod +x build.sh
cp ../clasp-2.1.3-source.tar.gz ./
cp -r ../jna ./
cd ../
if [ -f "libjnaclasp-2.1.3.tar.gz" ]
then
	rm libjnaclasp-2.1.3.tar.gz
fi
tar -cvzf libjnaclasp-2.1.3.tar.gz libjnaclasp-2.1.3
rm -Rf libjnaclasp-2.1.3

