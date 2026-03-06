#!/usr/bin/env bash
set -euo pipefail

# This script exists to provide a stable entrypoint for local codex-assisted checks.
# It intentionally keeps setup minimal and avoids exporting secrets.

if ! command -v jq >/dev/null 2>&1; then
  echo "use-codex-cli: jq is required" >&2
  exit 1
fi

if [ -z "${CODEX_MODEL:-}" ]; then
  export CODEX_MODEL="gpt-5"
fi

echo "use-codex-cli: CODEX_MODEL=$CODEX_MODEL"
