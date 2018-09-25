#!/bin/bash

git clone -b FG_3.0 https://github.com/MinecraftForge/ForgeGradle
cd ForgeGradle
git checkout 7be40d2
. ./gradlew publishToMavenLocal --no-daemon
cd ..
rm -rf ForgeGradle