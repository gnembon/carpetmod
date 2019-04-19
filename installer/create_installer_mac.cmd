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
if exist output-mac rd /s /q output-mac
mkdir output-mac
echo Copying files ...
copy ..\build\distributions\carpetmod_1.13.2_Client.zip output-mac > nul
copy ..\build\distributions\carpetmod_1.13.2_Server.zip output-mac > nul
copy mac_install_server.cmd output-mac > nul
copy mac_install_singleplayer.cmd output-mac > nul
copy README.txt output-mac > nul
copy 1.13.2-carpet.json output-mac > nul
if exist carpet_package_mac.zip del /q carpet_package_mac.zip
echo Zipping ...
7za a carpet_package_mac.zip .\output-mac\* > nul
echo Cleaning ...
rd /s /q output-mac
pause

