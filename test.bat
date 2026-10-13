@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if not exist out mkdir out
if exist test-sources.txt del test-sources.txt
for /r src\main\java %%f in (*.java) do (set "source=%%f" & echo !source:%CD%\=!)>>test-sources.txt
for /r src\test\java %%f in (*.java) do (set "source=%%f" & echo !source:%CD%\=!)>>test-sources.txt
javac --release 17 -Xlint:all -d out @test-sources.txt
if errorlevel 1 exit /b 1
java -cp out crfms.SecurityTest
if errorlevel 1 exit /b 1
where python >nul 2>nul && python tools\validate_schema.py
if errorlevel 1 exit /b 1
if exist lib\sqlite-jdbc-3.50.3.0.jar if exist lib\slf4j-api-2.0.17.jar if exist lib\slf4j-nop-2.0.17.jar (
  java -cp "out;lib\sqlite-jdbc-3.50.3.0.jar;lib\slf4j-api-2.0.17.jar;lib\slf4j-nop-2.0.17.jar" crfms.DatabaseIntegrationTest
  exit /b %errorlevel%
)
echo ERROR: DatabaseIntegrationTest requires SQLite JDBC and SLF4J runtime dependencies in lib/.
exit /b 1
