#!/usr/bin/env bash
# Stage project changes, commit, and optionally push (use in WSL where SSH is configured).
# Usage:
#   ./commit.sh "chore: update readme"
#   ./commit.sh -p "fix: create notes inside current folder"

set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

PUSH=false
MESSAGE_PARTS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -p|-Push) PUSH=true; shift ;;
    *) MESSAGE_PARTS+=("$1"); shift ;;
  esac
done

MESSAGE="${MESSAGE_PARTS[*]}"
MESSAGE="${MESSAGE#"${MESSAGE%%[![:space:]]*}"}"
MESSAGE="${MESSAGE%"${MESSAGE##*[![:space:]]}"}"

if [[ -z "$MESSAGE" ]]; then
  cat <<'EOF'
Commit message required.

Examples:
  ./commit.sh "chore: update readme"
  ./commit.sh -p "fix: folder note path"
EOF
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Not a git repository: $ROOT" >&2
  exit 1
fi

echo "==> Staging changes (excluding build outputs)..."
git add -A -- . \
  ":(exclude)app/build" \
  ":(exclude)build" \
  ":(exclude).gradle" \
  ":(exclude)local.properties" \
  ":(exclude)pat.txt" \
  ":(exclude).env"

mapfile -t STAGED < <(git diff --cached --name-only)
if [[ ${#STAGED[@]} -eq 0 ]]; then
  echo "Nothing to commit."
  exit 0
fi

echo "==> Staged files:"
for f in "${STAGED[@]}"; do
  echo "    $f"
done

echo "==> Commit: $MESSAGE"
git commit -m "$MESSAGE"

if [[ "$PUSH" == true ]]; then
  BRANCH="$(git rev-parse --abbrev-ref HEAD)"
  echo "==> Push branch: $BRANCH"
  if git rev-parse --abbrev-ref "@{u}" >/dev/null 2>&1; then
    git push
  else
    git push -u origin "$BRANCH"
  fi
else
  echo "==> Done. Use -p to push: ./commit.sh -p \"$MESSAGE\""
fi
