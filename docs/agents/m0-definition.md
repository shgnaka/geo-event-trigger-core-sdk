# M0 Definition

## Goal
実装開始前に、仕様逸脱を防ぐ最小の制御面を完成させる。

## Deliverables
1. API契約骨組み（6インターフェース）
2. 契約テスト雛形（差し替え可能性を確認）
3. プライバシー/セキュリティ監査チェック（外部送信なし、ログ最小化）

## Exit Criteria
- `lint/unit/contract/security` が main 相当で green。
- `Detector/Policy/Action` 置換に対する契約テスト雛形が存在。
- 監査レポートで high リスクが 0。

## Non-Goals
- ドメイン別UI
- モデル精度最適化
- 外部連携基盤の追加

