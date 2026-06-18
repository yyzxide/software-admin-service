#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${ADMIN_BASE_URL:-http://127.0.0.1:8090}"
USERNAME="${ADMIN_SECURITY_USERNAME:-admin}"
PASSWORD="${ADMIN_SECURITY_PASSWORD:-admin123456}"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

request_json() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  local output="$TMP_DIR/response.json"

  if [[ -n "$body" ]]; then
    curl -fsS -X "$method" "$BASE_URL$path" \
      -H 'Content-Type: application/json' \
      -d "$body" \
      -o "$output"
  else
    curl -fsS -X "$method" "$BASE_URL$path" -o "$output"
  fi

  python3 - "$output" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as fh:
    payload = json.load(fh)

if payload.get("code") != 0:
    raise SystemExit(f"API returned non-zero code: {payload}")
PY
}

request_auth_json() {
  local method="$1"
  local path="$2"
  local token="$3"
  local output="$TMP_DIR/response.json"

  curl -fsS -X "$method" "$BASE_URL$path" \
    -H "Authorization: Bearer $token" \
    -o "$output"

  python3 - "$output" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as fh:
    payload = json.load(fh)

if payload.get("code") != 0:
    raise SystemExit(f"API returned non-zero code: {payload}")
PY
}

echo "[1/9] health"
request_json GET /api/v1/admin/health

echo "[2/9] login"
LOGIN_BODY="$(printf '{"username":"%s","password":"%s"}' "$USERNAME" "$PASSWORD")"
request_json POST /api/v1/admin/auth/login "$LOGIN_BODY"
TOKEN="$(python3 - "$TMP_DIR/response.json" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as fh:
    payload = json.load(fh)

print(payload["data"]["access_token"])
PY
)"

echo "[3/9] current user"
request_auth_json GET /api/v1/admin/auth/me "$TOKEN"

echo "[4/9] swagger api docs"
curl -fsS "$BASE_URL/v3/api-docs" -o "$TMP_DIR/openapi.json"
python3 - "$TMP_DIR/openapi.json" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as fh:
    payload = json.load(fh)

paths = payload.get("paths", {})
required = [
    "/api/v1/admin/auth/login",
    "/api/v1/admin/rbac/users",
    "/api/v1/admin/rbac/roles",
    "/api/v1/admin/rbac/permissions",
    "/api/v1/admin/software/apps",
    "/api/v1/admin/reviews",
    "/api/v1/admin/operation-logs",
]
missing = [path for path in required if path not in paths]
if missing:
    raise SystemExit(f"OpenAPI paths missing: {missing}")
PY

echo "[5/9] categories"
request_auth_json GET /api/v1/admin/categories "$TOKEN"

echo "[6/9] tags"
request_auth_json GET /api/v1/admin/tags "$TOKEN"

echo "[7/9] software list"
request_auth_json GET /api/v1/admin/software/apps "$TOKEN"

echo "[8/9] RBAC"
request_auth_json GET /api/v1/admin/rbac/users "$TOKEN"
request_auth_json GET /api/v1/admin/rbac/roles "$TOKEN"
request_auth_json GET /api/v1/admin/rbac/permissions "$TOKEN"

echo "[9/9] reviews and operation logs"
request_auth_json GET /api/v1/admin/reviews "$TOKEN"
request_auth_json GET /api/v1/admin/operation-logs "$TOKEN"

echo "Smoke checks passed: $BASE_URL"
