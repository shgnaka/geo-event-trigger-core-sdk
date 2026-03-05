# Git Governance

## Branch Strategy
- Model: trunk-based development
- Permanent branch: `main` only
- Work branches: short-lived
  - `feat/<issue-id>-<scope>`
  - `fix/<issue-id>-<scope>`
  - `chore/<issue-id>-<scope>`

## Issue Policy
- Default: every implementation PR must link an Issue
- Exception: docs-only typo or formatting fixes
- Unit of work: `1 agent responsibility = 1 Issue = 1 PR`

## PR Policy
- Title: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:` ...)
- Merge method: squash merge only
- Required before merge:
  - CI checks green: `lint`, `unit`, `contract`, `security`
  - PR template completed
  - Branch updated with latest `main`
- Single-account operation:
  - Required human approval is disabled
  - Quality gate is enforced by CI + checklist

## Labels
- Taxonomy:
  - `status:*`
  - `priority:*`
  - `risk:*`
  - `area:*`
  - `agent:*`
- Source of truth: `.github/labels.yml`
- Sync workflow: `.github/workflows/sync-labels.yml` (manual trigger)

## Ownership
- `CODEOWNERS` is enabled for future multi-user operation
- Current owner: `@shgnaka`

## Release Policy
- Semantic Versioning tags from `main`: `vX.Y.Z`
- Hotfix path: `hotfix/*` only for urgent production fixes
- Release Issue template: `.github/ISSUE_TEMPLATE/release.yml`

## Recommended GitHub Settings
- Branch protection for `main`:
  - Require pull request before merging
  - Require status checks: `lint`, `unit`, `contract`, `security`
  - Restrict direct pushes
  - Allow squash merge, disable merge commit/rebase merge

