#!/usr/bin/env bash
set -euo pipefail

MODULE="${MODULE:-core-sdk}"
OUT_FILE="${OUT_FILE:-/tmp/attacker-report.json}"

if ! command -v jq >/dev/null 2>&1; then
  echo "attacker_static: jq is required" >&2
  exit 1
fi

search() {
  local pattern="$1"
  shift
  if [ "${FORCE_GREP:-0}" != "1" ] && command -v rg >/dev/null 2>&1; then
    if [ "$#" -gt 0 ]; then
      rg -n "$pattern" "$@"
    else
      rg -n "$pattern"
    fi
  else
    if [ "$#" -gt 0 ]; then
      grep -R -n -E "$pattern" "$@"
    else
      grep -n -E "$pattern"
    fi
  fi
}

tmp_findings="$(mktemp)"
trap 'rm -f "$tmp_findings"' EXIT

add_finding() {
  local finding_id="$1"
  local severity="$2"
  local category="$3"
  local evidence="$4"
  local attack_path="$5"
  local confidence="$6"

  jq -cn \
    --arg finding_id "$finding_id" \
    --arg module "$MODULE" \
    --arg severity "$severity" \
    --arg category "$category" \
    --arg evidence "$evidence" \
    --arg attack_path "$attack_path" \
    --argjson confidence "$confidence" \
    '{
      finding_id: $finding_id,
      module: $module,
      severity: $severity,
      category: $category,
      evidence: [$evidence],
      attack_path: $attack_path,
      confidence: $confidence
    }' >>"$tmp_findings"
}

# 1) Detect unpinned third-party GitHub Actions tag usage.
tmp_unpinned="$(mktemp)"
while IFS= read -r line; do
  use_ref="$(printf "%s" "$line" | sed -E 's/^[^:]+:[0-9]+:[[:space:]]*-?[[:space:]]*uses:[[:space:]]*//; s/^[[:space:]]+//; s/[[:space:]]+$//')"
  if printf "%s" "$use_ref" | grep -qE '^actions/(checkout|upload-artifact|github-script)@'; then
    continue
  fi
  if printf "%s" "$use_ref" | grep -qE '@[0-9a-f]{40}$'; then
    continue
  fi
  printf "%s\n" "$line" >>"$tmp_unpinned"
done < <(search 'uses:[[:space:]]*[^[:space:]]+@[^[:space:]]+' .github/workflows 2>/dev/null || true)

if [ -s "$tmp_unpinned" ]; then
  count="$(wc -l <"$tmp_unpinned" | tr -d ' ')"
  evidence="$(head -n1 "$tmp_unpinned") (and $((count - 1)) more occurrence(s))"
  add_finding \
    "ACT-UNPINNED-001" \
    "medium" \
    "ci-action-integrity" \
    "$evidence" \
    "Mutable action tags can be repointed and execute unexpected code in CI." \
    "0.78"
fi
rm -f "$tmp_unpinned"

# 2) Detect broad write-all permissions.
if search 'permissions:[[:space:]]*write-all' .github/workflows >/tmp/security-loop-write-all.txt 2>/dev/null; then
  evidence="$(head -n1 /tmp/security-loop-write-all.txt)"
  add_finding \
    "PERM-WRITEALL-001" \
    "high" \
    "ci-permissions" \
    "$evidence" \
    "write-all permissions allow broad token abuse if workflow is compromised." \
    "0.92"
fi
rm -f /tmp/security-loop-write-all.txt

# 3) Detect suspicious curl|bash style execution in scripts/workflows.
if search 'curl[^|]*\|\s*(bash|sh)|wget[^|]*\|\s*(bash|sh)' scripts .github/workflows >/tmp/security-loop-curlpipe.txt 2>/dev/null; then
  evidence="$(head -n1 /tmp/security-loop-curlpipe.txt)"
  add_finding \
    "SUPPLY-CURLPIPE-001" \
    "high" \
    "supply-chain-exec" \
    "$evidence" \
    "Remote script execution can inject malicious code at build/publish time." \
    "0.9"
fi
rm -f /tmp/security-loop-curlpipe.txt

# 4) Detect secret-like literals in tracked files (excluding docs).
if git grep -nE '(AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|AIza[0-9A-Za-z_-]{35}|-----BEGIN (RSA|OPENSSH|EC) PRIVATE KEY-----)' -- . ':!docs/*' >/tmp/security-loop-secrets.txt; then
  evidence="$(head -n1 /tmp/security-loop-secrets.txt)"
  add_finding \
    "SECRET-LITERAL-001" \
    "critical" \
    "secret-exposure" \
    "$evidence" \
    "Exposed credentials can enable direct repository/publish compromise." \
    "0.98"
fi
rm -f /tmp/security-loop-secrets.txt

jq -s \
  --arg module "$MODULE" \
  --arg generated_at "$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
  '{
    schema_version: "1.0",
    module: $module,
    generated_at: $generated_at,
    findings: .
  }' "$tmp_findings" >"$OUT_FILE"

echo "attacker_static: wrote $OUT_FILE"
