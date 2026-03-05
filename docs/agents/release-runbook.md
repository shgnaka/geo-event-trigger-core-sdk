# Formal Release Runbook

## Purpose
M2で正式リリース (`vX.Y.Z`) を公開し、タグ・Release・CHANGELOG・受け入れ状態を一致させる。

## Target (current)
- Version: `v0.1.0`
- Repository: `shgnaka/geo-event-trigger-core-sdk`

## Preconditions
- Current branch is `main`
- Working tree is clean
- GitHub CLI authenticated
- Required checks all green:
  - `lint`
  - `unit`
  - `consistency`
  - `contract`
  - `compatibility`
  - `policy`
  - `security`

## Command
```bash
scripts/release.sh v0.1.0 shgnaka/geo-event-trigger-core-sdk
```

## Expected Output
- Annotated tag `v0.1.0` exists on origin
- GitHub Release for `v0.1.0` is published (`isDraft=false`, `isPrerelease=false`)
- Release URL is recorded in acceptance board

## Post Steps
1. Update `docs/agents/acceptance-board.md` M2 checklist
2. Ensure `CHANGELOG.md` has finalized `v0.1.0` notes
3. Confirm open issues/PRs relevant to release are triaged
