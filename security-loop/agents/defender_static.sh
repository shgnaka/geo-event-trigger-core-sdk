#!/usr/bin/env bash
set -euo pipefail

MODULE="${MODULE:-core-sdk}"
IN_REPORT="${IN_REPORT:-/tmp/attacker-report.json}"
OUT_FILE="${OUT_FILE:-/tmp/defender-report.json}"

if ! command -v jq >/dev/null 2>&1; then
  echo "defender_static: jq is required" >&2
  exit 1
fi

if [ ! -f "$IN_REPORT" ]; then
  echo "defender_static: input report not found: $IN_REPORT" >&2
  exit 1
fi

jq -c '.findings[]?' "$IN_REPORT" | while IFS= read -r finding; do
  finding_id="$(jq -r '.finding_id' <<<"$finding")"
  severity="$(jq -r '.severity' <<<"$finding")"
  category="$(jq -r '.category' <<<"$finding")"

  case "$category" in
    ci-action-integrity)
      fix_strategy="Pin third-party actions to immutable commit SHA and review action upgrades via dedicated PR."
      patch_scope='[".github/workflows"]'
      validation_steps='["rg -n \"uses:\" .github/workflows","verify non-official actions end with 40-hex SHA"]'
      residual_risk="Compromise of an already pinned commit remains possible but less likely."
      ;;
    ci-permissions)
      fix_strategy="Reduce workflow token permissions to least privilege and scope write permissions to specific jobs only."
      patch_scope='[".github/workflows"]'
      validation_steps='["review workflow permissions blocks","re-run CI and confirm no write-all usage"]'
      residual_risk="Legitimate write operations may require explicit narrower grants."
      ;;
    supply-chain-exec)
      fix_strategy="Remove curl|bash execution pattern and replace with pinned checksummed downloads or vendored scripts."
      patch_scope='["scripts",".github/workflows"]'
      validation_steps='["rg -n \"curl.*\\\\|\\\\s*(bash|sh)|wget.*\\\\|\\\\s*(bash|sh)\" scripts .github/workflows"]'
      residual_risk="Future script additions can reintroduce unsafe patterns without policy checks."
      ;;
    secret-exposure)
      fix_strategy="Revoke/rotate exposed credentials immediately and purge secret from history if needed."
      patch_scope='["repository settings","commit history","secret management"]'
      validation_steps='["rotate token/key","git grep secret patterns","re-run security gates"]'
      residual_risk="Previously leaked credentials may already be abused before revocation."
      ;;
    *)
      fix_strategy="Triage finding manually and apply least-privilege, integrity, and logging-minimization controls."
      patch_scope='["manual-triage"]'
      validation_steps='["security review", "re-run security loop"]'
      residual_risk="Unknown until triage is completed."
      ;;
  esac

  jq -cn \
    --arg finding_id "$finding_id" \
    --arg module "$MODULE" \
    --arg severity "$severity" \
    --arg fix_strategy "$fix_strategy" \
    --argjson patch_scope "$patch_scope" \
    --argjson validation_steps "$validation_steps" \
    --arg residual_risk "$residual_risk" \
    '{
      finding_id: $finding_id,
      module: $module,
      severity: $severity,
      fix_strategy: $fix_strategy,
      patch_scope: $patch_scope,
      validation_steps: $validation_steps,
      residual_risk: $residual_risk
    }'
done | jq -s \
  --arg module "$MODULE" \
  --arg generated_at "$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
  '{
    schema_version: "1.0",
    module: $module,
    generated_at: $generated_at,
    remediations: .
  }' >"$OUT_FILE"

echo "defender_static: wrote $OUT_FILE"
