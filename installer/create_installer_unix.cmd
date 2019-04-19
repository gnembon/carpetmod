@echo off
if not exist ..\build\distributions\carpetmod_1.13.2_Client.zip (
    echo Patches missing, create them with /gradlew createRelease
    pause
    goto:eof
)
if not exist ..\build\distributions\carpetmod_1.13.2_Server.zip (
    echo Patches missing, create them with /gradlew createRelease
    pause
    goto:eof
)
if exist output-ux rd /s /q output-ux
mkdir output-ux
echo Copying files ...
copy ..\build\distributions\carpetmod_1.13.2_Client.zip output-ux > nul
copy ..\build\distributions\carpetmod_1.13.2_Server.zip output-ux > nul
copy unix_install_server.sh output-ux > nul
copy unix_install_singleplayer.sh output-ux > nul
copy README.txt output-ux > nul
copy 1.13.2-carpet.json output-ux > nul
if exist carpet_package_ux.zip del /q carpet_package_ux.zip
echo Zipping ...
7za a carpet_package_ux.zip .\output-ux\* > nul
echo Cleaning ...
rd /s /q output-ux
pause

