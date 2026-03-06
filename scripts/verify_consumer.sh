#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-0.1.0}"
ARTIFACT_PATH="dist/geo-event-trigger-core-sdk-$VERSION.jar"

if [ ! -f "$ARTIFACT_PATH" ]; then
  echo "Artifact not found: $ARTIFACT_PATH"
  echo "Run: scripts/build_artifact.sh $VERSION"
  exit 1
fi

OUT_DIR=".build/consumer"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

javac -cp "$ARTIFACT_PATH" -d "$OUT_DIR" $(find examples/minimal-wrapper/src/main/java -name "*.java" | tr '\n' ' ')
java -cp "$OUT_DIR:$ARTIFACT_PATH" com.geo.sdk.example.MinimalWrapperExample

echo "Consumer verification: ok"
