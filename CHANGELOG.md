# Changelog

All notable changes to this project are documented in this file.
The format follows Keep a Changelog style with project-specific integration notes.

## [Unreleased]
### Added
- Two-stage security loop framework (`security-loop/run.sh`, attacker/defender agents, JSON schemas).
- Security runbook for two-stage operation (`docs/agents/security-two-stage-runbook.md`).
- Maven Central publish workflow (`.github/workflows/publish-central.yml`).

### Changed
- CI `security` job now runs security-loop and uploads JSON artifacts.
- `risk:medium` findings are tracked through automated issue workflow and PM/Orchestrator cadence rules.
- Gradle publish config now includes signing, sources/javadoc jars, and Central-ready POM metadata.

### Fixed
- None.

### Security
- Release publish pipeline enforces quality gates + high-threshold security-loop before publish.

### Integration Notes
- Security reports are now emitted as CI artifacts for integration-facing audit evidence.

### Breaking Changes
- None.

### Upgrade Notes
- None.

## [v0.1.0] - 2026-03-05

### Added
- Multi-agent governance assets (PM/Orchestrator/Test/Security runbooks)
- Core SDK pipeline with pluggable contracts and candidate-evaluation trace
- Policy controls, compatibility loader reporting, and updater replay-ledger persistence
- Formal release automation script (`scripts/release.sh`)
- App integration lane assets:
  - artifact build script (`scripts/build_artifact.sh`)
  - consumer verification script (`scripts/verify_consumer.sh`)
  - quick start guide (`docs/quick-start.md`)
  - minimal wrapper sample (`examples/minimal-wrapper/...`)

### Changed
- CI split into contract/consistency/compatibility/policy/security lanes
- Security audit hardening for no-transfer and log minimization enforcement
- Release operation standardized with rehearsal and formal runbooks
- CI incident triage runbook added for outage vs quota diagnosis (`docs/agents/ci-incident-runbook.md`)

### Fixed
- None.

### Security
- Security gate explicitly validates integration assets for:
  - no network transfer defaults
  - no sensitive payload logging examples

### Integration Notes
- First stable integration baseline for external apps is now available:
  - group: `com.geo.sdk`
  - artifact: `geo-event-trigger-core-sdk`
  - version: `0.1.0`
- Recommended onboarding entrypoint:
  1. Build artifact (`scripts/build_artifact.sh 0.1.0`)
  2. Follow quick-start (`docs/quick-start.md`)
  3. Validate consumer (`scripts/verify_consumer.sh 0.1.0`)

### Breaking Changes
- None.

### Upgrade Notes
- First stable release. No migration required.

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

### Breaking Changes
- None.

### Notes
- `v0.1.0-rc1` is rehearsal-only and intentionally not retained as a remote tag.
