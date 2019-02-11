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
if exist output-win rd /s /q output-win
mkdir output-win
echo Copying files ...
copy ..\build\distributions\carpetmod_1.13.2_Client.zip output-win > nul
copy ..\build\distributions\carpetmod_1.13.2_Server.zip output-win > nul
copy 7za.exe output-win > nul
copy win_install_server.cmd output-win > nul
copy win_install_singleplayer.cmd output-win > nul
copy README.txt output-win > nul
if exist carpet_package_win.zip del /q carpet_package_win.zip
echo Zipping ...
7za a carpet_package_win.zip .\output-win\* > nul
echo Cleaning ...
rd /s /q output-win
pause

