# Spec MVP Template

## Metadata
- Title:
- Status: (`draft` | `refine` | `stable`)
- Updated:
- Owner:

## Goal
- What problem this spec solves.

## Core Pipeline
- `Input -> Candidate -> Feature -> Score -> Decision -> Feedback -> Update`

## Public Contracts
- `Detector.detect(input, ctx) -> Candidate[]`
- `FeatureExtractor.extract(candidate, ctx) -> FeatureVector`
- `Scorer.score(features, model) -> ScoreResult`
- `Policy.decide(score, budget, ctx) -> ActionPlan`
- `Updater.apply(feedback, trace, model) -> ModelState`
- `ActionExecutor.execute(plan) -> ActionResult`

## Behavioral Guarantees
- Selection strategy:
- Failure handling:
- Idempotency / replay behavior:
- Version compatibility behavior:

## Security & Privacy Constraints
- External transfer policy:
- Sensitive logging policy:
- Data retention policy:

## Validation Plan
- Contract tests:
- Consistency tests:
- Compatibility tests:
- Policy tests:
- Security tests:

## Implementation Mapping
- Implemented:
- Partial:
- Deferred:

## Change Log
- YYYY-MM-DD:
