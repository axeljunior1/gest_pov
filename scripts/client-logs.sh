#!/usr/bin/env bash
# Logs Gest_POV (compose client)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

docker compose -f docker-compose.client.yml --env-file .env logs -f "$@"
