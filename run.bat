@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if not exist lib mkdir lib
if not exist out mkdir out
if not exist data mkdir data
if not exist exports mkdir exports
if not exist logs mkdir logs
if not exist backups mkdir backups
if not exist lib\sqlite-jdbc-3.50.3.0.jar (
  echo Downloading SQLite JDBC driver for first-time setup...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.50.3.0/sqlite-jdbc-3.50.3.0.jar' -OutFile 'lib\sqlite-jdbc-3.50.3.0.jar'"
  if errorlevel 1 (echo Download failed. Check internet access. & pause & exit /b 1)
)
if not exist lib\slf4j-api-2.0.17.jar (powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar' -OutFile 'lib\slf4j-api-2.0.17.jar'" & if errorlevel 1 (echo SLF4J download failed. & pause & exit /b 1))
if not exist lib\slf4j-nop-2.0.17.jar (powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/slf4j/slf4j-nop/2.0.17/slf4j-nop-2.0.17.jar' -OutFile 'lib\slf4j-nop-2.0.17.jar'" & if errorlevel 1 (echo SLF4J download failed. & pause & exit /b 1))
if exist sources.txt del sources.txt
for /r src\main\java %%f in (*.java) do (set "source=%%f" & echo !source:%CD%\=!)>>sources.txt
javac --release 17 -Xlint:all -cp "lib\sqlite-jdbc-3.50.3.0.jar" -d out @sources.txt
if errorlevel 1 (pause & exit /b 1)
java -cp "out;lib\sqlite-jdbc-3.50.3.0.jar;lib\slf4j-api-2.0.17.jar;lib\slf4j-nop-2.0.17.jar" crfms.Main %*
