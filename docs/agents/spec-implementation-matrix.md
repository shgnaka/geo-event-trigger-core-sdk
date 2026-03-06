# Spec-Implementation Matrix

## Core SDK Spec Alignment

| Spec Item | Status | Evidence |
|---|---|---|
| 6 core contracts fixed | Implemented | `CoreSdk.requiredContracts()`, `SdkTestMain.testContractSignaturesPublished` |
| Pipeline multi-candidate evaluation and best selection | Implemented | `PipelineEngine.run`, `testPipelineSelectsBestCandidate` |
| Candidate-level failure isolation | Implemented | `PipelineEngine.run`, `testPipelineContinuesAfterCandidateFailure` |
| Policy boundary controls (threshold/daily/question/cooldown) | Implemented | `BudgetPolicyEngine`, `testPolicyBoundaries` |
| Policy forced controls and invalid input fail-safe | Implemented | `BudgetPolicyEngine`, `testPolicyForceAndValidationControls` |
| Updater apply/undo and reproducibility checkpoints | Implemented | `InMemoryUpdater`, `testUpdaterUndoAndDeterminism` |
| Replay dedup persistence across restarts | Implemented | `FileReplayLedger`, `testUpdaterReplayLedgerPersistsAcrossInstances` |
| Compatibility loading + migration report | Implemented | `CompatibilityLoader.loadWithReport`, `testCompatibilityLoad` |
| Downgrade rejection and unsupported version failure | Implemented | `CompatibilityLoader`, `testCompatibilityRejectsUnsupportedOrDowngrade` |
| No external transfer / log minimization runtime gate | Implemented | `ci/run-security-audit.sh`, `docs/agents/security-audit-controls.md` |
| Two-stage attacker/defender security loop with high+ gate and medium tracking | Implemented | `security-loop/run.sh`, `.github/workflows/ci.yml`, `docs/agents/security-two-stage-runbook.md` |
| Formal release operation (`v0.1.0`) | Implemented | `scripts/release.sh`, release `v0.1.0` |
| Android wrapper implementation | Deferred | out-of-scope for core SDK repo |

## Deferred Items
- Android wrapper/app integration.
- Domain-specific UI flow and copy.
