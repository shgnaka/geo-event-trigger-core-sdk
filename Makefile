.PHONY: lint test contract consistency compatibility policy security

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

security:
	@if [ -x .github/workflows/ci.yml ]; then echo "security gate is on GitHub Actions"; fi
	@echo "Run local secret scan"
	@git grep -nE '(AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|AIza[0-9A-Za-z_-]{35}|-----BEGIN (RSA|OPENSSH|EC) PRIVATE KEY-----)' -- . ':!docs/*' && exit 1 || true
	@echo "security: ok"
