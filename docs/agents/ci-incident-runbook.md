# CI Incident Runbook

Issue: #31 (`agent:orchestrator`), #1 (`agent:pm`)

## Purpose
GitHub Actions check failure時に、コード不具合とCI基盤障害を迅速に切り分ける。

## Triage Steps
1. 対象PRのcheck一覧を確認:
   - `gh pr checks <PR_NUMBER>`
2. 失敗ジョブのログを確認:
   - `gh run view <RUN_ID> --job <JOB_ID> --log-failed`
3. 失敗種別を分類:
   - `Service Unavailable` / `Failed to resolve action download info`: 基盤側一時障害
   - `minutes` / `billing` / `spending limit exceeded`: 利用枠超過
   - テスト失敗/アサーション失敗: コード不具合

## Recovery Actions
### A. 基盤側一時障害
1. 失敗ジョブのみ再実行:
   - `gh run rerun <RUN_ID> --failed`
2. 15分以内に同一エラー継続時:
   - PRを `status:blocked` にして次サイクルへ繰越
   - Orchestrator decision logへ記録

### B. 利用枠超過
1. 利用枠を確認（`user` scope 必要）:
   - `gh auth refresh -h github.com -s user`
   - `gh api /users/<USER>/settings/billing/actions`
2. PMが当日対応を決定:
   - 追加枠確保 or 実行頻度の一時抑制
3. 対応完了まで新規PRのマージを保留

### C. コード不具合
1. 失敗テストの担当agentへアサイン
2. 修正PRで再検証

## Evidence Required
- PRコメントに以下を残す:
  - 発生日時（UTC）
  - 失敗ジョブURL
  - 判定カテゴリ（A/B/C）
  - 実施アクションと結果
