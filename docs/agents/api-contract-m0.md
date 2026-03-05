# API Contract M0

M0で固定する中核インターフェース:

- `Detector.detect(input, ctx) -> Candidate[]`
- `FeatureExtractor.extract(candidate, ctx) -> FeatureVector`
- `Scorer.score(features, model) -> ScoreResult`
- `Policy.decide(score, budget, ctx) -> ActionPlan`
- `Updater.apply(feedback, trace, model) -> ModelState`
- `ActionExecutor.execute(plan) -> ActionResult`

実装参照: `src/main/java/com/geo/sdk/core/CoreSdk.java`

契約の最小要件:
- すべてのI/Oは副作用最小のデータオブジェクトで受け渡す。
- `Policy` は budget/cooldown を必ず評価する。
- `Updater` は `undo()` で直前状態を復元できる。
- `CompatibilityLoader` は旧 schema/model から現行へ読み込み可能。
