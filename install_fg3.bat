git clone -b FG_3.0 https://github.com/MinecraftForge/ForgeGradle
cd ForgeGradle
git checkout 7be40d2
call gradlew publishToMavenLocal --no-daemon
cd ..
rmdir ForgeGradle /S /Q