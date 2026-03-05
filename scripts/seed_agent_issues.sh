#!/usr/bin/env bash
set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI is required."
  exit 1
fi

repo="${1:-shgnaka/geo-event-trigger-core-sdk}"

create_issue() {
  local title="$1"
  local labels="$2"
  local body="$3"
  gh issue create --repo "$repo" --title "$title" --label "$labels" --body "$body"
}

create_issue \
  "chore: [agent:pm] define milestone cadence and acceptance board" \
  "agent:pm,priority:p1,status:ready" \
  "Define M0/M1 acceptance and daily triage + blocker flow."

create_issue \
  "chore: [agent:orchestrator] establish dependency-driven execution lanes" \
  "agent:orchestrator,priority:p1,status:ready" \
  "Operationalize dependency graph, parallel lanes, and stop conditions."

create_issue \
  "feat: [agent:api-contract] define core SDK interfaces and io contracts" \
  "agent:api-contract,area:api,priority:p0,status:ready" \
  "Define 6 interface contracts and contract-test viewpoints."

create_issue \
  "feat: [agent:pipeline] implement core event-to-update pipeline skeleton" \
  "agent:pipeline,priority:p0,status:ready" \
  "Implement normal-path pipeline skeleton and trace/error shape."

create_issue \
  "feat: [agent:policy] implement cooldown and budget policy engine" \
  "agent:policy,priority:p1,status:ready" \
  "Implement cooldown/daily cap/question budget controls."

create_issue \
  "feat: [agent:updater] implement feedback apply and undo rollback" \
  "agent:updater,priority:p1,status:ready" \
  "Implement apply + undo path and reproducibility checkpoints."

create_issue \
  "feat: [agent:compat] implement schema and model version compatibility loader" \
  "agent:compat,priority:p1,status:ready" \
  "Implement schema/model compatibility loader and migration checks."

create_issue \
  "test: [agent:test] implement contract consistency compatibility policy suites" \
  "agent:test,area:test,priority:p0,status:ready" \
  "Implement contract/consistency/compatibility/policy test suites in CI."

create_issue \
  "security: [agent:security-audit] enforce no-external-transfer and log minimization" \
  "agent:security-audit,area:security,risk:high,priority:p0,status:ready" \
  "Audit no-external-transfer guarantees and log minimization constraints."

echo "Seeded 9 agent issues in $repo"

