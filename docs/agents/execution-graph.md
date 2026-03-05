# Execution Graph (M0 Start)

## Sequence
1. PM Agent
2. Orchestrator Agent
3. API Contract Agent
4. Parallel Block A
   - Pipeline Agent
   - Policy Agent
   - Updater Agent
   - Compatibility Agent
5. Test Agent
6. Security Audit Agent
7. PM Agent (acceptance)

## Dependency Rules
- API Contract Agent の成果物が確定するまで Block A は開始しない。
- Block A は相互参照可能だが、インターフェース変更は API Contract Agent 経由でのみ許可。
- Test Agent は Block A の各PRに対し随時実行し、最終的に全体回帰を実行。
- Security Audit Agent は各PRで差分監査 + マイルストーン終端で全体監査を行う。

## Stop Conditions
- `contract` 失敗: 以降の feature 実装を停止、API Contract Agent に戻す。
- `security` 失敗: PM/Orchestrator を含め全体停止、修正PR優先。
- 互換性破壊: Compatibility Agent が復旧パッチを完了するまでマージ停止。

## M0 Gate
- M0完了条件:
  - インターフェース契約雛形が固定
  - 契約テスト雛形が CI で実行可能
  - 外部送信経路ゼロ/ログ最小化チェックが監査済み

