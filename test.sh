#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
find src/main/java src/test/java -name '*.java' > test-sources.txt
javac --release 17 -Xlint:all -d out @test-sources.txt
java -cp out crfms.SecurityTest
if command -v python3 >/dev/null 2>&1; then python3 tools/validate_schema.py; else echo "SchemaValidation: SKIPPED (Python 3 not installed)."; fi
if [ -f lib/sqlite-jdbc-3.50.3.0.jar ] && [ -f lib/slf4j-api-2.0.17.jar ] && [ -f lib/slf4j-nop-2.0.17.jar ]; then
  java -cp "out:lib/sqlite-jdbc-3.50.3.0.jar:lib/slf4j-api-2.0.17.jar:lib/slf4j-nop-2.0.17.jar" crfms.DatabaseIntegrationTest
else
  echo "ERROR: DatabaseIntegrationTest requires SQLite JDBC and SLF4J runtime dependencies in lib/."
  exit 1
fi
