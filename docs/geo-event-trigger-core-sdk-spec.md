---
title: "コンテキスト介入中核SDK仕様"
date: "2026-03-05"
tags: ["context-aware", "sdk", "on-device-learning", "location", "policy-engine"]
status: "stable"
updated: "2026-03-05"
hypothesis: "Detector/Policy/Actionを分離したSDK化により、用途別ラッパーを短期間で展開できる。"
impact: 5
effort: 4
risk: 3
---

## Problem

位置・滞在・通知・学習ロジックをアプリごとに個別実装すると、機能追加のたびに再実装が発生し、検証と保守のコストが高い。

## Architecture

中核SDKは `InputEvent -> Candidate -> Feature -> Score -> ActionPlan -> Feedback -> Update` を共通パイプラインとして提供する。  
アプリ側ラッパーは `Detector / Policy / Action` の設定とUI責務を持つ。

## Core Contracts

固定インターフェース:

- `Detector.detect(input, ctx) -> Candidate[]`
- `FeatureExtractor.extract(candidate, ctx) -> FeatureVector`
- `Scorer.score(features, model) -> ScoreResult`
- `Policy.decide(score, budget, ctx) -> ActionPlan`
- `Updater.apply(feedback, trace, model) -> ModelState`
- `ActionExecutor.execute(plan) -> ActionResult`

補助契約:

- `Updater.undo() -> ModelState`（直前更新の復元）
- `CompatibilityLoader.load(persisted, targetModelVersion, targetFeatureSchemaVersion) -> ModelState`
- `CompatibilityLoader.loadWithReport(...) -> CompatibilityLoadResult`

## Behavior Guarantees

### Pipeline

- 候補が複数ある場合は評価可能候補を走査し、最高スコア候補を採用する。
- 候補単位の例外は全体停止させず、候補評価結果に失敗理由を残す。
- 評価可能候補がない場合は `SKIP(no-scorable-candidate)` を返す。

### Policy

- `askThreshold`, 日次上限、質問予算、クールダウンを評価する。
- 失敗安全側として `invalid-budget` / `invalid-score` は `SKIP`。
- コンテキスト強制制御 (`policy_force_silent`, `policy_force_skip`) を許可する。

### Updater

- `feedbackId` で重複適用を抑止する。
- `feedback.candidateId` と `trace.selectedCandidate` が不一致なら更新を拒否する。
- Replay ledger は永続化可能で、TTLと件数上限で管理する。
- `undo()` は直前状態へロールバックし、関連チェックポイントも巻き戻す。

### Compatibility

- `feature_schema_version` / `model_version` の互換読み込みを提供する。
- downgrade は不許可。
- 移行内容は `appliedMigrations` / `warnings` で取得可能。

## Security & Privacy Constraints

- 位置データ・学習データはデフォルトで端末外送信しない。
- Core runtime に外部送信プリミティブと endpoint literal を置かない。
- 生位置/生コンテキスト等の機微データをログ出力しない。

## Validation Criteria

1. 契約テスト
- すべてのプラグイン実装が共通I/O契約を満たす。
- 成功判定: `Detector/Policy/Action` 差し替え後も同一テスト群が通る。

2. 学習整合性テスト
- `FeedbackEvent` の重み更新、`Undo` 復元、再実行決定再現性を検証。
- 成功判定: 直前更新を完全復元し、重複適用を抑止できる。

3. 互換性テスト
- `feature_schema_version` と `model_version` の互換読み込みを検証。
- 成功判定: 旧版データから現行版へ移行し、未対応/不正versionは明示失敗。

4. ポリシー挙動テスト
- クールダウン、日次上限、質問予算、強制制御を検証。
- 成功判定: 予算超過時に `Ask` を抑制し、理由コードが一致する。

## Assumptions

- 初期ターゲットはAndroid中心で、オフライン優先とする。
- SDKはUIを持たず、ラッパー側で画面遷移・文言を実装する。
- 特徴量スキーマとモデルはバージョン管理し、差分更新に対応する。

## Decision Log

- 2026-03-05: `refine` として作成。
- 2026-03-05: 提供形態は `組み込みSDK`。
- 2026-03-05: 抽象単位は `Detector + Policy + Action`。
- 2026-03-05: 端末内完結（外部送信なし）をデフォルト方針。
- 2026-03-05: ラッパー責務を「UI・ドメイン設定・フィードバック取得」に限定。
- 2026-03-05: 実装整合を反映し `stable` に昇格。
