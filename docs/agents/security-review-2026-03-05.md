# Security Review Report (2026-03-05)

## Scope
- Runtime core source (`src/main/java/com/geo/sdk/core`)
- CI security gate (`ci/run-security-audit.sh`, `.github/workflows/ci.yml`)
- Update integrity path (`InMemoryUpdater`)

## Findings and Actions

### Fixed: Mutable input-map tampering risk
- Severity: Medium (integrity)
- Risk: External caller could mutate input/tag/signal maps after object creation and change behavior implicitly.
- Action: Added defensive copy + unmodifiable map in:
  - `InputEvent.attributes`
  - `Context.tags`
  - `Candidate.signals`
  - `FeatureVector.values`
  - `PersistedModel.weights`
- Verification: `testRecordInputMapsAreDefensivelyCopied`

### Fixed: Feedback-to-trace mismatch poisoning risk
- Severity: Medium (integrity)
- Risk: `Updater.apply` accepted feedback for a different candidate than selected trace candidate.
- Action: Added strict candidate id match validation.
- Verification: `testUpdaterRejectsMismatchedFeedbackCandidate`

## Residual Risk

### Open: Replay protection is process-local only
- Severity: Medium
- Detail: `feedbackId` deduplication is in-memory; restart clears history, allowing replay.
- Recommendation: persist applied feedback ledger with TTL and bounded storage.

## Current Gate Status
- local checks: `lint/unit/consistency/contract/compatibility/policy/security` all green.

