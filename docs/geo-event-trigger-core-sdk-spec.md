---
title: "コンテキスト介入中核SDK仕様"
date: "2026-03-05"
tags: ["context-aware", "sdk", "on-device-learning", "location", "policy-engine"]
status: "refine"
updated: "2026-03-05"
hypothesis: "Detector/Policy/Actionを分離したSDK化により、用途別ラッパーを短期間で展開できる。"
impact: 5
effort: 4
risk: 3
---

## Problem

位置・滞在・通知・学習ロジックをアプリごとに個別実装すると、機能追加のたびに再実装が発生し、検証と保守のコストが高い。

## Idea

端末内完結の中核SDKを定義し、アプリ側は用途別ラッパーとして `Detector / Policy / Action` の設定とUIだけを担当する。  
中核は `InputEvent -> Candidate -> Feature -> Score -> ActionPlan -> Feedback -> Update` の共通パイプラインとして提供する。

主要インターフェースは次を固定する。

- `Detector.detect(input, ctx) -> Candidate[]`
- `FeatureExtractor.extract(candidate, ctx) -> FeatureVector`
- `Scorer.score(features, model) -> ScoreResult`
- `Policy.decide(score, budget, ctx) -> ActionPlan`
- `Updater.apply(feedback, trace, model) -> ModelState`
- `ActionExecutor.execute(plan) -> ActionResult`

## Assumptions

- 初期ターゲットはAndroid中心で、オフライン優先とする。
- 位置データや学習データはデフォルトで端末外に送信しない。
- SDKはUIを持たず、ラッパー側で画面遷移・文言を実装する。
- 特徴量スキーマとモデルはバージョン管理し、将来の差分更新に対応する。

## Validation Plan

1. 契約テスト
- すべてのプラグイン実装が共通I/O契約を満たすことを確認する。
- 成功判定: `Detector/Policy/Action` 差し替え後も同じテスト群が通る。

2. 学習整合性テスト
- `FeedbackEvent` に対する重み更新と `Undo` ロールバックを検証する。
- 成功判定: 直前更新の完全復元と、再実行時の決定再現性を満たす。

3. 互換性テスト
- `feature_schema_version` と `model_version` の互換読み込みを検証する。
- 成功判定: 旧版データから現行版モデルへ移行できる。

4. ポリシー挙動テスト
- クールダウン、日次上限、質問予算を跨ぐ境界ケースを検証する。
- 成功判定: 予算超過時に `Ask` が抑制される。

## Decision Log

- 2026-03-05: `refine` に作成。理由: 位置文脈アプリ群で再利用可能な中核仕様を先に固定するため。
- 2026-03-05: 提供形態は `組み込みSDK` を採用。
- 2026-03-05: 抽象単位は `Detector + Policy + Action` の3プラグインを採用。
- 2026-03-05: 端末内完結（位置データ外部送信なし）をデフォルト方針として採用。
- 2026-03-05: ラッパー責務を「UI・ドメイン設定・フィードバック取得」に限定。

## Next Action

実装リポジトリ向けに、上記インターフェースを `templates/spec-mvp.md` 形式でAPI契約として転記する。
