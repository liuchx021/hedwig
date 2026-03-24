#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# verify-release-commit.sh
#
# Verifies that a push to develop has a consistent release state:
#   1. If app source changed, build/hedwig.jar must also be updated
#   2. If deploy/runtime files changed, warn (or block unless skipped)
#   3. build/hedwig.jar must exist and be non-empty
#
# Supports [skip-deploy-check] in the latest commit message to
# allow deploy/* changes without blocking the pipeline.
# ──────────────────────────────────────────────────────────────
set -euo pipefail

resolve_diff_range() {
  if git rev-parse --verify HEAD^ >/dev/null 2>&1; then
    printf 'HEAD^..HEAD'
  else
    printf 'HEAD'
  fi
}

DIFF_RANGE="${1:-$(resolve_diff_range)}"
CHANGED_FILES="$(git diff --name-only --diff-filter=ACMR "${DIFF_RANGE}")"

if [[ -z "${CHANGED_FILES}" ]]; then
  echo "No tracked file changes detected in ${DIFF_RANGE}."
  exit 0
fi

echo "Changed files in ${DIFF_RANGE}:"
printf '%s\n' "${CHANGED_FILES}"
echo

artifact_changed=false
app_inputs_changed=false
undeployed_runtime_changed=false

while IFS= read -r file; do
  [[ -z "${file}" ]] && continue

  case "${file}" in
    build/hedwig.jar)
      artifact_changed=true
      ;;
  esac

  case "${file}" in
    backend/*|frontend-react/*)
      case "${file}" in
        backend/target/*|frontend-react/node_modules/*|frontend-react/.vite/*)
          ;;
        *)
          app_inputs_changed=true
          ;;
      esac
      ;;
    deploy/*|scripts/run-http.sh|scripts/install-systemd.sh)
      undeployed_runtime_changed=true
      ;;
  esac
done <<< "${CHANGED_FILES}"

# ── Check 1: source changed but jar not rebuilt ───────────────
if [[ "${app_inputs_changed}" == true && "${artifact_changed}" != true ]]; then
  echo "ERROR: application source/build inputs changed, but build/hedwig.jar was not updated." >&2
  echo "Run ./scripts/build-and-stage.sh and commit the refreshed build/hedwig.jar before pushing." >&2
  exit 1
fi

# ── Check 2: deploy/runtime files changed ────────────────────
if [[ "${undeployed_runtime_changed}" == true ]]; then
  COMMIT_MSG="$(git log -1 --format='%s%n%b' 2>/dev/null || true)"

  if echo "${COMMIT_MSG}" | grep -qiF '[skip-deploy-check]'; then
    echo "WARNING: deploy/runtime files changed, but [skip-deploy-check] found in commit message — continuing."
  else
    echo "ERROR: deploy/runtime files changed, but the current CI job uploads only build/hedwig.jar." >&2
    echo "Files under deploy/ and scripts/run-http.sh/install-systemd.sh need manual server rollout or a broader deploy pipeline." >&2
    echo "" >&2
    echo "To bypass this check, include [skip-deploy-check] in your commit message." >&2
    exit 1
  fi
fi

# ── Check 3: artifact must exist ─────────────────────────────
if [[ ! -s build/hedwig.jar ]]; then
  echo "ERROR: build/hedwig.jar is missing or empty." >&2
  exit 1
fi

echo "Artifact checksum:"
sha256sum build/hedwig.jar
echo
echo "All checks passed."
