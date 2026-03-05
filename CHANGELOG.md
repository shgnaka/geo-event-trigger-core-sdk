# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

## [v0.1.0] - 2026-03-05

### Added
- Multi-agent governance assets (PM/Orchestrator/Test/Security runbooks)
- Core SDK pipeline with pluggable contracts and candidate-evaluation trace
- Policy controls, compatibility loader reporting, and updater replay-ledger persistence
- Formal release automation script (`scripts/release.sh`)

### Changed
- CI split into contract/consistency/compatibility/policy/security lanes
- Security audit hardening for no-transfer and log minimization enforcement
- Release operation standardized with rehearsal and formal runbooks

### Notes
- First formal public release from `main`.

## [v0.1.0-rc1] - 2026-03-05 (rehearsal)

### Added
- Multi-agent governance assets (PM/Orchestrator/Test/Security runbooks)
- Core SDK pipeline skeleton with pluggable contracts
- Policy controls and compatibility loader hardening
- Security audit controls and replay-ledger persistence for updater feedback dedup

### Changed
- CI split into contract/consistency/compatibility/policy/security lanes
- Release rehearsal automation script and runbook introduced

### Notes
- `v0.1.0-rc1` is rehearsal-only and intentionally not retained as a remote tag.
