#!/usr/bin/env bash
set -euo pipefail

MODULE="core-sdk"
ITERATIONS=1
MODE="ci"
OUT_DIR="out/security-loop"
GATE_THRESHOLD="${GATE_THRESHOLD:-high}"

usage() {
  cat <<EOF
Usage: $0 --module <name> [--iterations <n>] [--mode <ci|local>] [--out-dir <path>] [--gate-threshold <critical|high|medium|never>]
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --module)
      MODULE="${2:-}"
      shift 2
      ;;
    --iterations)
      ITERATIONS="${2:-1}"
      shift 2
      ;;
    --mode)
      MODE="${2:-ci}"
      shift 2
      ;;
    --out-dir)
      OUT_DIR="${2:-out/security-loop}"
      shift 2
      ;;
    --gate-threshold)
      GATE_THRESHOLD="${2:-high}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [ -z "$MODULE" ]; then
  echo "module must be non-empty" >&2
  exit 1
fi

if ! [[ "$ITERATIONS" =~ ^[0-9]+$ ]]; then
  echo "iterations must be numeric" >&2
  exit 1
fi

if [ "$ITERATIONS" -lt 1 ]; then
  echo "iterations must be >= 1" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "security-loop: jq is required" >&2
  exit 1
fi

if [ "$MODE" != "ci" ] && [ "$MODE" != "local" ]; then
  echo "mode must be ci or local" >&2
  exit 1
fi

case "$GATE_THRESHOLD" in
  critical|high|medium|never)
    ;;
  *)
    echo "gate-threshold must be one of critical|high|medium|never" >&2
    exit 1
    ;;
esac

mkdir -p "$OUT_DIR"

attacker_file="$OUT_DIR/attacker.report.json"
defender_file="$OUT_DIR/defender.report.json"
summary_file="$OUT_DIR/summary.json"
codex_finding_file="$OUT_DIR/attacker.codex.finding.json"
attacker_input_for_defender="$attacker_file"

echo "security-loop: module=$MODULE mode=$MODE iterations=$ITERATIONS gate=$GATE_THRESHOLD out=$OUT_DIR"

MODULE="$MODULE" OUT_FILE="$attacker_file" security-loop/agents/attacker_static.sh

if [ "$MODE" = "local" ] && [ "${ENABLE_CODEX_ATTACKER:-0}" = "1" ]; then
  MODULE="$MODULE" OUT_FILE="$codex_finding_file" security-loop/agents/attacker_codex.sh
  jq -s \
    --arg module "$MODULE" \
    --arg generated_at "$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
    '{
      schema_version: "1.0",
      module: $module,
      generated_at: $generated_at,
      findings: ((.[0].findings // []) + [.[1]])
    }' "$attacker_file" "$codex_finding_file" >"$OUT_DIR/attacker.merged.json"
  mv "$OUT_DIR/attacker.merged.json" "$attacker_file"
fi

MODULE="$MODULE" IN_REPORT="$attacker_input_for_defender" OUT_FILE="$defender_file" security-loop/agents/defender_static.sh

critical_count="$(jq '[.findings[]? | select(.severity=="critical")] | length' "$attacker_file")"
high_count="$(jq '[.findings[]? | select(.severity=="high")] | length' "$attacker_file")"
medium_count="$(jq '[.findings[]? | select(.severity=="medium")] | length' "$attacker_file")"
low_count="$(jq '[.findings[]? | select(.severity=="low")] | length' "$attacker_file")"
info_count="$(jq '[.findings[]? | select(.severity=="info")] | length' "$attacker_file")"
total_count="$(jq '[.findings[]?] | length' "$attacker_file")"

max_severity="none"
if [ "$critical_count" -gt 0 ]; then
  max_severity="critical"
elif [ "$high_count" -gt 0 ]; then
  max_severity="high"
elif [ "$medium_count" -gt 0 ]; then
  max_severity="medium"
elif [ "$low_count" -gt 0 ]; then
  max_severity="low"
elif [ "$info_count" -gt 0 ]; then
  max_severity="info"
fi

gate_fail="false"
case "$GATE_THRESHOLD" in
  critical)
    [ "$critical_count" -gt 0 ] && gate_fail="true"
    ;;
  high)
    if [ "$critical_count" -gt 0 ] || [ "$high_count" -gt 0 ]; then
      gate_fail="true"
    fi
    ;;
  medium)
    if [ "$critical_count" -gt 0 ] || [ "$high_count" -gt 0 ] || [ "$medium_count" -gt 0 ]; then
      gate_fail="true"
    fi
    ;;
  never)
    gate_fail="false"
    ;;
esac

jq -cn \
  --arg module "$MODULE" \
  --arg generated_at "$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
  --arg mode "$MODE" \
  --arg gate_threshold "$GATE_THRESHOLD" \
  --arg max_severity "$max_severity" \
  --argjson critical "$critical_count" \
  --argjson high "$high_count" \
  --argjson medium "$medium_count" \
  --argjson low "$low_count" \
  --argjson info "$info_count" \
  --argjson total "$total_count" \
  --argjson gate_fail "$gate_fail" \
  '{
    schema_version: "1.0",
    module: $module,
    generated_at: $generated_at,
    mode: $mode,
    gate_threshold: $gate_threshold,
    counts: {
      critical: $critical,
      high: $high,
      medium: $medium,
      low: $low,
      info: $info,
      total: $total
    },
    max_severity: $max_severity,
    gate_fail: $gate_fail
  }' >"$summary_file"

echo "security-loop: summary $(jq -c . "$summary_file")"
echo "security-loop: attacker report -> $attacker_file"
echo "security-loop: defender report -> $defender_file"
echo "security-loop: summary report  -> $summary_file"

if [ "$gate_fail" = "true" ]; then
  echo "security-loop: gate failed at threshold=$GATE_THRESHOLD"
  exit 2
fi

echo "security-loop: gate passed"
