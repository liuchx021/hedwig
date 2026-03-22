#!/usr/bin/env bash

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

if [[ "${app_inputs_changed}" == true && "${artifact_changed}" != true ]]; then
  echo "ERROR: application source/build inputs changed, but build/hedwig.jar was not updated." >&2
  echo "Run ./scripts/build-and-stage.sh and commit the refreshed build/hedwig.jar before pushing." >&2
  exit 1
fi

if [[ "${undeployed_runtime_changed}" == true ]]; then
  echo "ERROR: deploy/runtime files changed, but the current CI job uploads only build/hedwig.jar." >&2
  echo "Files under deploy/ and scripts/run-http.sh/install-systemd.sh need manual server rollout or a broader deploy pipeline." >&2
  exit 1
fi

if [[ ! -s build/hedwig.jar ]]; then
  echo "ERROR: build/hedwig.jar is missing or empty." >&2
  exit 1
fi

echo "Artifact checksum:"
sha256sum build/hedwig.jar
