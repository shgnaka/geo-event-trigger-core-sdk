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
            case "all" -> {
                runUnit();
                runContract();
            }
            default -> throw new IllegalArgumentException("unknown suite: " + suite);
        }
        System.out.println("OK: " + suite);
    }

    private static void runUnit() {
        testPipelineHappyPath();
        testPolicyBoundaries();
        testUpdaterUndoAndDeterminism();
        testCompatibilityLoad();
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
        assertNotNull(outcome.trace().candidate(), "candidate must exist");
        assertNotNull(outcome.trace().featureVector(), "features must exist");
        assertNotNull(outcome.trace().score(), "score must exist");
    }

    private static void testPolicyBoundaries() {
        Policy policy = new BudgetPolicyEngine(0.2);
        Context ctx = new Context(5_000L, "Asia/Tokyo", Map.of());

        ActionPlan below = policy.decide(new ScoreResult(0.1, "1", "t1"), new Budget(2, 0, 2, 1000L, 0L), ctx);
        assertEquals(ActionType.SILENT, below.type(), "below threshold should be silent");

        ActionPlan budgetExhausted = policy.decide(new ScoreResult(0.9, "1", "t2"), new Budget(1, 1, 0, 1000L, 0L), ctx);
        assertEquals(ActionType.SKIP, budgetExhausted.type(), "budget exhausted should skip");
        assertEquals("budget-exhausted", budgetExhausted.reason(), "budget exhausted reason");

        ActionPlan cooldown = policy.decide(new ScoreResult(0.9, "1", "t3"), new Budget(3, 0, 3, 10_000L, 3_000L), ctx);
        assertEquals(ActionType.SKIP, cooldown.type(), "cooldown should skip ask");
        assertEquals("cooldown-active", cooldown.reason(), "cooldown reason");

        ActionPlan ask = policy.decide(new ScoreResult(0.9, "1", "t4"), new Budget(3, 0, 3, 1000L, 1_000L), ctx);
        assertEquals(ActionType.ASK, ask.type(), "eligible score should ask");
    }

    private static void testUpdaterUndoAndDeterminism() {
        PipelineEngine engine = CoreSdk.defaultEngine();
        ModelState base = CoreSdk.defaultModel();
        InMemoryUpdater updater = new InMemoryUpdater(0.01);

        InputEvent input = new InputEvent("evt-u", 35.0, 139.0, 1000L, Map.of("dwell_minutes", "10"));
        Context ctx = new Context(8_000L, "Asia/Tokyo", Map.of());
        Budget budget = new Budget(10, 0, 5, 1000L, 0L);

        PipelineOutcome first = engine.run(input, ctx, budget, base);
        ModelState updated = updater.apply(new FeedbackEvent(first.trace().candidate().candidateId(), true, 9_000L), first.trace(), base);

        ModelState rolledBack = updater.undo();
        assertEquals(base.weights(), rolledBack.weights(), "undo should restore exact previous weights");
        assertEquals(base.revision(), rolledBack.revision(), "undo should restore previous revision");

        PipelineOutcome second = engine.run(input, ctx, budget, base);
        assertEquals(first.trace().actionPlan().type(), second.trace().actionPlan().type(), "rerun should be deterministic on action type");

        boolean changedAnyWeight = !updated.weights().equals(base.weights());
        assertTrue(changedAnyWeight, "positive feedback should update weights");
    }

    private static void testCompatibilityLoad() {
        CompatibilityLoader loader = new CompatibilityLoader();
        PersistedModel old = new PersistedModel("1", "1", Map.of("presence", 0.2, "stay", 0.4), 3);

        ModelState migrated = loader.load(old, "2", "2");
        assertEquals("2", migrated.modelVersion(), "model version should migrate");
        assertEquals("2", migrated.featureSchemaVersion(), "schema version should migrate");
        assertTrue(migrated.weights().containsKey("dwell"), "stay should map to dwell");
        assertTrue(migrated.weights().containsKey("night_bonus"), "new features should be backfilled");
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
}
