# Multi-Agent Charter

この文書は `geo-event-trigger-core-sdk` の実装に使う 9 エージェントの責務境界を固定する。

## 1) PM Agent
- Objective: マイルストーン管理、優先度決定、受け入れ判定を行う。
- Inputs: 仕様書、各エージェント進捗、CI結果、リスク報告。
- Outputs: 週次/日次優先度、Issue順序、受け入れ判定記録。
- Non-goals: 実装詳細の決定、コード変更。
- DoD: M0/M1 の完了判定が記録され、ブロッカーが割り当て済み。

## 2) Orchestrator Agent
- Objective: 技術依存の解消順序を設計し、ハンドオフを調停する。
- Inputs: 全エージェントの出力物、依存関係、失敗レポート。
- Outputs: 実行順序、並列化可能区間、再実行指示。
- Non-goals: プロダクト優先度の変更。
- DoD: 依存違反ゼロで PR が統合可能状態になる。

## 3) API Contract Agent
- Objective: 中核インターフェースの I/O 契約を定義・固定する。
- Inputs: 仕様書、互換性要件、テスト要件。
- Outputs: API契約文書、型定義、契約テスト観点。
- Non-goals: 推論ロジック実装。
- DoD: `Detector/FeatureExtractor/Scorer/Policy/Updater/ActionExecutor` 契約がテスト可能な形で確定。

## 4) Pipeline Agent
- Objective: `InputEvent -> Candidate -> Feature -> Score -> ActionPlan -> Feedback -> Update` を実装する。
- Inputs: API契約、Policy/Updater 要件。
- Outputs: パイプライン実装、トレース情報、エラーハンドリング。
- Non-goals: 予算ポリシーの最適化。
- DoD: 正常系の end-to-end 実行が再現可能。

## 5) Policy Agent
- Objective: クールダウン、日次上限、質問予算制御を実装する。
- Inputs: ScoreResult、Budget、Context。
- Outputs: ActionPlan 決定ロジック、境界条件テスト仕様。
- Non-goals: モデル学習更新。
- DoD: 境界ケースで `Ask` 抑制が仕様どおり動作。

## 6) Updater Agent
- Objective: フィードバック反映、更新履歴、Undo ロールバックを実装する。
- Inputs: FeedbackEvent、Trace、ModelState。
- Outputs: 更新後 ModelState、Undo履歴、再現性指標。
- Non-goals: UI上の説明設計。
- DoD: 直前更新の完全復元と再実行決定再現性を満たす。

## 7) Compatibility Agent
- Objective: `feature_schema_version` と `model_version` 互換読み込みを保証する。
- Inputs: 旧版データ、現行スキーマ、移行ポリシー。
- Outputs: 互換ローダ、マイグレーション手順、互換性テストケース。
- Non-goals: 新特徴量アルゴリズム追加。
- DoD: 旧版から現行版へロスなく移行可能。

## 8) Test Agent
- Objective: 契約/学習整合性/互換性/ポリシー挙動の自動テストを整備する。
- Inputs: API契約、各実装PR、失敗ログ。
- Outputs: テストスイート、CI統合、失敗解析。
- Non-goals: 本番データ監視設計。
- DoD: 必須テスト群が CI で安定実行される。

## 9) Security Audit Agent
- Objective: 端末外送信なし保証とログ最小化を監査する。
- Inputs: 変更差分、ログ出力、依存関係、設定ファイル。
- Outputs: 監査レポート、阻害要因、修正提案。
- Non-goals: 機能要件の優先度調整。
- DoD: 外部送信経路が許可済み以外ゼロ、機微ログ出力ゼロ、高リスク脆弱性ゼロ。

