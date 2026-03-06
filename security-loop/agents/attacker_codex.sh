#!/usr/bin/env bash
set -euo pipefail

MODULE="${MODULE:-core-sdk}"
OUT_FILE="${OUT_FILE:-/tmp/attacker-codex.json}"

if [ -z "${CODEX_MODEL:-}" ]; then
  CODEX_MODEL="gpt-5"
fi

# Minimal deterministic shape for downstream validation.
jq -cn \
  --arg finding_id "CODEX-ATTACKER-PLACEHOLDER-001" \
  --arg module "$MODULE" \
  --arg severity "info" \
  --arg category "manual-codex-review" \
  --arg evidence "Codex attacker integration placeholder output." \
  --arg attack_path "Run local codex-assisted attacker review and replace this placeholder with concrete finding." \
  --argjson confidence "0.4" \
  --arg model "$CODEX_MODEL" \
  '{
    finding_id: $finding_id,
    module: $module,
    severity: $severity,
    category: $category,
    evidence: [$evidence],
    attack_path: $attack_path,
    confidence: $confidence,
    model: $model
  }' >"$OUT_FILE"

echo "attacker_codex: wrote $OUT_FILE"
