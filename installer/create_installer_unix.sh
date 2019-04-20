#!/bin/bash

# For debug: Print commands and their arguments as they are executed.
#set -x

# Some colors for bash
RED='\033[0;31m'
NOCOLOR='\033[0m'

if [[ ! -f  ../build/distributions/carpetmod_1.13.2_Client.zip ]]; then
    printf "${RED}Patches missing, create them with /gradlew createRelease"
    exit 1
fi
if [[ ! -f ../build/distributions/carpetmod_1.13.2_Server.zip ]]; then
    printf "${RED}Patches missing, create them with /gradlew createRelease"
    exit 1
fi

[[ -d output-ux ]] && rm -rf output-ux
mkdir output-ux

printf "${NOCOLOR}Copying files ...\n"
cp ../build/distributions/carpetmod_1.13.2_Client.zip output-ux
cp ../build/distributions/carpetmod_1.13.2_Server.zip output-ux
cp unix_install_server.sh output-ux
cp unix_install_singleplayer.sh output-ux
cp README.txt output-ux

echo "Zipping ..."
[[ -f carpet_package_ux.zip ]] && rm -f carpet_package_ux.zip
pushd output-ux > /dev/null
tar czf ../carpet_package_ux.tar.gz *
popd > /dev/null

echo "Cleaning ..."
rm -rf output-ux
