package com.geo.sdk.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.geo.sdk.core.CoreSdk.*;

public final class SdkTestMain {
    private SdkTestMain() {}

    public static void main(String[] args) {
        String suite = args.length == 0 ? "all" : args[0];
        switch (suite) {
            case "unit" -> runUnit();
            case "contract" -> runContract();
            case "consistency" -> runConsistency();
            case "compatibility" -> runCompatibility();
            case "policy" -> runPolicy();
            case "all" -> {
                runUnit();
                runContract();
                runConsistency();
                runCompatibility();
                runPolicy();
            }
            default -> throw new IllegalArgumentException("unknown suite: " + suite);
        }
        System.out.println("OK: " + suite);
    }

    private static void runUnit() {
        testPipelineHappyPath();
        testContractSignaturesPublished();
        testPluginSwappable();
    }

    private static void runConsistency() {
        testPipelineHappyPath();
        testPipelineSelectsBestCandidate();
        testPipelineSkipsWhenNoScorableCandidate();
        testPipelineContinuesAfterCandidateFailure();
        testRecordInputMapsAreDefensivelyCopied();
        testUpdaterUndoAndDeterminism();
        testUpdaterIdempotencyAndCheckpointing();
        testUpdaterRejectsMismatchedFeedbackCandidate();
    }

    private static void runCompatibility() {
        testCompatibilityLoad();
        testCompatibilityRejectsUnsupportedOrDowngrade();
    }

    private static void runPolicy() {
        testPolicyBoundaries();
        testPolicyForceAndValidationControls();
    }

    private static void runContract() {
        testContractSignaturesPublished();
        testPluginSwappable();
    }

    private static void testPipelineHappyPath() {
        PipelineEngine engine = CoreSdk.defaultEngine();
        ModelState model = CoreSdk.defaultModel();

        Map<String, String> attrs = new HashMap<>();
        attrs.put("dwell_minutes", "20");
        InputEvent input = new InputEvent("evt-1", 35.0, 139.0, 1000L, attrs);

        Context ctx = new Context(10_000L, "Asia/Tokyo", Map.of("time_band", "night"));
        Budget budget = new Budget(10, 0, 3, 1000L, 0L);

        PipelineOutcome outcome = engine.run(input, ctx, budget, model);
        assertTrue(outcome.actionResult().success(), "action should succeed");
        assertNotNull(outcome.trace().selectedCandidate(), "candidate must exist");
        assertNotNull(outcome.trace().selectedFeatureVector(), "features must exist");
        assertNotNull(outcome.trace().selectedScore(), "score must exist");
        assertEquals("executed", outcome.trace().status(), "status should be executed");
    }

    private static void testPipelineSelectsBestCandidate() {
        Detector detector = (input, ctx) -> List.of(
                new Candidate("c-low", "location", Map.of("presence", 1.0, "dwell", 1.0)),
                new Candidate("c-high", "location", Map.of("presence", 1.0, "dwell", 30.0))
        );
        FeatureExtractor fx = (candidate, ctx) -> new FeatureVector("2", candidate.signals());
        Scorer scorer = (features, model) -> new ScoreResult(
                features.values().getOrDefault("dwell", 0.0),
                model.modelVersion(),
                "trace-score");

        PipelineEngine engine = new PipelineEngine(
                detector,
                fx,
                scorer,
                new BudgetPolicyEngine(0.2),
                new NoopActionExecutor());

        PipelineOutcome outcome = engine.run(
                new InputEvent("evt-best", 35.0, 139.0, 1000L, Map.of()),
                new Context(10_000L, "Asia/Tokyo", Map.of()),
                new Budget(5, 0, 5, 1000L, 0L),
                CoreSdk.defaultModel());

        assertEquals("c-high", outcome.trace().selectedCandidate().candidateId(), "best candidate should be selected");
        assertEquals(2, outcome.trace().evaluations().size(), "both candidates should be evaluated");
        long selectedCount = outcome.trace().evaluations().stream().filter(CandidateEvaluation::selected).count();
        assertEquals(1L, selectedCount, "exactly one candidate should be marked selected");
    }

    private static void testPipelineSkipsWhenNoScorableCandidate() {
        Detector detector = (input, ctx) -> List.of(new Candidate("c1", "location", Map.of("presence", 1.0)));
        FeatureExtractor failingFx = (candidate, ctx) -> { throw new IllegalStateException("bad feature"); };
        Scorer scorer = (features, model) -> new ScoreResult(1.0, model.modelVersion(), "trace");

        PipelineEngine engine = new PipelineEngine(
                detector,
                failingFx,
                scorer,
                new BudgetPolicyEngine(0.2),
                new NoopActionExecutor());

        PipelineOutcome outcome = engine.run(
                new InputEvent("evt-no-score", 0, 0, 1L, Map.of()),
                new Context(2_000L, "UTC", Map.of()),
                new Budget(1, 0, 1, 0L, 0L),
                CoreSdk.defaultModel());

        assertEquals(ActionType.SKIP, outcome.trace().actionPlan().type(), "no-scorable should skip");
        assertEquals("no-scorable-candidate", outcome.trace().actionPlan().reason(), "reason should be explicit");
        assertEquals("skipped", outcome.trace().status(), "status should be skipped");
    }

    private static void testPipelineContinuesAfterCandidateFailure() {
        Detector detector = (input, ctx) -> List.of(
                new Candidate("c-bad", "location", Map.of("presence", 1.0)),
                new Candidate("c-good", "location", Map.of("presence", 1.0, "dwell", 5.0))
        );

        FeatureExtractor fx = (candidate, ctx) -> {
            if ("c-bad".equals(candidate.candidateId())) {
                throw new IllegalArgumentException("bad candidate");
            }
            return new FeatureVector("2", candidate.signals());
        };

        Scorer scorer = (features, model) -> new ScoreResult(
                features.values().getOrDefault("dwell", 0.0),
                model.modelVersion(),
                "trace-continue");

        PipelineEngine engine = new PipelineEngine(
                detector,
                fx,
                scorer,
                new BudgetPolicyEngine(0.2),
                new NoopActionExecutor());

        PipelineOutcome outcome = engine.run(
                new InputEvent("evt-continue", 0, 0, 1L, Map.of()),
                new Context(10_000L, "UTC", Map.of()),
                new Budget(10, 0, 10, 0L, 0L),
                CoreSdk.defaultModel());

        assertEquals("c-good", outcome.trace().selectedCandidate().candidateId(), "pipeline should continue after failure");
        boolean hasFailureMarker = outcome.trace().evaluations().stream()
                .anyMatch(e -> "candidate-processing-error:IllegalArgumentException".equals(e.decisionReason()));
        assertTrue(hasFailureMarker, "candidate failure should be captured in evaluation trace");
    }

    private static void testPolicyBoundaries() {
        Policy policy = new BudgetPolicyEngine(0.2);
        Context ctx = new Context(5_000L, "Asia/Tokyo", Map.of());

        ActionPlan below = policy.decide(new ScoreResult(0.1, "1", "t1"), new Budget(2, 0, 2, 1000L, 0L), ctx);
        assertEquals(ActionType.SILENT, below.type(), "below threshold should be silent");

        ActionPlan dailyExhausted = policy.decide(new ScoreResult(0.9, "1", "t2"), new Budget(1, 1, 2, 1000L, 0L), ctx);
        assertEquals(ActionType.SKIP, dailyExhausted.type(), "daily cap exhausted should skip");
        assertEquals("daily-limit-exhausted", dailyExhausted.reason(), "daily cap reason");

        ActionPlan questionBudgetExhausted = policy.decide(new ScoreResult(0.9, "1", "t2b"), new Budget(3, 1, 0, 1000L, 0L), ctx);
        assertEquals(ActionType.SKIP, questionBudgetExhausted.type(), "question budget exhausted should skip");
        assertEquals("question-budget-exhausted", questionBudgetExhausted.reason(), "question budget reason");

        ActionPlan cooldown = policy.decide(new ScoreResult(0.9, "1", "t3"), new Budget(3, 0, 3, 10_000L, 3_000L), ctx);
        assertEquals(ActionType.SKIP, cooldown.type(), "cooldown should skip ask");
        assertEquals("cooldown-active", cooldown.reason(), "cooldown reason");

        ActionPlan ask = policy.decide(new ScoreResult(0.9, "1", "t4"), new Budget(3, 0, 3, 1000L, 1_000L), ctx);
        assertEquals(ActionType.ASK, ask.type(), "eligible score should ask");
    }

    private static void testPolicyForceAndValidationControls() {
        Policy policy = new BudgetPolicyEngine(0.2);

        Context forcedSilentCtx = new Context(5_000L, "Asia/Tokyo", Map.of("policy_force_silent", "true"));
        ActionPlan forcedSilent = policy.decide(new ScoreResult(1.0, "1", "f1"), new Budget(3, 0, 3, 1000L, 0L), forcedSilentCtx);
        assertEquals(ActionType.SILENT, forcedSilent.type(), "force-silent tag should override ask");
        assertEquals("forced-silent", forcedSilent.reason(), "force-silent reason");

        Context forcedSkipCtx = new Context(5_000L, "Asia/Tokyo", Map.of("policy_force_skip", "true"));
        ActionPlan forcedSkip = policy.decide(new ScoreResult(1.0, "1", "f2"), new Budget(3, 0, 3, 1000L, 0L), forcedSkipCtx);
        assertEquals(ActionType.SKIP, forcedSkip.type(), "force-skip tag should override ask");
        assertEquals("forced-skip", forcedSkip.reason(), "force-skip reason");

        Context normalCtx = new Context(5_000L, "Asia/Tokyo", Map.of());
        ActionPlan invalidBudget = policy.decide(new ScoreResult(1.0, "1", "f3"), new Budget(-1, 0, 3, 1000L, 0L), normalCtx);
        assertEquals(ActionType.SKIP, invalidBudget.type(), "invalid budget should skip");
        assertEquals("invalid-budget", invalidBudget.reason(), "invalid budget reason");

        ActionPlan invalidScore = policy.decide(new ScoreResult(Double.NaN, "1", "f4"), new Budget(3, 0, 3, 1000L, 0L), normalCtx);
        assertEquals(ActionType.SKIP, invalidScore.type(), "invalid score should skip");
        assertEquals("invalid-score", invalidScore.reason(), "invalid score reason");
    }

    private static void testUpdaterUndoAndDeterminism() {
        PipelineEngine engine = CoreSdk.defaultEngine();
        ModelState base = CoreSdk.defaultModel();
        InMemoryUpdater updater = new InMemoryUpdater(0.01);

        InputEvent input = new InputEvent("evt-u", 35.0, 139.0, 1000L, Map.of("dwell_minutes", "10"));
        Context ctx = new Context(8_000L, "Asia/Tokyo", Map.of());
        Budget budget = new Budget(10, 0, 5, 1000L, 0L);

        PipelineOutcome first = engine.run(input, ctx, budget, base);
        ModelState updated = updater.apply(new FeedbackEvent("fb-1", first.trace().selectedCandidate().candidateId(), true, 9_000L), first.trace(), base);

        ModelState rolledBack = updater.undo();
        assertEquals(base.weights(), rolledBack.weights(), "undo should restore exact previous weights");
        assertEquals(base.revision(), rolledBack.revision(), "undo should restore previous revision");

        PipelineOutcome second = engine.run(input, ctx, budget, base);
        assertEquals(first.trace().actionPlan().type(), second.trace().actionPlan().type(), "rerun should be deterministic on action type");

        boolean changedAnyWeight = !updated.weights().equals(base.weights());
        assertTrue(changedAnyWeight, "positive feedback should update weights");
    }

    private static void testUpdaterIdempotencyAndCheckpointing() {
        PipelineEngine engine = CoreSdk.defaultEngine();
        ModelState base = CoreSdk.defaultModel();
        InMemoryUpdater updater = new InMemoryUpdater(0.01, 4);

        InputEvent input = new InputEvent("evt-up2", 35.0, 139.0, 1000L, Map.of("dwell_minutes", "7"));
        Context ctx = new Context(8_000L, "Asia/Tokyo", Map.of());
        Budget budget = new Budget(10, 0, 5, 1000L, 0L);
        PipelineOutcome outcome = engine.run(input, ctx, budget, base);

        FeedbackEvent feedback = new FeedbackEvent("fb-idem", outcome.trace().selectedCandidate().candidateId(), true, 9_000L);
        ModelState once = updater.apply(feedback, outcome.trace(), base);
        ModelState twice = updater.apply(feedback, outcome.trace(), once);

        assertEquals(once.weights(), twice.weights(), "duplicate feedback id should not be applied twice");
        assertEquals(once.revision(), twice.revision(), "duplicate feedback id should not advance revision");

        assertEquals(1, updater.checkpoints().size(), "exactly one checkpoint should be stored");
        InMemoryUpdater.UpdateCheckpoint checkpoint = updater.checkpoints().get(0);
        assertEquals("fb-idem", checkpoint.feedbackId(), "checkpoint should capture feedback id");
        assertEquals(base.revision(), checkpoint.revisionBefore(), "checkpoint revision before");
        assertEquals(once.revision(), checkpoint.revisionAfter(), "checkpoint revision after");
    }

    private static void testUpdaterRejectsMismatchedFeedbackCandidate() {
        PipelineEngine engine = CoreSdk.defaultEngine();
        ModelState base = CoreSdk.defaultModel();
        InMemoryUpdater updater = new InMemoryUpdater(0.01, 4);

        InputEvent input = new InputEvent("evt-up3", 35.0, 139.0, 1000L, Map.of("dwell_minutes", "7"));
        Context ctx = new Context(8_000L, "Asia/Tokyo", Map.of());
        Budget budget = new Budget(10, 0, 5, 1000L, 0L);
        PipelineOutcome outcome = engine.run(input, ctx, budget, base);

        FeedbackEvent feedback = new FeedbackEvent("fb-bad", "another-candidate", true, 9_000L);
        assertThrows(() -> updater.apply(feedback, outcome.trace(), base), "feedback candidate mismatch");
    }

    private static void testRecordInputMapsAreDefensivelyCopied() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("dwell_minutes", "20");
        InputEvent input = new InputEvent("evt-immutable", 0.0, 0.0, 1L, attrs);
        attrs.put("dwell_minutes", "999");
        assertEquals("20", input.attributes().get("dwell_minutes"), "input attributes should be immutable snapshot");
        assertThrows(() -> input.attributes().put("new_key", "x"), "UnsupportedOperationException");

        Map<String, Double> signals = new HashMap<>();
        signals.put("presence", 1.0);
        Candidate candidate = new Candidate("c-immutable", "location", signals);
        signals.put("presence", 9.0);
        assertEquals(1.0, candidate.signals().get("presence"), "candidate signals should be immutable snapshot");
        assertThrows(() -> candidate.signals().put("x", 1.0), "UnsupportedOperationException");
    }

    private static void testCompatibilityLoad() {
        CompatibilityLoader loader = new CompatibilityLoader();
        PersistedModel old = new PersistedModel("1", "1", Map.of("presence", 0.2, "stay", 0.4), 3);

        ModelState migrated = loader.load(old, "2", "2");
        assertEquals("2", migrated.modelVersion(), "model version should migrate");
        assertEquals("2", migrated.featureSchemaVersion(), "schema version should migrate");
        assertTrue(migrated.weights().containsKey("dwell"), "stay should map to dwell");
        assertTrue(migrated.weights().containsKey("night_bonus"), "new features should be backfilled");

        CompatibilityLoader.CompatibilityLoadResult report = loader.loadWithReport(old, "2", "2");
        assertTrue(report.appliedMigrations().contains("schema:1->2:stay-to-dwell"), "report should include stay-to-dwell migration");
        assertTrue(report.appliedMigrations().contains("model:1->2:add-model_migration_bias"), "report should include model migration");
    }

    private static void testCompatibilityRejectsUnsupportedOrDowngrade() {
        CompatibilityLoader loader = new CompatibilityLoader();
        PersistedModel current = new PersistedModel("2", "2", Map.of("presence", 0.2), 3);

        assertThrows(() -> loader.load(current, "1", "2"), "model downgrade is not supported");
        assertThrows(() -> loader.load(current, "2", "1"), "schema downgrade is not supported");
        assertThrows(() -> loader.load(current, "3", "2"), "target model version unsupported");
        assertThrows(() -> loader.load(new PersistedModel("x", "2", Map.of(), 0), "2", "2"), "model version must be integer");
    }

    private static void testContractSignaturesPublished() {
        List<String> contracts = CoreSdk.requiredContracts();
        assertEquals(6, contracts.size(), "six core contracts must be published");
        assertTrue(contracts.get(0).contains("Detector.detect"), "detector contract missing");
        assertTrue(contracts.get(5).contains("ActionExecutor.execute"), "action executor contract missing");
    }

    private static void testPluginSwappable() {
        Detector detector = (input, ctx) -> List.of(new Candidate("c-custom", "custom", Map.of("presence", 1.0)));
        FeatureExtractor fx = (candidate, ctx) -> new FeatureVector("2", Map.of("presence", 1.0));
        Scorer scorer = (features, model) -> new ScoreResult(1.0, model.modelVersion(), "trace-custom");
        Policy policy = (score, budget, ctx) -> new ActionPlan(ActionType.ASK, "custom-policy", ctx.nowEpochMs());
        ActionExecutor executor = plan -> new ActionResult(true, "custom-exec");

        PipelineEngine engine = new PipelineEngine(detector, fx, scorer, policy, executor);
        PipelineOutcome out = engine.run(
                new InputEvent("evt-c", 0.0, 0.0, 1L, Map.of()),
                new Context(1_000L, "UTC", Map.of()),
                new Budget(1, 0, 1, 0L, 0L),
                CoreSdk.defaultModel());

        assertEquals(ActionType.ASK, out.trace().actionPlan().type(), "swapped policy should control decision");
        assertEquals("custom-policy", out.trace().actionPlan().reason(), "custom reason should be preserved");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    private static void assertThrows(Runnable runnable, String expectedMessagePart) {
        try {
            runnable.run();
            throw new AssertionError("Expected exception containing: " + expectedMessagePart);
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            String className = ex.getClass().getSimpleName();
            if (!message.contains(expectedMessagePart) && !className.contains(expectedMessagePart)) {
                throw new AssertionError("Unexpected exception message: " + ex.getMessage());
            }
        }
    }
}
