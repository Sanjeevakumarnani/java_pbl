#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
find src/main/java -name '*.java' -print > sources.txt
javac --release 17 -Xlint:all -d out @sources.txt
echo "Build passed. Run ./run.sh to download SQLite JDBC (if needed) and launch the GUI."
