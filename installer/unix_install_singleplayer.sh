#!/bin/bash

# Some colors
RED='\033[0;31m'
NOCOLOR='\033[0m'


if [ ! -d ~/.minecraft/versions/1.13.2 ]; then
	printf "${RED}To patch client, you need to run 1.13.2 game at least once."
	exit 1
fi

echo "Duplicating"

if [ -d ~/.minecraft/versions/1.13.2-carpet ]; then
	echo "Cleaning previous carpet installation ..."
	rm -rf ~/.minecraft/versions/1.13.2-carpet > /dev/null
fi

mkdir ~/.minecraft/versions/1.13.2-carpet > /dev/null
cp ~/.minecraft/versions/1.13.2/1.13.2.jar ~/.minecraft/versions/1.13.2-carpet/1.13.2-carpet.jar > /dev/null
cp 1.13.2-carpet.json ~/.minecraft/versions/1.13.2-carpet/ > /dev/null

echo "Extracting patches"
mkdir ____patches > /dev/null
unzip carpetmod_1.13.2_Client.zip -d ____patches > /dev/null

echo "Patching Client"
pushd ____patches > /dev/null
zip -ur ~/.minecraft/versions/1.13.2-carpet/1.13.2-carpet.jar * > /dev/null
zip -d ~/.minecraft/versions/1.13.2-carpet/1.13.2-carpet.jar META-INF/* > /dev/null
popd > /dev/null

echo "Cleanup"
rm -rf ____patches

echo "Done"

