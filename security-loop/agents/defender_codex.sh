#!/usr/bin/env bash
set -euo pipefail

MODULE="${MODULE:-core-sdk}"
IN_REPORT="${IN_REPORT:-/tmp/attacker-codex.json}"
OUT_FILE="${OUT_FILE:-/tmp/defender-codex.json}"

if ! command -v jq >/dev/null 2>&1; then
  echo "defender_codex: jq is required" >&2
  exit 1
fi

if [ ! -f "$IN_REPORT" ]; then
  echo "defender_codex: input report not found: $IN_REPORT" >&2
  exit 1
fi

finding_id="$(jq -r '.finding_id // "CODEX-ATTACKER-PLACEHOLDER-001"' "$IN_REPORT")"
severity="$(jq -r '.severity // "info"' "$IN_REPORT")"

jq -cn \
  --arg finding_id "$finding_id" \
  --arg module "$MODULE" \
  --arg severity "$severity" \
  --arg fix_strategy "Convert codex placeholder into concrete remediation after local attacker evidence is reviewed." \
  --argjson patch_scope '["security-loop/agents","docs/agents"]' \
  --argjson validation_steps '["attach attacker evidence to PR", "re-run security-loop/run.sh --mode local"]' \
  --arg residual_risk "Placeholder output does not provide exploit-grade evidence." \
  '{
    finding_id: $finding_id,
    module: $module,
    severity: $severity,
    fix_strategy: $fix_strategy,
    patch_scope: $patch_scope,
    validation_steps: $validation_steps,
    residual_risk: $residual_risk
  }' >"$OUT_FILE"

echo "defender_codex: wrote $OUT_FILE"
