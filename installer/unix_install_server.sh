#!/bin/bash

# For debug: Print commands and their arguments as they are executed.
# set -x

# Some colors for bash
RED='\033[0;31m'
NOCOLOR='\033[0m'

echo "Location server..."
if [[ ! -f server.jar ]]; then
     printf "${RED}... cannot locate server jar, make sure its placed in current directory\n"
     echo "    and its named like: server.jar"
     exit 1
fi

cp server.jar ____server.jar
printf "${NOCOLOR}Extracting patches ..."
unzip carpetmod_1.13.2_Server.zip -d ____patches
echo "Patching server ..."
pushd ____patches > /dev/null
zip -ur  ../____server.jar *
popd > /dev/null
echo "Cleanup ..."
rm -rf ____patches
mv ____server.jar minecraft_server.1.13.2_carpet.jar
echo "Done"
