#!/usr/bin/env bash
set -euo pipefail

TARGET_DIR="src/main/java"

if [ ! -d "$TARGET_DIR" ]; then
  echo "security-audit: no runtime source directory, skip"
  exit 0
fi

# 1) No external transfer primitives in core runtime.
NETWORK_PATTERN='import java\\.net\\.|java\\.net\\.(URL|URI|Socket|DatagramSocket|HttpURLConnection)|java\\.nio\\.channels\\.(SocketChannel|AsynchronousSocketChannel)|HttpClient|URLConnection|OkHttp|okhttp3|Retrofit|retrofit2|WebSocket|grpc|mqtt|ApacheHttpClient'
if rg -n "$NETWORK_PATTERN" "$TARGET_DIR"; then
  echo "Security audit failed: network transfer primitives found in runtime core."
  exit 1
fi

# 2) No endpoint literal in runtime core (prevents hidden exfil paths).
if rg -n "https?://" "$TARGET_DIR"; then
  echo "Security audit failed: endpoint literal found in runtime core."
  exit 1
fi

# 3) Log minimization: disallow likely raw location/context payload logging.
LOG_CALL_PATTERN='System\.out\.print(?:ln)?|logger\.|printStackTrace\(|\blog\s*\('
SENSITIVE_PATTERN='lat|lon|latitude|longitude|location|InputEvent|Context|Candidate|FeatureVector|ScoreResult'
if rg -n -P "$LOG_CALL_PATTERN" "$TARGET_DIR" | rg -n "$SENSITIVE_PATTERN"; then
  echo "Security audit failed: possible sensitive runtime payload logging found."
  exit 1
fi

# 4) Integration assets should not introduce network defaults.
if [ -d "examples" ]; then
  if rg -n "http://|https://|import java\\.net\\.|HttpClient|URLConnection|OkHttp|Retrofit|WebSocket" examples; then
    echo "Security audit failed: integration example contains network transfer defaults."
    exit 1
  fi
fi

# 5) Integration docs/examples should avoid raw location payload logging samples.
if rg -n "System\\.out|logger\\.|\\blog\\s*\\(" examples docs/quick-start.md 2>/dev/null | rg -n "lat|lon|latitude|longitude|location|InputEvent|Context"; then
  echo "Security audit failed: integration assets contain sensitive logging examples."
  exit 1
fi

echo "security-audit: ok"
