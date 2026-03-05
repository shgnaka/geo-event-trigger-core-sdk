package com.geo.sdk.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CoreSdk {
    private CoreSdk() {}

    public enum ActionType {
        ASK,
        SILENT,
        SKIP
    }

    public record InputEvent(
            String eventId,
            double latitude,
            double longitude,
            long timestampEpochMs,
            Map<String, String> attributes) {
    }

    public record Context(
            long nowEpochMs,
            String zoneId,
            Map<String, String> tags) {
    }

    public record Candidate(
            String candidateId,
            String type,
            Map<String, Double> signals) {
    }

    public record FeatureVector(
            String featureSchemaVersion,
            Map<String, Double> values) {
    }

    public record ScoreResult(
            double value,
            String modelVersion,
            String traceId) {
    }

    public record Budget(
            int dailyAskLimit,
            int asksUsedToday,
            int questionBudget,
            long cooldownMs,
            long lastAskEpochMs) {
    }

    public record ActionPlan(
            ActionType type,
            String reason,
            long cooldownUntilEpochMs) {
    }

    public record ActionResult(
            boolean success,
            String message) {
    }

    public record FeedbackEvent(
            String candidateId,
            boolean positive,
            long timestampEpochMs) {
    }

    public record ModelState(
            String modelVersion,
            String featureSchemaVersion,
            Map<String, Double> weights,
            long revision) {
        public ModelState {
            weights = Collections.unmodifiableMap(new HashMap<>(weights));
        }

        public double weight(String key) {
            return weights.getOrDefault(key, 0.0d);
        }
    }

    public record ExecutionTrace(
            String traceId,
            Candidate candidate,
            FeatureVector featureVector,
            ScoreResult score,
            ActionPlan actionPlan) {
    }

    public record PipelineOutcome(
            ActionResult actionResult,
            ExecutionTrace trace,
            ModelState modelState) {
    }

    public interface Detector {
        List<Candidate> detect(InputEvent input, Context ctx);
    }

    public interface FeatureExtractor {
        FeatureVector extract(Candidate candidate, Context ctx);
    }

    public interface Scorer {
        ScoreResult score(FeatureVector features, ModelState model);
    }

    public interface Policy {
        ActionPlan decide(ScoreResult score, Budget budget, Context ctx);
    }

    public interface Updater {
        ModelState apply(FeedbackEvent feedback, ExecutionTrace trace, ModelState model);

        ModelState undo();
    }

    public interface ActionExecutor {
        ActionResult execute(ActionPlan plan);
    }

    public static final class PipelineEngine {
        private final Detector detector;
        private final FeatureExtractor featureExtractor;
        private final Scorer scorer;
        private final Policy policy;
        private final ActionExecutor actionExecutor;

        public PipelineEngine(
                Detector detector,
                FeatureExtractor featureExtractor,
                Scorer scorer,
                Policy policy,
                ActionExecutor actionExecutor) {
            this.detector = Objects.requireNonNull(detector);
            this.featureExtractor = Objects.requireNonNull(featureExtractor);
            this.scorer = Objects.requireNonNull(scorer);
            this.policy = Objects.requireNonNull(policy);
            this.actionExecutor = Objects.requireNonNull(actionExecutor);
        }

        public PipelineOutcome run(InputEvent input, Context ctx, Budget budget, ModelState model) {
            List<Candidate> candidates = detector.detect(input, ctx);
            if (candidates.isEmpty()) {
                ActionPlan noAction = new ActionPlan(ActionType.SKIP, "no-candidate", ctx.nowEpochMs());
                ActionResult result = actionExecutor.execute(noAction);
                ExecutionTrace trace = new ExecutionTrace(UUID.randomUUID().toString(), null, null,
                        new ScoreResult(0.0, model.modelVersion(), UUID.randomUUID().toString()), noAction);
                return new PipelineOutcome(result, trace, model);
            }

            Candidate candidate = candidates.get(0);
            FeatureVector featureVector = featureExtractor.extract(candidate, ctx);
            ScoreResult score = scorer.score(featureVector, model);
            ActionPlan plan = policy.decide(score, budget, ctx);
            ActionResult result = actionExecutor.execute(plan);
            ExecutionTrace trace = new ExecutionTrace(score.traceId(), candidate, featureVector, score, plan);
            return new PipelineOutcome(result, trace, model);
        }
    }

    public static final class BasicDetector implements Detector {
        @Override
        public List<Candidate> detect(InputEvent input, Context ctx) {
            Map<String, Double> signals = new HashMap<>();
            signals.put("presence", 1.0);
            signals.put("hour_of_day", (double) ((ctx.nowEpochMs() / 3600000L) % 24));
            String dwell = input.attributes() == null ? "0" : input.attributes().getOrDefault("dwell_minutes", "0");
            try {
                signals.put("dwell", Double.parseDouble(dwell));
            } catch (NumberFormatException ignore) {
                signals.put("dwell", 0.0);
            }
            Candidate candidate = new Candidate(input.eventId() + "-candidate", "location", signals);
            return List.of(candidate);
        }
    }

    public static final class BasicFeatureExtractor implements FeatureExtractor {
        private final String featureSchemaVersion;

        public BasicFeatureExtractor(String featureSchemaVersion) {
            this.featureSchemaVersion = featureSchemaVersion;
        }

        @Override
        public FeatureVector extract(Candidate candidate, Context ctx) {
            Map<String, Double> values = new HashMap<>(candidate.signals());
            if ("night".equalsIgnoreCase(ctx.tags().getOrDefault("time_band", ""))) {
                values.put("night_bonus", 1.0);
            } else {
                values.put("night_bonus", 0.0);
            }
            return new FeatureVector(featureSchemaVersion, values);
        }
    }

    public static final class LinearScorer implements Scorer {
        @Override
        public ScoreResult score(FeatureVector features, ModelState model) {
            double score = 0.0d;
            for (Map.Entry<String, Double> entry : features.values().entrySet()) {
                score += entry.getValue() * model.weight(entry.getKey());
            }
            String traceId = UUID.randomUUID().toString();
            return new ScoreResult(score, model.modelVersion(), traceId);
        }
    }

    public static final class BudgetPolicyEngine implements Policy {
        private final double askThreshold;

        public BudgetPolicyEngine(double askThreshold) {
            this.askThreshold = askThreshold;
        }

        @Override
        public ActionPlan decide(ScoreResult score, Budget budget, Context ctx) {
            if (score.value() < askThreshold) {
                return new ActionPlan(ActionType.SILENT, "below-threshold", ctx.nowEpochMs());
            }
            if (budget.questionBudget() <= 0 || budget.asksUsedToday() >= budget.dailyAskLimit()) {
                return new ActionPlan(ActionType.SKIP, "budget-exhausted", ctx.nowEpochMs());
            }
            long cooldownUntil = budget.lastAskEpochMs() + budget.cooldownMs();
            if (ctx.nowEpochMs() < cooldownUntil) {
                return new ActionPlan(ActionType.SKIP, "cooldown-active", cooldownUntil);
            }
            return new ActionPlan(ActionType.ASK, "eligible", ctx.nowEpochMs() + budget.cooldownMs());
        }
    }

    public static final class NoopActionExecutor implements ActionExecutor {
        @Override
        public ActionResult execute(ActionPlan plan) {
            return new ActionResult(true, "executed:" + plan.type() + ":" + plan.reason());
        }
    }

    public static final class InMemoryUpdater implements Updater {
        private final double learningRate;
        private final Deque<ModelState> history = new ArrayDeque<>();

        public InMemoryUpdater(double learningRate) {
            this.learningRate = learningRate;
        }

        @Override
        public ModelState apply(FeedbackEvent feedback, ExecutionTrace trace, ModelState model) {
            history.push(model);
            if (trace.featureVector() == null) {
                return model;
            }
            Map<String, Double> nextWeights = new HashMap<>(model.weights());
            double direction = feedback.positive() ? 1.0 : -1.0;
            for (Map.Entry<String, Double> feature : trace.featureVector().values().entrySet()) {
                double oldWeight = nextWeights.getOrDefault(feature.getKey(), 0.0d);
                double updated = oldWeight + (direction * feature.getValue() * learningRate);
                nextWeights.put(feature.getKey(), updated);
            }
            return new ModelState(model.modelVersion(), model.featureSchemaVersion(), nextWeights, model.revision() + 1);
        }

        @Override
        public ModelState undo() {
            if (history.isEmpty()) {
                throw new IllegalStateException("no-history");
            }
            return history.pop();
        }
    }

    public record PersistedModel(
            String modelVersion,
            String featureSchemaVersion,
            Map<String, Double> weights,
            long revision) {
    }

    public static final class CompatibilityLoader {
        public ModelState load(PersistedModel persisted, String targetModelVersion, String targetFeatureSchemaVersion) {
            Map<String, Double> migrated = new HashMap<>(persisted.weights());

            if ("1".equals(persisted.featureSchemaVersion()) && "2".equals(targetFeatureSchemaVersion)) {
                if (migrated.containsKey("stay")) {
                    migrated.put("dwell", migrated.get("stay"));
                }
                migrated.putIfAbsent("night_bonus", 0.0d);
            }

            if (!targetFeatureSchemaVersion.equals(persisted.featureSchemaVersion())) {
                migrated.putIfAbsent("schema_migration_bias", 0.0d);
            }

            return new ModelState(targetModelVersion, targetFeatureSchemaVersion, migrated, persisted.revision());
        }
    }

    public static ModelState defaultModel() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("presence", 0.25);
        weights.put("hour_of_day", 0.01);
        weights.put("dwell", 0.04);
        weights.put("night_bonus", 0.3);
        return new ModelState("1", "2", weights, 0);
    }

    public static PipelineEngine defaultEngine() {
        return new PipelineEngine(
                new BasicDetector(),
                new BasicFeatureExtractor("2"),
                new LinearScorer(),
                new BudgetPolicyEngine(0.2),
                new NoopActionExecutor());
    }

    public static List<String> requiredContracts() {
        List<String> names = new ArrayList<>();
        names.add("Detector.detect(input, ctx) -> Candidate[]");
        names.add("FeatureExtractor.extract(candidate, ctx) -> FeatureVector");
        names.add("Scorer.score(features, model) -> ScoreResult");
        names.add("Policy.decide(score, budget, ctx) -> ActionPlan");
        names.add("Updater.apply(feedback, trace, model) -> ModelState");
        names.add("ActionExecutor.execute(plan) -> ActionResult");
        return names;
    }
}
