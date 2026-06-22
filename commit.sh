#!/usr/bin/env bash
# Stage project changes, commit, and optionally push (use in WSL where SSH is configured).
# Usage:
#   ./commit.sh "chore: update readme"
#   ./commit.sh -p "fix: create notes inside current folder"
#   ./commit.sh --push-only          # push only (commit already done on Windows)
#
# Environment:
#   COMMIT_MESSAGE       commit message (avoids shell quoting issues)
#   COMMIT_MESSAGE_FILE  path to file containing the message (UTF-8)

set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

PUSH=false
PUSH_ONLY=false
MESSAGE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -p|-Push) PUSH=true; shift ;;
    --push-only) PUSH_ONLY=true; shift ;;
    -*) echo "Unknown option: $1" >&2; exit 1 ;;
    *)
      if [[ -z "$MESSAGE" ]]; then
        MESSAGE="$1"
      else
        MESSAGE="$MESSAGE $1"
      fi
      shift
      ;;
  esac
done

if [[ -n "${COMMIT_MESSAGE_FILE:-}" && -f "$COMMIT_MESSAGE_FILE" ]]; then
  MESSAGE="$(<"$COMMIT_MESSAGE_FILE")"
elif [[ -n "${COMMIT_MESSAGE:-}" ]]; then
  MESSAGE="$COMMIT_MESSAGE"
fi

MESSAGE="${MESSAGE#"${MESSAGE%%[![:space:]]*}"}"
MESSAGE="${MESSAGE%"${MESSAGE##*[![:space:]]}"}"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Not a git repository: $ROOT" >&2
  exit 1
fi

push_branch() {
  local branch
  branch="$(git rev-parse --abbrev-ref HEAD)"
  echo "==> Push branch: $branch"
  if git rev-parse --abbrev-ref "@{u}" >/dev/null 2>&1; then
    git push
  else
    git push -u origin "$branch"
  fi
}

if [[ "$PUSH_ONLY" == true ]]; then
  push_branch
  exit 0
fi

if [[ -z "$MESSAGE" ]]; then
  cat <<'EOF'
Commit message required.

Examples:
  ./commit.sh "chore: update readme"
  ./commit.sh -p "fix: folder note path"
  COMMIT_MESSAGE="fix: foo" ./commit.sh -p
EOF
  exit 1
fi

stage_changes() {
  echo "==> Staging changes (excluding build outputs)..."
  set +e
  git add -A -- . \
    ":(exclude)app/build" \
    ":(exclude)build" \
    ":(exclude).gradle" \
    ":(exclude)local.properties" \
    ":(exclude)pat.txt" \
    ":(exclude).env"
  local add_status=$?
  set -e
  # git add may exit 1 when pathspec matches only ignored paths; not fatal.
  if [[ $add_status -gt 1 ]]; then
    exit $add_status
  fi
}

stage_changes

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
  push_branch
else
  echo "==> Done. Use -p to push: ./commit.sh -p \"\$MESSAGE\""
fi
