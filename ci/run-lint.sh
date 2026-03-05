#!/usr/bin/env bash
set -euo pipefail

if [ ! -d src ]; then
  echo "No src directory."
  exit 0
fi

# Basic static checks for M0: tabs, trailing spaces, and TODO markers in production code.
if rg -n "\t" src/main/java; then
  echo "Tabs found in src/main/java"
  exit 1
fi

if rg -n " +$" src/main/java src/test/java; then
  echo "Trailing spaces found"
  exit 1
fi

if rg -n "TODO|FIXME" src/main/java; then
  echo "Unresolved TODO/FIXME found in production code"
  exit 1
fi

echo "lint: ok"
