# Security Two-Stage Runbook

## Purpose
Run attacker-view detection and defender-view remediation planning in sequence with structured outputs.

## Execution Modes
- CI (default):
  - `security-loop/run.sh --module core-sdk --mode ci --iterations 1 --out-dir out/security-loop-ci --gate-threshold high`
  - Static attacker/defender only.
- Local (optional codex-assisted):
  - `ENABLE_CODEX_ATTACKER=1 security-loop/run.sh --module core-sdk --mode local --iterations 1 --out-dir out/security-loop-local --gate-threshold high`

## Output Artifacts
- `attacker.report.json`
- `defender.report.json`
- `summary.json`

## Gate Rule
- `high` threshold:
  - fail on `critical` or `high`
  - pass on `medium`/`low`/`info`

## Medium Workflow
1. CI creates/updates `[security-medium][core-sdk] security-loop findings` issue.
2. PM assigns owner + ETA in daily triage.
3. Orchestrator prioritizes remediation before next sprint start.
