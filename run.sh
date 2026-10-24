#!/usr/bin/env bash
# Compile everything, then start the app (or run the tests with: ./run.sh test)
set -e
cd "$(dirname "$0")"

mkdir -p out
find src test -name "*.java" -print0 | xargs -0 javac -d out

if [ "$1" = "test" ]; then
    java -cp out cricket.ScoreTests
else
    java -cp out app.Main
fi
