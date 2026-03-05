# Contributing

## Workflow
1. Create an Issue (unless docs/typo-only change).
2. Create a short-lived branch from `main`.
3. Implement and open a PR using the PR template.
4. Ensure CI checks pass (`lint`, `unit`, `contract`, `security`).
5. Merge with squash merge.

## Naming Rules
- Branch: `feat/<issue-id>-<scope>`, `fix/<issue-id>-<scope>`, `chore/<issue-id>-<scope>`
- PR title: Conventional Commits style

## Quality and Privacy
- Do not add new external transfer paths for location/learning data without explicit design approval.
- Keep logs minimal; avoid storing precise location or personal raw data in logs.
- Never commit secrets.

## Governance References
- [Git governance](docs/git-governance.md)
- [SDK spec](docs/geo-event-trigger-core-sdk-spec.md)

