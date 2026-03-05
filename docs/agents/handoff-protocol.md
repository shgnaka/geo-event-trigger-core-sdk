# Agent Handoff Protocol

## Handoff Envelope
全エージェント間の受け渡しは以下の最小構造に統一する。

```yaml
handoff_id: "<agent>-<issue>-<timestamp>"
from: "<agent>"
to: "<agent>"
input:
  issue: "#123"
  artifacts:
    - path: "..."
      type: "code|doc|test|report"
decision:
  summary: "what changed"
  constraints:
    - "..."
verification:
  checks:
    - name: "lint|unit|contract|security|custom"
      result: "pass|fail|skip"
  evidence:
    - "command/output pointer"
blockers:
  - "none|description"
next_action:
  owner: "<agent>"
  eta: "YYYY-MM-DD"
```

## Return / Reject Rule
- Reject 条件:
  - 必須入力欠落（Issue番号、artifact path、検証結果）
  - 契約逸脱（API互換破壊、無許可外部送信、機微ログ追加）
  - CI失敗が未説明
- Reject 時アクション:
  - `status:blocked` ラベルを付与
  - 差し戻し理由を 3 行以内で記録
  - 再提出時に前回 `handoff_id` を参照

## Priority and Conflict Rule
- 優先度順: `security > contract > compatibility > feature > docs`
- 競合時の決定権:
  - 仕様解釈: PM Agent
  - 技術依存順: Orchestrator Agent
  - セキュリティ阻害: Security Audit Agent が veto 可

## Completion Criteria
- Handoff 完了は以下を満たすこと:
  - 次担当が `accepted` を明示
  - 必須CIチェック結果が handoff に含まれる
  - blocker が `none` または対応Issueが紐付く

