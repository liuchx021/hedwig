#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend-react"
BACKEND_DIR="$ROOT_DIR/backend"
BUILD_DIR="$ROOT_DIR/build"

require_command() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 1
  fi
}

configure_java_home() {
  if [[ "$(uname -s)" != "Darwin" ]] || [[ ! -x /usr/libexec/java_home ]]; then
    return
  fi

  local mac_java_home
  mac_java_home="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
  if [[ -n "$mac_java_home" ]]; then
    export JAVA_HOME="$mac_java_home"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
}

echo "[1/4] Checking build tools"
configure_java_home
require_command java
require_command node
require_command npm
require_command mvn

echo "[2/4] Building frontend"
(cd "$FRONTEND_DIR" && npm ci && npm run build)

echo "[3/4] Building backend"
(cd "$BACKEND_DIR" && mvn clean package -DskipTests -q)

echo "[4/4] Staging artifact"
mkdir -p "$BUILD_DIR"
cp "$BACKEND_DIR/target/hedwig.jar" "$BUILD_DIR/hedwig.jar"

echo "Done: build/hedwig.jar ($(du -h "$BUILD_DIR/hedwig.jar" | cut -f1))"
