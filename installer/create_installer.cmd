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
if exist output rd /s /q output
mkdir output
echo Copying files ...
copy ..\build\distributions\carpetmod_1.13.2_Client.zip output > nul
copy ..\build\distributions\carpetmod_1.13.2_Server.zip output > nul
copy 7za.exe output > nul
copy win_install_server.cmd output > nul
copy win_install_singleplayer.cmd output > nul
copy unix_install_server.sh output > nul
copy unix_install_singleplayer.sh output > nul
copy README.txt output > nul
if exist carpet_package.zip del /q carpet_package.zip
echo Zipping ...
7za a carpet_package.zip .\output\* > nul
echo Cleaning ...
rd /s /q output
pause

