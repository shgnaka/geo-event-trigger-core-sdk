# Security Audit Controls

`agent:security-audit` が CI とローカルで検証する最小コントロール。

参照: `docs/agents/security-two-stage-runbook.md`

## Runtime Controls
- Core runtime (`src/main/java`) にネットワーク転送プリミティブを置かない。
- Core runtime に `http://` / `https://` の endpoint literal を置かない。
- 生の位置/文脈/候補/スコア情報をログ出力しない。

## CI Gate
- `security` job で以下を実行:
  - secret-like pattern scan
  - dependency audit (best effort)
  - `ci/run-security-audit.sh`
  - `security-loop/run.sh --mode ci` (attacker -> defender -> summary)

## Two-Stage Agent Modes
- CI mode:
  - Static attacker/defender only
  - JSON reports are uploaded as Actions artifact
  - Gate threshold: `high` (high/critical => fail)
- Local mode:
  - Optional codex-assisted attacker/defender (`ENABLE_CODEX_ATTACKER=1`)
  - Result summary should be attached to PR comment/evidence

## Exception Rule
- 例外が必要な場合は以下を必須化:
  - Security issue を事前起票
  - 例外理由、データフロー、代替案、期限付き撤去計画を記録
  - PM + Security Audit の両方で受け入れ判断

## Severity Handling
- `critical` / `high`:
  - CI gate fail
  - immediate remediation required
- `medium`:
  - CI gate pass
  - auto-create/update tracking issue
  - PM daily triageで owner + ETA 必須
- `low` / `info`:
  - report only
  - weekly review for trend tracking
