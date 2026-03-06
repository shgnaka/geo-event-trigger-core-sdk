# Release Notes Style Guide

## Goal
Provide release notes that help integrators decide whether to upgrade, how to upgrade, and what to verify.

## Required Sections
For each stable release entry in `CHANGELOG.md`, include:
- `Added`
- `Changed`
- `Fixed`
- `Security`
- `Integration Notes`
- `Breaking Changes`
- `Upgrade Notes`

If a section has no updates, write `None.` explicitly.

## Writing Rules
- Write for app integrators, not only SDK maintainers.
- Prefer impact statements over implementation detail.
- Include concrete version coordinates when relevant.
- Include links/paths to onboarding docs or scripts for validation.
- Keep security-related changes separate from generic fixes.

## Minimum Quality Checklist
- Integrator can tell whether migration is needed.
- Integrator can identify what to run for validation.
- Security-relevant changes are visible without reading code diffs.
- Release entry matches GitHub Release title/tag/date.
