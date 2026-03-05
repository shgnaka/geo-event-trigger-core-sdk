# Test Matrix (Agent:test)

CIで実行するスイート:

- `contract`
  - インターフェース契約公開
  - プラグイン差し替え可能性
- `consistency`
  - パイプライン候補選択/失敗耐性
  - updater apply/undo と再現性チェックポイント
- `compatibility`
  - schema/model 移行
  - ダウングレード・未対応バージョン拒否
- `policy`
  - 閾値、日次上限、質問予算、クールダウン
  - force制御と入力異常の fail-safe

実行コマンド:
- `ci/run-contract.sh`
- `ci/run-consistency.sh`
- `ci/run-compatibility.sh`
- `ci/run-policy.sh`
