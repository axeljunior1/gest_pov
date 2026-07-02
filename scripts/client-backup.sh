#!/usr/bin/env bash
# Backup minimal Gest_POV : PostgreSQL + volumes licence et uploads
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Erreur : .env absent."
  exit 1
fi

# shellcheck disable=SC1091
source .env 2>/dev/null || true

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="${ROOT}/backups/${STAMP}"
mkdir -p "$BACKUP_DIR"

COMPOSE="docker compose -f docker-compose.client.yml --env-file .env"
PG_CONTAINER="$($COMPOSE ps -q postgres)"

if [[ -z "$PG_CONTAINER" ]]; then
  echo "Erreur : conteneur postgres non démarré. Lancez scripts/client-start.sh"
  exit 1
fi

echo "Backup PostgreSQL → ${BACKUP_DIR}/postgres.dump"
docker exec "$PG_CONTAINER" pg_dump -U "${POSTGRES_USER:-erp_user}" -d "${POSTGRES_DB:-erp_products}" -Fc \
  > "${BACKUP_DIR}/postgres.dump"

BACKEND_CONTAINER="$($COMPOSE ps -q backend)"
if [[ -z "$BACKEND_CONTAINER" ]]; then
  echo "Avertissement : backend absent — skip volumes licence/uploads"
else
  echo "Backup volume licence (via conteneur backend)..."
  docker exec "$BACKEND_CONTAINER" sh -c "tar czf /tmp/gest-pov-license.tar.gz -C /app/gest-pov-data ."
  docker cp "${BACKEND_CONTAINER}:/tmp/gest-pov-license.tar.gz" "${BACKUP_DIR}/gest-pov-license.tar.gz"
  docker exec "$BACKEND_CONTAINER" rm -f /tmp/gest-pov-license.tar.gz

  echo "Backup volume uploads (via conteneur backend)..."
  docker exec "$BACKEND_CONTAINER" sh -c "tar czf /tmp/gest-pov-uploads.tar.gz -C /app/uploads ."
  docker cp "${BACKEND_CONTAINER}:/tmp/gest-pov-uploads.tar.gz" "${BACKUP_DIR}/gest-pov-uploads.tar.gz"
  docker exec "$BACKEND_CONTAINER" rm -f /tmp/gest-pov-uploads.tar.gz
fi

cat > "${BACKUP_DIR}/README.txt" <<EOF
Backup Gest_POV — ${STAMP}
- postgres.dump : pg_restore (voir docs/client-docker-deployment.md)
- gest-pov-license.tar.gz : volume licence / installation ID
- gest-pov-uploads.tar.gz : images produits uploadées
EOF

echo "Terminé : ${BACKUP_DIR}"
