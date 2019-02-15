@echo off
if not exist %appdata%\.minecraft\versions\1.13.2 (
	echo To patch client, you need to run 1.13.2 game at least once
	pause
	goto:eof
)
echo Duplicating...
if exist %appdata%\.minecraft\versions\1.13.2-carpet (
	echo Cleaning previous carpet installation ...
	rd /s /q %appdata%\.minecraft\versions\1.13.2-carpet > nul
)
mkdir %appdata%\.minecraft\versions\1.13.2-carpet > nul
copy %appdata%\.minecraft\versions\1.13.2\1.13.2.jar %appdata%\.minecraft\versions\1.13.2-carpet\1.13.2-carpet.jar > nul
copy 1.13.2-carpet.json %appdata%\.minecraft\versions\1.13.2-carpet > nul
echo Xtracting patches ...
mkdir ____patches
7za x carpetmod_1.13.2_Client.zip -bd -y -o____patches > nul
echo Patching client ...
7za a -y %appdata%\.minecraft\versions\1.13.2-carpet\1.13.2-carpet.jar  .\____patches\* > nul
7za d -y %appdata%\.minecraft\versions\1.13.2-carpet\1.13.2-carpet.jar META-INF > nul
echo Cleaning ...
rd /s /q ____patches > nul
pause
