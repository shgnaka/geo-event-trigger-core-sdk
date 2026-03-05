#!/usr/bin/env bash
set -euo pipefail

rm -rf out
mkdir -p out
javac -d out $(find src/main/java src/test/java -name "*.java" | tr '\n' ' ')
java -cp out com.geo.sdk.core.SdkTestMain consistency
