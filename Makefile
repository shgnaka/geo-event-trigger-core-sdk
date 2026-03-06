.PHONY: lint test contract consistency compatibility policy artifact verify-consumer security security-loop

lint:
	ci/run-lint.sh

test:
	ci/run-unit.sh

contract:
	ci/run-contract.sh

consistency:
	ci/run-consistency.sh

compatibility:
	ci/run-compatibility.sh

policy:
	ci/run-policy.sh

artifact:
	scripts/build_artifact.sh 0.1.0

verify-consumer:
	scripts/verify_consumer.sh 0.1.0

security:
	@echo "Run local secret scan"
	@git grep -nE '(AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|AIza[0-9A-Za-z_-]{35}|-----BEGIN (RSA|OPENSSH|EC) PRIVATE KEY-----)' -- . ':!docs/*' && exit 1 || true
	@ci/run-security-audit.sh
	@echo "security: ok"

security-loop:
	@security-loop/run.sh --module core-sdk --iterations 1 --mode ci --out-dir out/security-loop-local --gate-threshold high
