#!/usr/bin/env bash
# Export des images Docker pour livraison client (sans code source)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

POSTGRES_IMAGE="postgres:16-alpine"
OUT_DIR="$ROOT/images"
mkdir -p "$OUT_DIR"

# Vérifier que les images applicatives existent
docker image inspect monapp-backend:1.0.0 >/dev/null 2>&1 || {
  echo "Image monapp-backend:1.0.0 absente. Lancez ./build-images.sh d'abord."
  exit 1
}
docker image inspect monapp-frontend:1.0.0 >/dev/null 2>&1 || {
  echo "Image monapp-frontend:1.0.0 absente. Lancez ./build-images.sh d'abord."
  exit 1
}

echo "==> Pull $POSTGRES_IMAGE"
docker pull "$POSTGRES_IMAGE"

echo "==> Export monapp-backend:1.0.0"
docker save monapp-backend:1.0.0 -o "$OUT_DIR/monapp-backend-1.0.0.tar"

echo "==> Export monapp-frontend:1.0.0"
docker save monapp-frontend:1.0.0 -o "$OUT_DIR/monapp-frontend-1.0.0.tar"

echo "==> Export $POSTGRES_IMAGE"
docker save "$POSTGRES_IMAGE" -o "$OUT_DIR/postgres.tar"

echo ""
echo "Archives créées dans images/ :"
ls -lh "$OUT_DIR"/*.tar
