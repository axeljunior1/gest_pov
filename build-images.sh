#!/usr/bin/env bash
# Build des images Docker éditeur (backend + frontend)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "==> Build monapp-backend:1.0.0"
docker build -t monapp-backend:1.0.0 -f backend/Dockerfile backend

echo "==> Build monapp-frontend:1.0.0"
docker build -t monapp-frontend:1.0.0 -f frontend/Dockerfile frontend

echo ""
echo "Images prêtes :"
docker images monapp-backend:1.0.0 monapp-frontend:1.0.0 --format "  {{.Repository}}:{{.Tag}}  ({{.Size}})"
