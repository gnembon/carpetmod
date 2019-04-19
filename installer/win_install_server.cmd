@echo off
echo Locating server ...
if not exist server.jar (
    echo ... cannot locate server jar, make sure its placed in current directory
    echo     and its named like: server.jar
    pause
    goto:eof
)
copy server.jar ____server.jar > nul
echo Extracting patches ...
mkdir ____patches
7za x carpetmod_1.13.2_Server.zip -bd -y -o____patches > nul
echo Patching server ...
7za a -y ____server.jar  .\____patches\* > nul
echo Cleanup ...
rd /s /q ____patches
move /y ____server.jar minecraft_server.1.13.2_carpet.jar > nul
echo Done
pause