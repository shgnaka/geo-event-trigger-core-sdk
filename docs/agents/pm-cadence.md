# PM Cadence and Acceptance Flow

Issue: #1 (`agent:pm`)

## Cadence
- Daily (15 min): triage + blocker review
- Twice weekly (30 min): milestone health review
- Milestone close: acceptance board sign-off

## Daily Triage Flow
1. Gather open Issues and PR states
2. Tag each item:
   - `ready`
   - `blocked`
   - `in-review`
   - `merged`
3. For `blocked` items record:
   - blocker summary
   - owner
   - ETA
4. Re-order by priority:
   - `security` > `contract` > `compatibility` > `feature` > `docs`

## Blocker Policy
- Blocker must be registered in acceptance board within same day.
- If blocker exceeds SLA:
  - escalate to PM decision log
  - freeze dependent lane until owner and ETA are explicit
- CI障害判定は `docs/agents/ci-incident-runbook.md` に従う

## Acceptance Gates
### M0 Gate
- Core contracts fixed
- Test suites wired to CI (`contract/consistency/compatibility/policy`)
- Security controls active (`security` gate green)

### M1 Gate
- Pipeline/Policy/Updater/Compatibility behavior integrated
- Regression suite stable across two consecutive PRs
- No open `risk:high` issue without mitigation owner

## PM Responsibilities
- Decide milestone scope and cutoff date
- Keep acceptance board current
- Resolve priority conflicts with Orchestrator
- Record acceptance/rejection rationale

## Integration Lane (App Onboarding)
- Keep `docs/integration/integration-lane.md` aligned with current acceptance criteria.
- Track onboarding blockers for artifact/docs/sample/security in daily triage.
- Require integration verification evidence before marking lane `done`.
