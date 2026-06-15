#!/usr/bin/env bash
# Installation client : charge les images Docker et démarre la stack
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

IMAGES_DIR="$ROOT/images"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker n'est pas installé ou pas dans le PATH."
  exit 1
fi

if [ ! -f "$ROOT/.env" ]; then
  if [ -f "$ROOT/.env.example" ]; then
    echo "Fichier .env absent — copie depuis .env.example"
    cp "$ROOT/.env.example" "$ROOT/.env"
    echo "⚠️  Modifiez .env (mots de passe) puis relancez ./install.sh"
    exit 1
  else
    echo "Fichier .env manquant."
    exit 1
  fi
fi

for archive in \
  "$IMAGES_DIR/postgres.tar" \
  "$IMAGES_DIR/monapp-backend-1.0.0.tar" \
  "$IMAGES_DIR/monapp-frontend-1.0.0.tar"
do
  if [ ! -f "$archive" ]; then
    echo "Archive manquante : $archive"
    exit 1
  fi
done

echo "==> Chargement des images Docker"
docker load -i "$IMAGES_DIR/postgres.tar"
docker load -i "$IMAGES_DIR/monapp-backend-1.0.0.tar"
docker load -i "$IMAGES_DIR/monapp-frontend-1.0.0.tar"

echo "==> Démarrage des services"
docker compose --env-file .env up -d

# Affichage des URLs (lit FRONTEND_PORT / BACKEND_PORT depuis .env)
set -a
# shellcheck disable=SC1091
source "$ROOT/.env"
set +a

echo ""
echo "Installation terminée."
echo "  Frontend : http://localhost:${FRONTEND_PORT:-80}"
echo "  Backend  : http://localhost:${BACKEND_PORT:-8080}"
echo ""
echo "Commandes : ./start.sh | ./stop.sh"
