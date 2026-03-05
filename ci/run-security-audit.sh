#!/usr/bin/env bash
set -euo pipefail

# M0 privacy gate:
# - disallow direct network client imports in core runtime
# - disallow obvious raw location logging patterns

if rg -n "import java\\.net\\.|HttpClient|OkHttp|Retrofit|URLConnection" src/main/java; then
  echo "Security audit failed: network client usage found in core runtime."
  exit 1
fi

if rg -n "System\\.out\\.print|logger\\.|log\\(" src/main/java | rg -n "lat|lon|latitude|longitude|location"; then
  echo "Security audit failed: possible raw location log output found."
  exit 1
fi

echo "security-audit: ok"

