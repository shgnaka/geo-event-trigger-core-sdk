# Issue Seed (9 agent responsibilities)

以下は `1 agent responsibility = 1 Issue = 1 PR` の初期Issue定義。

## 1. PM Agent
- Title: `chore: [agent:pm] define milestone cadence and acceptance board`
- Labels: `agent:pm`, `priority:p1`, `status:ready`
- Body:
  - M0/M1 の受け入れ基準を Issue 化
  - 日次トリアージと blocker 管理フローを固定

## 2. Orchestrator Agent
- Title: `chore: [agent:orchestrator] establish dependency-driven execution lanes`
- Labels: `agent:orchestrator`, `priority:p1`, `status:ready`
- Body:
  - 依存グラフの実運用手順化
  - 並列実行と停止条件の運用テンプレート作成

## 3. API Contract Agent
- Title: `feat: [agent:api-contract] define core SDK interfaces and io contracts`
- Labels: `agent:api-contract`, `area:api`, `priority:p0`, `status:ready`
- Body:
  - 6インターフェース契約の型定義
  - 契約テスト観点の受け渡し

## 4. Pipeline Agent
- Title: `feat: [agent:pipeline] implement core event-to-update pipeline skeleton`
- Labels: `agent:pipeline`, `priority:p0`, `status:ready`
- Body:
  - E2E 正常系パイプライン骨組み
  - トレース/失敗時の戻り値標準化

## 5. Policy Agent
- Title: `feat: [agent:policy] implement cooldown and budget policy engine`
- Labels: `agent:policy`, `priority:p1`, `status:ready`
- Body:
  - クールダウン/日次上限/質問予算制御
  - 境界条件ケース定義

## 6. Updater Agent
- Title: `feat: [agent:updater] implement feedback apply and undo rollback`
- Labels: `agent:updater`, `priority:p1`, `status:ready`
- Body:
  - 更新適用 + Undo
  - 再現性担保の検証点定義

## 7. Compatibility Agent
- Title: `feat: [agent:compat] implement schema and model version compatibility loader`
- Labels: `agent:compat`, `priority:p1`, `status:ready`
- Body:
  - `feature_schema_version`/`model_version` 互換
  - 旧版データ移行パス確認

## 8. Test Agent
- Title: `test: [agent:test] implement contract consistency compatibility policy suites`
- Labels: `agent:test`, `area:test`, `priority:p0`, `status:ready`
- Body:
  - 契約/整合性/互換性/ポリシー挙動テスト
  - CI 組み込み

## 9. Security Audit Agent
- Title: `security: [agent:security-audit] enforce no-external-transfer and log minimization`
- Labels: `agent:security-audit`, `area:security`, `risk:high`, `priority:p0`, `status:ready`
- Body:
  - 外部送信経路の検証
  - ログ最小化監査と阻害条件定義

