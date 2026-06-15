#!/usr/bin/env bash
# Arrête la stack Gest_POV (conserve les volumes / données)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

docker compose --env-file .env down 2>/dev/null || docker compose down

echo "Services arrêtés. Les données PostgreSQL et uploads sont conservés (volumes Docker)."
