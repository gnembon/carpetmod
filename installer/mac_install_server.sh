#!/bin/bash
echo "Locating server ..."
if [ ! -f server*.jar ]
then
    echo "... cannot locate server jar, make sure its placed in current directory"
    echo "and its named like: server<....>.jar" 
    exit 1
fi
for i in server*.jar; do cp "$i" ____server.jar;done
echo "Extracting patches ..."
mkdir ____patches
unzip -o -q carpetmod_1.13.2_Server.zip -d ____patches
echo "Patching server ..."
cd ____patches
zip -r -q -X ../____server.jar  ./*
cd ..
echo "Cleanup ..."
rm -rf ./____patches
mv -f ____server.jar minecraft_server.1.13.2_carpet.jar
echo "Done"