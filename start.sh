#!/usr/bin/env bash
# Démarre la stack Gest_POV (images déjà installées)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if [ ! -f "$ROOT/.env" ]; then
  echo "Fichier .env manquant. Copiez .env.example vers .env ou lancez ./install.sh"
  exit 1
fi

docker compose --env-file .env up -d

set -a
# shellcheck disable=SC1091
source "$ROOT/.env"
set +a

echo "Services démarrés."
echo "  Frontend : http://localhost:${FRONTEND_PORT:-80}"
echo "  Backend  : http://localhost:${BACKEND_PORT:-8080}"
