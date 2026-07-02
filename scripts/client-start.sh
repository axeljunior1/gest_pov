#!/usr/bin/env bash
# Démarre Gest_POV (compose client) — build si nécessaire
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Erreur : .env absent. Copiez .env.example vers .env et renseignez les secrets."
  exit 1
fi

docker compose -f docker-compose.client.yml --env-file .env up -d --build
echo "Gest_POV démarré — voir APP_BIND et APP_PORT dans .env (défaut http://127.0.0.1:80)"
