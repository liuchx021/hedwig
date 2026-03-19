#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${APP_HOME:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
SERVICE_NAME="${SERVICE_NAME:-hedwig}"
SERVICE_USER="${SERVICE_USER:-$(id -un)}"
SERVICE_GROUP="${SERVICE_GROUP:-$(id -gn)}"
UNIT_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

if ! command -v systemctl >/dev/null 2>&1; then
  echo "systemctl not found. This script is intended for systemd-based Ubuntu servers." >&2
  exit 1
fi

if [[ ! -f "$APP_DIR/run-http.sh" ]]; then
  echo "Missing run-http.sh in $APP_DIR" >&2
  exit 1
fi

SUDO=""
if [[ "${EUID}" -ne 0 ]]; then
  SUDO="sudo"
fi

$SUDO tee "$UNIT_FILE" >/dev/null <<EOF
[Unit]
Description=Hedwig
After=network.target

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_GROUP
WorkingDirectory=$APP_DIR
Environment=ENV_FILE=$APP_DIR/app.env
ExecStart=$APP_DIR/run-http.sh
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

$SUDO systemctl daemon-reload
$SUDO systemctl enable --now "$SERVICE_NAME"
$SUDO systemctl status --no-pager "$SERVICE_NAME"
