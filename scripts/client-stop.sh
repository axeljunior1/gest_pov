#!/usr/bin/env bash
# Arrête Gest_POV (compose client) sans supprimer les volumes
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

docker compose -f docker-compose.client.yml --env-file .env down
echo "Gest_POV arrêté (données conservées dans les volumes Docker)."
