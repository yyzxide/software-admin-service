#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

failures=0

env_value() {
  local key="$1"
  local default_value="$2"
  local value=""

  if [[ -f .env ]]; then
    value="$(grep -E "^${key}=" .env | tail -n 1 | cut -d '=' -f 2- || true)"
  fi

  if [[ -n "$value" ]]; then
    value="${value%\"}"
    value="${value#\"}"
    value="${value%\'}"
    value="${value#\'}"
    printf '%s' "$value"
  else
    printf '%s' "$default_value"
  fi
}

check_command() {
  local name="$1"
  if command -v "$name" >/dev/null 2>&1; then
    echo "[ok] $name: $(command -v "$name")"
  else
    echo "[missing] $name"
    failures=$((failures + 1))
  fi
}

check_port() {
  local port="$1"
  local label="$2"
  local output=""

  if ! command -v ss >/dev/null 2>&1; then
    echo "[skip] ss not found; cannot inspect port $port ($label)"
    return
  fi

  if ! output="$(ss -ltn "sport = :$port" 2>/dev/null)"; then
    echo "[skip] cannot inspect port $port due to local permission limits ($label)"
    return
  fi

  if grep -q ":$port" <<<"$output"; then
    echo "[warn] port $port is already in use ($label)"
  else
    echo "[ok] port $port is available ($label)"
  fi
}

if [[ -f .env ]]; then
  echo "[ok] .env loaded"
else
  echo "[warn] .env not found; run: make setup"
fi

echo ""
echo "Checking commands..."
check_command java
check_command mvn
check_command docker
check_command mysql
check_command curl
check_command python3

if docker compose version >/dev/null 2>&1; then
  echo "[ok] docker compose: $(docker compose version --short 2>/dev/null || docker compose version)"
else
  echo "[missing] docker compose"
  failures=$((failures + 1))
fi

echo ""
echo "Checking default ports..."
check_port "$(env_value ADMIN_SERVER_PORT 8090)" "admin-service"
check_port "$(env_value ADMIN_DB_HOST_PORT 3308)" "docker mysql host port"
check_port "$(env_value ADMIN_REDIS_HOST_PORT 6381)" "docker redis host port"

echo ""
if [[ "$failures" -gt 0 ]]; then
  echo "Doctor finished with $failures missing dependency item(s)."
  exit 1
fi

echo "Doctor passed. You can run: make docker-up && make init-db && make run"
