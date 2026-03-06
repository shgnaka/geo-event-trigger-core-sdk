# Orchestrator Runbook

Issue: #2 (`agent:orchestrator`)

## 목적
依存グラフを日次運用し、並列レーンを安全に進める。

## Execution Lanes
- Lane 0 (Control): PM / Orchestrator
- Lane 1 (Contract): API Contract
- Lane 2 (Core Impl): Pipeline / Policy / Updater / Compatibility
- Lane 3 (Quality): Test / Security Audit

## Stage Gates
1. Gate A (Contract Lock)
- 条件:
  - API契約変更PRが `main` に反映済み
  - `contract` check green
- 未達時: Lane 2 は新規着手禁止

2. Gate B (Implementation Lock)
- 条件:
  - Lane 2 の対象Issueが `merged` または `blocked with owner`
  - `unit`, `consistency`, `compatibility`, `policy` green
- 未達時: Lane 3 は最終承認を保留

3. Gate C (Release Readiness)
- 条件:
  - `security` green
  - blocker ラベル付きIssueが0
  - Mx acceptance checklist complete

## Daily Orchestration Loop
1. `gh issue list --state open` で open issue を取得
2. blocker (`status:blocked`) と依存違反を確認
3. レーン単位で「開始可 / 保留 / 停止」を判定
4. 必要なら Orchestrator issue に決定ログを追記
5. PRの required checks を確認して merge順を更新

## Stop Conditions (Operational)
- `contract` 失敗: Lane 2 freeze, owner=`agent:api-contract`
- `security` 失敗: all lanes freeze except security fix
- 互換性破壊: merge freeze, owner=`agent:compat`
- 2回連続で同一check失敗: PMへ escalation

## Escalation SLA
- Critical (`security`): 4時間以内に一次対処
- High (`contract`/compat): 当日中
- Medium (others): 翌営業日

## Artifacts
- 決定ログ: `docs/agents/orchestrator-decision-log.md`
- 週次サマリ: `docs/agents/orchestrator-weekly-template.md`

## Integration Lane Extension
- Lane I1 (Distribution): artifact baseline and version coordinates.
- Lane I2 (Adoption): quick-start docs and minimal wrapper sample.
- Lane I3 (Compliance): security audit against integration assets.
- Stop condition: if I3 fails, freeze I2 merge until remediation evidence is attached.
