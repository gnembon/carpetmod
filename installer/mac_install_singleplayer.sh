#!/bin/bash
if [ ! -d ~/Library/Application\ Support/minecraft/versions/1.13.2 ]
then
    echo "To patch client, you need to run 1.13.2 game at least once"
    exit 1
fi
echo "Duplicating..."

if [ -d ~/Library/Application\ Support/minecraft/versions/1.13.2-carpet ]
then
	echo "Cleaning previous carpet installation ..."
	rm -rf ~/Library/Application\ Support/minecraft/versions/1.13.2-carpet
fi
mkdir ~/Library/Application\ Support/minecraft/versions/1.13.2-carpet
cp ~/Library/Application\ Support/minecraft/versions/1.13.2/1.13.2.jar ~/Library/Application\ Support/minecraft/versions/1.13.2-carpet/1.13.2-carpet.jar
cp 1.13.2-carpet.json ~/Library/Application\ Support/minecraft/versions/1.13.2-carpet/
echo "Xtracting patches ..."
mkdir ____patches
unzip -o -q carpetmod_1.13.2_Client.zip -d ____patches
echo "Patching client ..."
cd ____patches
zip -r -q -X ~/Library/Application\ Support/minecraft/versions/1.13.2-carpet/1.13.2-carpet.jar  ./*
cd ..
zip -q --delete  ~/Library/Application\ Support/minecraft/versions/1.13.2-carpet/1.13.2-carpet.jar "META-INF/*"
echo "Cleaning ..."
rm -rf ./____patches