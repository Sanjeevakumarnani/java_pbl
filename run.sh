#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p lib out data exports logs backups
JAR="lib/sqlite-jdbc-3.50.3.0.jar"
SLF="lib/slf4j-api-2.0.17.jar"
NOP="lib/slf4j-nop-2.0.17.jar"
if [ ! -f "$JAR" ]; then
  echo "Downloading SQLite JDBC driver (one-time setup)..."
  curl -fL --retry 2 "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.50.3.0/sqlite-jdbc-3.50.3.0.jar" -o "$JAR"
fi
if [ ! -f "$SLF" ]; then curl -fL --retry 2 "https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar" -o "$SLF"; fi
if [ ! -f "$NOP" ]; then curl -fL --retry 2 "https://repo.maven.apache.org/maven2/org/slf4j/slf4j-nop/2.0.17/slf4j-nop-2.0.17.jar" -o "$NOP"; fi
find src/main/java -name '*.java' -print > sources.txt
javac --release 17 -Xlint:all -d out @sources.txt
java -cp "out:$JAR:$SLF:$NOP" crfms.Main "$@"
