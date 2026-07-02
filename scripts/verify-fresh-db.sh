#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-growgate_fresh_db_$$}"
MYSQL_PORT="${MYSQL_PORT:-33306}"
APP_PORT="${APP_PORT:-18080}"
LOG_DIR="${LOG_DIR:-$ROOT_DIR/.tmp/fresh-db-verify}"
APP_LOG="$LOG_DIR/backend.log"
APP_PID=""

mkdir -p "$LOG_DIR"
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

cleanup() {
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
  COMPOSE_PROJECT_NAME="$PROJECT_NAME" MYSQL_PORT="$MYSQL_PORT" \
    docker compose -f "$ROOT_DIR/database/docker-compose.yml" down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "[fresh-db] missing command: $1" >&2
    exit 127
  }
}

wait_for_mysql() {
  local cid
  cid="$(COMPOSE_PROJECT_NAME="$PROJECT_NAME" MYSQL_PORT="$MYSQL_PORT" \
    docker compose -f "$ROOT_DIR/database/docker-compose.yml" ps -q growgate-mysql)"
  for _ in {1..60}; do
    if [[ "$(docker inspect -f '{{.State.Health.Status}}' "$cid" 2>/dev/null || true)" == "healthy" ]]; then
      return 0
    fi
    sleep 2
  done
  echo "[fresh-db] MySQL did not become healthy" >&2
  COMPOSE_PROJECT_NAME="$PROJECT_NAME" MYSQL_PORT="$MYSQL_PORT" \
    docker compose -f "$ROOT_DIR/database/docker-compose.yml" logs growgate-mysql >&2 || true
  exit 1
}

wait_for_health() {
  for _ in {1..60}; do
    if curl -fsS "http://127.0.0.1:$APP_PORT/health" >/dev/null 2>&1; then
      return 0
    fi
    if [[ -n "$APP_PID" ]] && ! kill -0 "$APP_PID" 2>/dev/null; then
      echo "[fresh-db] backend exited before health check" >&2
      tail -120 "$APP_LOG" >&2 || true
      exit 1
    fi
    sleep 2
  done
  echo "[fresh-db] backend health check timed out" >&2
  tail -160 "$APP_LOG" >&2 || true
  exit 1
}

require_cmd docker
require_cmd curl
if ! docker info >/dev/null 2>&1; then
  echo "[fresh-db] Docker daemon is not running. Start Docker Desktop or Colima, then rerun: ./scripts/verify-fresh-db.sh" >&2
  exit 1
fi

echo "[fresh-db] starting isolated MySQL project=$PROJECT_NAME port=$MYSQL_PORT"
COMPOSE_PROJECT_NAME="$PROJECT_NAME" MYSQL_PORT="$MYSQL_PORT" \
  docker compose -f "$ROOT_DIR/database/docker-compose.yml" up -d
wait_for_mysql

echo "[fresh-db] building backend jar"
cd "$ROOT_DIR/backend/BE-02"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}" GRADLE_USER_HOME="${GRADLE_USER_HOME:-.gradle-home}" \
  ./gradlew bootJar -x test --no-daemon

JAR="$(find build/libs -maxdepth 1 -name '*.jar' | head -1)"
if [[ -z "$JAR" ]]; then
  echo "[fresh-db] bootJar did not produce a jar" >&2
  exit 1
fi

echo "[fresh-db] starting backend with ddl-auto=validate"
DB_URL="jdbc:mysql://127.0.0.1:$MYSQL_PORT/growgate?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&characterEncoding=UTF-8" \
DB_USERNAME="growgate_app" \
DB_PASSWORD="growgate_app" \
JWT_SECRET="fresh-db-verify-secret-fresh-db-verify-secret" \
JPA_DDL_AUTO="validate" \
PUBLIC_DATA_ENABLED="false" \
SERVER_PORT="$APP_PORT" \
  java -jar "$JAR" >"$APP_LOG" 2>&1 &
APP_PID="$!"

wait_for_health
echo "[fresh-db] PASS: clean Docker DB starts backend with ddl-auto=validate"
