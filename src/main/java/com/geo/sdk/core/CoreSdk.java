package com.geo.sdk.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        public InputEvent {
            attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(attributes));
        }
    }

    public record Context(
            long nowEpochMs,
            String zoneId,
            Map<String, String> tags) {
        public Context {
            tags = tags == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(tags));
        }
    }

    public record Candidate(
            String candidateId,
            String type,
            Map<String, Double> signals) {
        public Candidate {
            signals = signals == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(signals));
        }
    }

    public record FeatureVector(
            String featureSchemaVersion,
            Map<String, Double> values) {
        public FeatureVector {
            values = values == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(values));
        }
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
            String feedbackId,
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

    public record CandidateEvaluation(
            Candidate candidate,
            FeatureVector featureVector,
            ScoreResult score,
            boolean selected,
            String decisionReason) {
    }

    public record ExecutionTrace(
            String traceId,
            String status,
            Candidate selectedCandidate,
            FeatureVector selectedFeatureVector,
            ScoreResult selectedScore,
            List<CandidateEvaluation> evaluations,
            ActionPlan actionPlan,
            String failureStage) {
        public ExecutionTrace {
            evaluations = Collections.unmodifiableList(new ArrayList<>(evaluations));
        }
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
        private final int maxCandidatesToEvaluate;

        public PipelineEngine(
                Detector detector,
                FeatureExtractor featureExtractor,
                Scorer scorer,
                Policy policy,
                ActionExecutor actionExecutor) {
            this(detector, featureExtractor, scorer, policy, actionExecutor, 10);
        }

        public PipelineEngine(
                Detector detector,
                FeatureExtractor featureExtractor,
                Scorer scorer,
                Policy policy,
                ActionExecutor actionExecutor,
                int maxCandidatesToEvaluate) {
            this.detector = Objects.requireNonNull(detector);
            this.featureExtractor = Objects.requireNonNull(featureExtractor);
            this.scorer = Objects.requireNonNull(scorer);
            this.policy = Objects.requireNonNull(policy);
            this.actionExecutor = Objects.requireNonNull(actionExecutor);
            if (maxCandidatesToEvaluate <= 0) {
                throw new IllegalArgumentException("maxCandidatesToEvaluate must be > 0");
            }
            this.maxCandidatesToEvaluate = maxCandidatesToEvaluate;
        }

        public PipelineOutcome run(InputEvent input, Context ctx, Budget budget, ModelState model) {
            validateInput(input, ctx, budget, model);
            String traceId = UUID.randomUUID().toString();

            List<Candidate> candidates = detector.detect(input, ctx);
            if (candidates == null || candidates.isEmpty()) {
                return skipOutcome(traceId, "no-candidate", "detector", model, List.of(), ctx.nowEpochMs());
            }

            List<CandidateEvaluation> provisional = new ArrayList<>();
            Candidate bestCandidate = null;
            FeatureVector bestFeatures = null;
            ScoreResult bestScore = null;

            int evaluated = 0;
            for (Candidate candidate : candidates) {
                if (candidate == null) {
                    continue;
                }
                if (evaluated >= maxCandidatesToEvaluate) {
                    provisional.add(new CandidateEvaluation(null, null, null, false, "max-candidates-reached"));
                    break;
                }
                evaluated++;

                try {
                    FeatureVector features = featureExtractor.extract(candidate, ctx);
                    if (features == null) {
                        provisional.add(new CandidateEvaluation(candidate, null, null, false, "feature-null"));
                        continue;
                    }

                    ScoreResult score = scorer.score(features, model);
                    if (score == null) {
                        provisional.add(new CandidateEvaluation(candidate, features, null, false, "score-null"));
                        continue;
                    }

                    provisional.add(new CandidateEvaluation(candidate, features, score, false, "scored"));
                    if (bestScore == null || score.value() > bestScore.value()) {
                        bestCandidate = candidate;
                        bestFeatures = features;
                        bestScore = score;
                    }
                } catch (RuntimeException ex) {
                    provisional.add(new CandidateEvaluation(candidate, null, null, false,
                            "candidate-processing-error:" + ex.getClass().getSimpleName()));
                }
            }

            if (bestCandidate == null || bestFeatures == null || bestScore == null) {
                return skipOutcome(traceId, "no-scorable-candidate", "feature-or-score", model, provisional,
                        ctx.nowEpochMs());
            }

            List<CandidateEvaluation> finalized = markSelected(provisional, bestCandidate);
            ActionPlan plan;
            try {
                plan = policy.decide(bestScore, budget, ctx);
            } catch (RuntimeException ex) {
                return skipOutcome(traceId, "policy-error", "policy", model, finalized, ctx.nowEpochMs());
            }

            ActionResult result;
            try {
                result = actionExecutor.execute(plan);
                if (result == null) {
                    result = new ActionResult(false, "execution-returned-null");
                }
            } catch (RuntimeException ex) {
                result = new ActionResult(false, "execution-error:" + ex.getClass().getSimpleName());
            }

            String status = result.success() ? "executed" : "execution-failed";
            String failureStage = result.success() ? "none" : "action";
            ExecutionTrace trace = new ExecutionTrace(
                    traceId,
                    status,
                    bestCandidate,
                    bestFeatures,
                    bestScore,
                    finalized,
                    plan,
                    failureStage);
            return new PipelineOutcome(result, trace, model);
        }

        private static void validateInput(InputEvent input, Context ctx, Budget budget, ModelState model) {
            Objects.requireNonNull(input, "input must not be null");
            Objects.requireNonNull(ctx, "ctx must not be null");
            Objects.requireNonNull(budget, "budget must not be null");
            Objects.requireNonNull(model, "model must not be null");
            if (input.eventId() == null || input.eventId().isBlank()) {
                throw new IllegalArgumentException("input.eventId must not be blank");
            }
            if (ctx.nowEpochMs() < 0) {
                throw new IllegalArgumentException("ctx.nowEpochMs must be >= 0");
            }
        }

        private static PipelineOutcome skipOutcome(
                String traceId,
                String reason,
                String failureStage,
                ModelState model,
                List<CandidateEvaluation> evaluations,
                long nowEpochMs) {
            ActionPlan noAction = new ActionPlan(ActionType.SKIP, reason, nowEpochMs);
            ActionResult result = new ActionResult(true, "executed:SKIP:" + reason);
            ExecutionTrace trace = new ExecutionTrace(
                    traceId,
                    "skipped",
                    null,
                    null,
                    null,
                    evaluations,
                    noAction,
                    failureStage);
            return new PipelineOutcome(result, trace, model);
        }

        private static List<CandidateEvaluation> markSelected(
                List<CandidateEvaluation> evaluations,
                Candidate selectedCandidate) {
            List<CandidateEvaluation> result = new ArrayList<>(evaluations.size());
            for (CandidateEvaluation evaluation : evaluations) {
                boolean selected = evaluation.candidate() != null
                        && evaluation.candidate().candidateId().equals(selectedCandidate.candidateId());
                String reason = selected ? "selected" : evaluation.decisionReason();
                result.add(new CandidateEvaluation(
                        evaluation.candidate(),
                        evaluation.featureVector(),
                        evaluation.score(),
                        selected,
                        reason));
            }
            return result;
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
        public record PolicyConfig(
                double askThreshold,
                boolean allowContextForceSilent,
                boolean allowContextForceSkip) {
        }

        private final PolicyConfig config;

        public BudgetPolicyEngine(double askThreshold) {
            this(new PolicyConfig(askThreshold, true, true));
        }

        public BudgetPolicyEngine(PolicyConfig config) {
            this.config = Objects.requireNonNull(config);
            if (Double.isNaN(config.askThreshold()) || Double.isInfinite(config.askThreshold())) {
                throw new IllegalArgumentException("askThreshold must be finite");
            }
        }

        @Override
        public ActionPlan decide(ScoreResult score, Budget budget, Context ctx) {
            Objects.requireNonNull(score, "score must not be null");
            Objects.requireNonNull(budget, "budget must not be null");
            Objects.requireNonNull(ctx, "ctx must not be null");

            Map<String, String> tags = ctx.tags() == null ? Map.of() : ctx.tags();
            if (config.allowContextForceSilent() && isTrue(tags.get("policy_force_silent"))) {
                return new ActionPlan(ActionType.SILENT, "forced-silent", ctx.nowEpochMs());
            }
            if (config.allowContextForceSkip() && isTrue(tags.get("policy_force_skip"))) {
                return new ActionPlan(ActionType.SKIP, "forced-skip", ctx.nowEpochMs());
            }

            if (budget.dailyAskLimit() < 0 || budget.asksUsedToday() < 0 || budget.questionBudget() < 0 || budget.cooldownMs() < 0) {
                return new ActionPlan(ActionType.SKIP, "invalid-budget", ctx.nowEpochMs());
            }

            if (Double.isNaN(score.value()) || Double.isInfinite(score.value())) {
                return new ActionPlan(ActionType.SKIP, "invalid-score", ctx.nowEpochMs());
            }

            if (score.value() < config.askThreshold()) {
                return new ActionPlan(ActionType.SILENT, "below-threshold", ctx.nowEpochMs());
            }
            if (budget.asksUsedToday() >= budget.dailyAskLimit()) {
                return new ActionPlan(ActionType.SKIP, "daily-limit-exhausted", ctx.nowEpochMs());
            }
            if (budget.questionBudget() <= 0) {
                return new ActionPlan(ActionType.SKIP, "question-budget-exhausted", ctx.nowEpochMs());
            }
            long cooldownUntil = budget.lastAskEpochMs() + budget.cooldownMs();
            if (ctx.nowEpochMs() < cooldownUntil) {
                return new ActionPlan(ActionType.SKIP, "cooldown-active", cooldownUntil);
            }
            return new ActionPlan(ActionType.ASK, "eligible", ctx.nowEpochMs() + budget.cooldownMs());
        }

        private static boolean isTrue(String value) {
            return "true".equalsIgnoreCase(value);
        }
    }

    public static final class NoopActionExecutor implements ActionExecutor {
        @Override
        public ActionResult execute(ActionPlan plan) {
            return new ActionResult(true, "executed:" + plan.type() + ":" + plan.reason());
        }
    }

    public static final class InMemoryUpdater implements Updater {
        public record UpdateCheckpoint(
                String checkpointId,
                String feedbackId,
                String traceId,
                long revisionBefore,
                long revisionAfter,
                String checksumBefore,
                String checksumAfter) {
        }

        public record ReplayRecord(
                String feedbackId,
                String checksum,
                long appliedAtEpochMs) {
        }

        public interface ReplayLedger {
            Map<String, ReplayRecord> load();

            void save(Map<String, ReplayRecord> records);
        }

        public interface TimeSource {
            long nowEpochMs();
        }

        public static final class SystemTimeSource implements TimeSource {
            @Override
            public long nowEpochMs() {
                return Clock.systemUTC().millis();
            }
        }

        public static final class NoopReplayLedger implements ReplayLedger {
            @Override
            public Map<String, ReplayRecord> load() {
                return Map.of();
            }

            @Override
            public void save(Map<String, ReplayRecord> records) {
                // no-op
            }
        }

        public static final class FileReplayLedger implements ReplayLedger {
            private final Path path;

            public FileReplayLedger(Path path) {
                this.path = Objects.requireNonNull(path);
            }

            @Override
            public Map<String, ReplayRecord> load() {
                if (!Files.exists(path)) {
                    return Map.of();
                }
                Map<String, ReplayRecord> result = new HashMap<>();
                try {
                    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                        if (line.isBlank() || line.startsWith("#")) {
                            continue;
                        }
                        String[] parts = line.split("\t", 3);
                        if (parts.length != 3) {
                            continue;
                        }
                        try {
                            long epoch = Long.parseLong(parts[2]);
                            ReplayRecord record = new ReplayRecord(parts[0], parts[1], epoch);
                            result.put(parts[0], record);
                        } catch (NumberFormatException ignore) {
                            // skip malformed line
                        }
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("failed to load replay ledger: " + path, ex);
                }
                return result;
            }

            @Override
            public void save(Map<String, ReplayRecord> records) {
                try {
                    Path parent = path.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    List<ReplayRecord> sorted = records.values().stream()
                            .sorted(Comparator.comparingLong(ReplayRecord::appliedAtEpochMs))
                            .toList();
                    List<String> lines = new ArrayList<>(sorted.size());
                    for (ReplayRecord record : sorted) {
                        lines.add(record.feedbackId() + "\t" + record.checksum() + "\t" + record.appliedAtEpochMs());
                    }
                    Files.write(path, lines, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                } catch (IOException ex) {
                    throw new IllegalStateException("failed to save replay ledger: " + path, ex);
                }
            }
        }

        private final double learningRate;
        private final int maxHistory;
        private final long replayTtlMs;
        private final int maxReplayEntries;
        private final TimeSource timeSource;
        private final ReplayLedger replayLedger;
        private final Deque<ModelState> history = new ArrayDeque<>();
        private final Deque<UpdateCheckpoint> checkpoints = new ArrayDeque<>();
        private final Map<String, ReplayRecord> replayRecords = new HashMap<>();

        public InMemoryUpdater(double learningRate) {
            this(learningRate, 128);
        }

        public InMemoryUpdater(double learningRate, int maxHistory) {
            this(learningRate, maxHistory, 14L * 24 * 60 * 60 * 1000, 64_000,
                    new SystemTimeSource(), new NoopReplayLedger());
        }

        public InMemoryUpdater(
                double learningRate,
                int maxHistory,
                long replayTtlMs,
                int maxReplayEntries,
                TimeSource timeSource,
                ReplayLedger replayLedger) {
            this.learningRate = learningRate;
            if (maxHistory <= 0) {
                throw new IllegalArgumentException("maxHistory must be > 0");
            }
            if (replayTtlMs <= 0) {
                throw new IllegalArgumentException("replayTtlMs must be > 0");
            }
            if (maxReplayEntries <= 0) {
                throw new IllegalArgumentException("maxReplayEntries must be > 0");
            }
            this.maxHistory = maxHistory;
            this.replayTtlMs = replayTtlMs;
            this.maxReplayEntries = maxReplayEntries;
            this.timeSource = Objects.requireNonNull(timeSource);
            this.replayLedger = Objects.requireNonNull(replayLedger);
            initializeReplayLedger();
        }

        @Override
        public ModelState apply(FeedbackEvent feedback, ExecutionTrace trace, ModelState model) {
            Objects.requireNonNull(feedback, "feedback must not be null");
            Objects.requireNonNull(trace, "trace must not be null");
            Objects.requireNonNull(model, "model must not be null");

            if (feedback.feedbackId() == null || feedback.feedbackId().isBlank()) {
                throw new IllegalArgumentException("feedback.feedbackId must not be blank");
            }
            if (feedback.candidateId() == null || feedback.candidateId().isBlank()) {
                throw new IllegalArgumentException("feedback.candidateId must not be blank");
            }

            long now = timeSource.nowEpochMs();
            pruneReplayLedger(now);

            // Idempotency: repeated apply for same feedbackId does nothing.
            String traceChecksum = checkpointChecksum(model, trace);
            if (replayRecords.containsKey(feedback.feedbackId())) {
                return model;
            }
            if (trace.selectedCandidate() == null || trace.selectedCandidate().candidateId() == null) {
                throw new IllegalArgumentException("trace.selectedCandidate must exist");
            }
            if (!feedback.candidateId().equals(trace.selectedCandidate().candidateId())) {
                throw new IllegalArgumentException("feedback candidate mismatch");
            }

            history.push(model);
            if (trace.selectedFeatureVector() == null) {
                return model;
            }
            Map<String, Double> nextWeights = new HashMap<>(model.weights());
            double direction = feedback.positive() ? 1.0 : -1.0;
            for (Map.Entry<String, Double> feature : trace.selectedFeatureVector().values().entrySet()) {
                double oldWeight = nextWeights.getOrDefault(feature.getKey(), 0.0d);
                double updated = oldWeight + (direction * feature.getValue() * learningRate);
                nextWeights.put(feature.getKey(), updated);
            }
            ModelState updated = new ModelState(model.modelVersion(), model.featureSchemaVersion(), nextWeights, model.revision() + 1);

            UpdateCheckpoint checkpoint = new UpdateCheckpoint(
                    UUID.randomUUID().toString(),
                    feedback.feedbackId(),
                    trace.traceId(),
                    model.revision(),
                    updated.revision(),
                    checkpointChecksum(model, trace),
                    checkpointChecksum(updated, trace));
            checkpoints.push(checkpoint);
            replayRecords.put(feedback.feedbackId(), new ReplayRecord(feedback.feedbackId(), traceChecksum, now));
            trimToMaxHistory();
            trimReplayLedgerToLimit();
            persistReplayLedger();
            return updated;
        }

        @Override
        public ModelState undo() {
            if (history.isEmpty()) {
                throw new IllegalStateException("no-history");
            }
            ModelState previous = history.pop();
            if (!checkpoints.isEmpty()) {
                UpdateCheckpoint checkpoint = checkpoints.pop();
                replayRecords.remove(checkpoint.feedbackId());
                persistReplayLedger();
            }
            return previous;
        }

        public List<UpdateCheckpoint> checkpoints() {
            return List.copyOf(checkpoints);
        }

        private void trimToMaxHistory() {
            while (history.size() > maxHistory) {
                history.removeLast();
            }
            while (checkpoints.size() > maxHistory) {
                checkpoints.removeLast();
            }
        }

        private void initializeReplayLedger() {
            Map<String, ReplayRecord> loaded = replayLedger.load();
            if (loaded == null || loaded.isEmpty()) {
                return;
            }
            for (ReplayRecord record : loaded.values()) {
                if (record == null || record.feedbackId() == null || record.feedbackId().isBlank()) {
                    continue;
                }
                replayRecords.put(record.feedbackId(), record);
            }
            pruneReplayLedger(timeSource.nowEpochMs());
            trimReplayLedgerToLimit();
            persistReplayLedger();
        }

        private void pruneReplayLedger(long nowEpochMs) {
            List<String> expired = new ArrayList<>();
            for (ReplayRecord record : replayRecords.values()) {
                if (nowEpochMs - record.appliedAtEpochMs() > replayTtlMs) {
                    expired.add(record.feedbackId());
                }
            }
            for (String feedbackId : expired) {
                replayRecords.remove(feedbackId);
            }
        }

        private void trimReplayLedgerToLimit() {
            if (replayRecords.size() <= maxReplayEntries) {
                return;
            }
            List<ReplayRecord> sorted = replayRecords.values().stream()
                    .sorted(Comparator.comparingLong(ReplayRecord::appliedAtEpochMs))
                    .toList();
            int removeCount = replayRecords.size() - maxReplayEntries;
            for (int i = 0; i < removeCount; i++) {
                replayRecords.remove(sorted.get(i).feedbackId());
            }
        }

        private void persistReplayLedger() {
            replayLedger.save(replayRecords);
        }

        private static String checkpointChecksum(ModelState state, ExecutionTrace trace) {
            int hash = Objects.hash(
                    state.modelVersion(),
                    state.featureSchemaVersion(),
                    state.revision(),
                    state.weights(),
                    trace.traceId(),
                    trace.selectedCandidate() == null ? "none" : trace.selectedCandidate().candidateId(),
                    trace.selectedScore() == null ? 0.0 : trace.selectedScore().value(),
                    trace.actionPlan() == null ? "none" : trace.actionPlan().reason());
            return Integer.toHexString(hash);
        }
    }

    public record PersistedModel(
            String modelVersion,
            String featureSchemaVersion,
            Map<String, Double> weights,
            long revision) {
        public PersistedModel {
            weights = weights == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(weights));
        }
    }

    public static final class CompatibilityLoader {
        public record CompatibilityLoadResult(
                ModelState modelState,
                List<String> appliedMigrations,
                List<String> warnings) {
            public CompatibilityLoadResult {
                appliedMigrations = List.copyOf(appliedMigrations);
                warnings = List.copyOf(warnings);
            }
        }

        private static final int MIN_SUPPORTED_SCHEMA_VERSION = 1;
        private static final int MAX_SUPPORTED_SCHEMA_VERSION = 2;
        private static final int MIN_SUPPORTED_MODEL_VERSION = 1;
        private static final int MAX_SUPPORTED_MODEL_VERSION = 2;

        public ModelState load(PersistedModel persisted, String targetModelVersion, String targetFeatureSchemaVersion) {
            return loadWithReport(persisted, targetModelVersion, targetFeatureSchemaVersion).modelState();
        }

        public CompatibilityLoadResult loadWithReport(
                PersistedModel persisted,
                String targetModelVersion,
                String targetFeatureSchemaVersion) {
            Objects.requireNonNull(persisted, "persisted must not be null");
            Objects.requireNonNull(targetModelVersion, "targetModelVersion must not be null");
            Objects.requireNonNull(targetFeatureSchemaVersion, "targetFeatureSchemaVersion must not be null");

            int sourceSchema = parseVersion("feature schema", persisted.featureSchemaVersion());
            int targetSchema = parseVersion("target feature schema", targetFeatureSchemaVersion);
            int sourceModel = parseVersion("model", persisted.modelVersion());
            int targetModel = parseVersion("target model", targetModelVersion);

            enforceSupportedRange("feature schema", sourceSchema, MIN_SUPPORTED_SCHEMA_VERSION, MAX_SUPPORTED_SCHEMA_VERSION);
            enforceSupportedRange("target feature schema", targetSchema, MIN_SUPPORTED_SCHEMA_VERSION, MAX_SUPPORTED_SCHEMA_VERSION);
            enforceSupportedRange("model", sourceModel, MIN_SUPPORTED_MODEL_VERSION, MAX_SUPPORTED_MODEL_VERSION);
            enforceSupportedRange("target model", targetModel, MIN_SUPPORTED_MODEL_VERSION, MAX_SUPPORTED_MODEL_VERSION);

            if (targetSchema < sourceSchema) {
                throw new IllegalArgumentException("schema downgrade is not supported");
            }
            if (targetModel < sourceModel) {
                throw new IllegalArgumentException("model downgrade is not supported");
            }

            Map<String, Double> migrated = new HashMap<>(persisted.weights() == null ? Map.of() : persisted.weights());
            List<String> migrations = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            if (sourceSchema == 1 && targetSchema >= 2) {
                if (migrated.containsKey("stay")) {
                    migrated.put("dwell", migrated.get("stay"));
                    migrations.add("schema:1->2:stay-to-dwell");
                } else {
                    warnings.add("schema:1->2:missing-stay-weight");
                }
                migrated.putIfAbsent("night_bonus", 0.0d);
                migrations.add("schema:1->2:add-night_bonus-default");
            }

            if (targetSchema != sourceSchema) {
                migrated.putIfAbsent("schema_migration_bias", 0.0d);
                migrations.add("schema:*:add-schema_migration_bias");
            }

            if (sourceModel == 1 && targetModel >= 2) {
                migrated.putIfAbsent("model_migration_bias", 0.0d);
                migrations.add("model:1->2:add-model_migration_bias");
            }

            // Fill baseline required weights for scorer stability.
            migrated.putIfAbsent("presence", 0.0d);
            migrated.putIfAbsent("hour_of_day", 0.0d);
            migrated.putIfAbsent("dwell", 0.0d);
            migrated.putIfAbsent("night_bonus", 0.0d);

            ModelState modelState = new ModelState(targetModelVersion, targetFeatureSchemaVersion, migrated, persisted.revision());
            return new CompatibilityLoadResult(modelState, migrations, warnings);
        }

        private static int parseVersion(String field, String value) {
            try {
                return Integer.parseInt(value);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(field + " version must be integer: " + value, ex);
            }
        }

        private static void enforceSupportedRange(String field, int version, int min, int max) {
            if (version < min || version > max) {
                throw new IllegalArgumentException(field + " version unsupported: " + version);
            }
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
