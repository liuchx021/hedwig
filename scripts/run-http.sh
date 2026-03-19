#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${APP_HOME:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
APP_JAR="${APP_JAR:-$APP_DIR/app.jar}"
ENV_FILE="${ENV_FILE:-$APP_DIR/app.env}"
EXAMPLE_ENV_FILE="$APP_DIR/app.env.example"
LOG_DIR="${LOG_DIR:-$APP_DIR/logs}"
LOG_FILE="${LOG_FILE:-$LOG_DIR/app.log}"
PID_FILE="${PID_FILE:-$APP_DIR/app.pid}"

ensure_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
    return
  fi

  head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'
}

ensure_env_file() {
  if [[ -f "$ENV_FILE" ]]; then
    return
  fi

  if [[ ! -f "$EXAMPLE_ENV_FILE" ]]; then
    echo "Missing template env file: $EXAMPLE_ENV_FILE" >&2
    exit 1
  fi

  cp "$EXAMPLE_ENV_FILE" "$ENV_FILE"
  local secret
  secret="$(ensure_secret)"
  sed -i.bak "s/^JWT_SECRET=.*/JWT_SECRET=$secret/" "$ENV_FILE"
  rm -f "$ENV_FILE.bak"
  echo "Created $ENV_FILE with a fresh JWT secret."
}

load_env_file() {
  ensure_env_file
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
}

is_running() {
  [[ -f "$PID_FILE" ]] || return 1
  local pid
  pid="$(cat "$PID_FILE")"
  [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1
}

stop_app() {
  if ! is_running; then
    echo "Application is not running."
    return 0
  fi

  local pid
  pid="$(cat "$PID_FILE")"
  kill "$pid"

  for _ in {1..20}; do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      rm -f "$PID_FILE"
      echo "Stopped process $pid."
      return 0
    fi
    sleep 1
  done

  echo "Process $pid did not stop gracefully, sending SIGKILL."
  kill -9 "$pid"
  rm -f "$PID_FILE"
}

build_java_command() {
  read -r -a JAVA_CMD <<< "${JAVA_OPTS:-}"
  JAVA_CMD=(java "${JAVA_CMD[@]}" -jar "$APP_JAR")
}

run_foreground() {
  load_env_file
  mkdir -p "${APP_DATA_DIR:-$APP_DIR/data}" "$LOG_DIR"
  export APP_DATA_DIR="${APP_DATA_DIR:-$APP_DIR/data}"
  export SERVER_ADDRESS="${SERVER_ADDRESS:-0.0.0.0}"
  export SERVER_PORT="${SERVER_PORT:-8080}"
  export TZ="${TZ:-Asia/Shanghai}"
  build_java_command
  exec "${JAVA_CMD[@]}"
}

start_background() {
  load_env_file
  mkdir -p "${APP_DATA_DIR:-$APP_DIR/data}" "$LOG_DIR"
  export APP_DATA_DIR="${APP_DATA_DIR:-$APP_DIR/data}"
  export SERVER_ADDRESS="${SERVER_ADDRESS:-0.0.0.0}"
  export SERVER_PORT="${SERVER_PORT:-8080}"
  export TZ="${TZ:-Asia/Shanghai}"

  if is_running; then
    echo "Application is already running with PID $(cat "$PID_FILE")."
    return 0
  fi

  build_java_command
  nohup "${JAVA_CMD[@]}" >>"$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
  echo "Started application with PID $(cat "$PID_FILE")."
  echo "Log file: $LOG_FILE"
}

show_status() {
  if is_running; then
    echo "Application is running with PID $(cat "$PID_FILE")."
  else
    echo "Application is not running."
  fi
}

show_logs() {
  mkdir -p "$LOG_DIR"
  touch "$LOG_FILE"
  tail -n "${TAIL_LINES:-100}" -f "$LOG_FILE"
}

if [[ ! -f "$APP_JAR" ]]; then
  echo "Missing application jar: $APP_JAR" >&2
  exit 1
fi

case "${1:-foreground}" in
  foreground)
    run_foreground
    ;;
  start)
    start_background
    ;;
  stop)
    stop_app
    ;;
  restart)
    stop_app || true
    start_background
    ;;
  status)
    show_status
    ;;
  logs)
    show_logs
    ;;
  *)
    echo "Usage: $0 [foreground|start|stop|restart|status|logs]" >&2
    exit 1
    ;;
esac
