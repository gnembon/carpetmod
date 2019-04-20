#!/bin/bash

# For debug: Print commands and their arguments as they are executed.
#set -x

# Some colors for bash
RED='\033[0;31m'
NOCOLOR='\033[0m'

if [[ ! -f  ../build/distributions/carpetmod_1.13.2_Client.zip ]]; then
    echo "${RED}Patches missing, create them with /gradlew createRelease"
    exit 1
fi
if [[ ! -f ../build/distributions/carpetmod_1.13.2_Server.zip ]]; then
    echo "${RED}Patches missing, create them with /gradlew createRelease"
    exit 1
fi

[[ -d output-win ]] && rm -rf output-win
mkdir output-win

echo "${NOCOLOR}Copying files ..."
cp ../build/distributions/carpetmod_1.13.2_Client.zip output-win
cp ../build/distributions/carpetmod_1.13.2_Server.zip output-win
cp 7za.exe output-win
cp win_install_server.cmd output-win
cp win_install_singleplayer.cmd output-win
cp README.txt output-win

echo "Zipping ..."
[[ -f carpet_package_win.zip ]] && rm -f carpet_package_win.zip
pushd output-win > /dev/null
zip -r ../carpet_package_win.zip * > /dev/null
popd > /dev/null

echo "Cleaning ..."
rm -rf output-win
