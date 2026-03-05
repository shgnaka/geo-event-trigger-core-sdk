#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.1.0}"
REPO="${2:-shgnaka/geo-event-trigger-core-sdk}"
TITLE="${3:-$VERSION}"
NOTES_FILE="${NOTES_FILE:-}"

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

create_release() {
  if [ -n "$NOTES_FILE" ]; then
    gh release create "$VERSION" \
      --repo "$REPO" \
      --target main \
      --title "$TITLE" \
      --notes-file "$NOTES_FILE"
  else
    gh release create "$VERSION" \
      --repo "$REPO" \
      --target main \
      --title "$TITLE" \
      --generate-notes
  fi
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

  echo "[2/5] creating annotated tag $VERSION"
  git tag -a "$VERSION" -m "release $VERSION"

  echo "[3/5] pushing tag to origin"
  git push origin "$VERSION"

  echo "[4/5] creating GitHub release"
  create_release

  echo "[5/5] release completed: $VERSION"
  gh release view "$VERSION" --repo "$REPO" --json tagName,isDraft,isPrerelease,url
}

main "$@"
