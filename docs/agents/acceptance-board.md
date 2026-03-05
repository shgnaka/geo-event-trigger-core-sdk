# Acceptance Board

## Milestone Status
- Current milestone: M2
- State: in-progress
- Last updated: 2026-03-05 (M2 formal release started)

## Gate Checklist

### M0
- [x] Core SDK contract defined
- [x] contract/consistency/compatibility/policy suites in CI
- [x] security gate active and green
- [x] no external transfer path in runtime core

### M1
- [x] #4 pipeline production-grade handling
- [x] #5 policy control hardening
- [x] #6 updater apply/undo + checkpoints
- [x] #7 compatibility loader hardening
- [x] release-tag rehearsal (`v0.1.0-rc1` temporary tag flow validated and cleaned up)

### M2
- [x] formal release automation prepared (`scripts/release.sh`, runbook)
- [ ] `v0.1.0` tag created on origin
- [ ] GitHub Release published (`isDraft=false`, `isPrerelease=false`)
- [ ] release URL recorded

## Blockers
- none

## Open Risks
- none

## Decision Log Pointers
- `docs/agents/orchestrator-decision-log.md`
