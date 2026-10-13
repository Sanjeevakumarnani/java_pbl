@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if not exist out mkdir out
if exist sources.txt del sources.txt
for /r src\main\java %%f in (*.java) do (set "source=%%f" & echo !source:%CD%\=!)>>sources.txt
javac --release 17 -Xlint:all -d out @sources.txt
if errorlevel 1 (pause & exit /b 1)
echo Build passed. Run run.bat to launch the GUI.
