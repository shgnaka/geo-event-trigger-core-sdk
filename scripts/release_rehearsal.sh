#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.1.0-rc1}"
REPO="${2:-shgnaka/geo-event-trigger-core-sdk}"
CREATE_DRAFT_RELEASE="${CREATE_DRAFT_RELEASE:-1}"

require_clean_main() {
  local branch
  branch="$(git rev-parse --abbrev-ref HEAD)"
  if [ "$branch" != "main" ]; then
    echo "Must run on main branch. current=$branch"
    exit 1
  fi
  if [ -n "$(git status --porcelain)" ]; then
    echo "Working tree must be clean."
    exit 1
  fi
}

run_checks() {
  ci/run-lint.sh
  ci/run-unit.sh
  ci/run-consistency.sh
  ci/run-contract.sh
  ci/run-compatibility.sh
  ci/run-policy.sh
  ci/run-security-audit.sh
}

tag_exists_remote() {
  git ls-remote --tags origin "refs/tags/$VERSION" | grep -q "$VERSION"
}

cleanup() {
  set +e
  if [ "$CREATE_DRAFT_RELEASE" = "1" ]; then
    gh release delete "$VERSION" --repo "$REPO" --yes >/dev/null 2>&1 || true
  fi
  git push --delete origin "$VERSION" >/dev/null 2>&1 || true
  git tag -d "$VERSION" >/dev/null 2>&1 || true
  set -e
}

main() {
  require_clean_main

  if git rev-parse "$VERSION" >/dev/null 2>&1; then
    echo "Tag already exists locally: $VERSION"
    exit 1
  fi
  if tag_exists_remote; then
    echo "Tag already exists on origin: $VERSION"
    exit 1
  fi

  echo "[1/5] running release checks"
  run_checks

  echo "[2/5] creating and pushing temporary tag $VERSION"
  git tag -a "$VERSION" -m "release rehearsal tag $VERSION"
  git push origin "$VERSION"

  if [ "$CREATE_DRAFT_RELEASE" = "1" ]; then
    echo "[3/5] creating draft prerelease"
    gh release create "$VERSION" \
      --repo "$REPO" \
      --target main \
      --title "$VERSION (rehearsal)" \
      --notes "Temporary release rehearsal artifact." \
      --prerelease \
      --draft
  else
    echo "[3/5] skipping draft prerelease"
  fi

  echo "[4/5] rehearsal verification complete"

  echo "[5/5] cleanup temporary artifacts"
  cleanup

  echo "Rehearsal completed: $VERSION"
}

main "$@"
