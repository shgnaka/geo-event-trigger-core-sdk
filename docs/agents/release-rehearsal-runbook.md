# Release Rehearsal Runbook

## Purpose
M1未完了項目 `release-tag rehearsal` を、本番同等の操作で検証する。

## Target
- Temporary tag: `v0.1.0-rc1`
- Repository: `shgnaka/geo-event-trigger-core-sdk`

## Preconditions
- Current branch is `main`
- Working tree is clean
- GitHub CLI is authenticated
- Required checks pass locally:
  - `lint`
  - `unit`
  - `consistency`
  - `contract`
  - `compatibility`
  - `policy`
  - `security`

## Procedure
1. Ensure clean main
2. Run all release checks
3. Create annotated temporary tag and push to origin
4. Create draft prerelease (optional but recommended)
5. Verify artifact visibility
6. Delete draft prerelease and remove temporary tag from remote/local

## Command
```bash
scripts/release_rehearsal.sh v0.1.0-rc1 shgnaka/geo-event-trigger-core-sdk
```

## Rehearsal Result (2026-03-05)
- Branch/clean check: passed
- Local quality gates: passed
- Temporary tag creation/push: passed
- Draft prerelease creation: passed
- Cleanup (release delete + remote/local tag delete): passed
- Residual temporary artifacts: none

## Notes
- This is a rehearsal only. Do not retain `v0.1.0-rc1` tag.
- For official release, use stable tag `vX.Y.Z` and keep artifacts.
