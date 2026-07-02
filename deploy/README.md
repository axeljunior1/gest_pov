# Déploiement Docker

## Production client (recommandé)

Voir **[docs/client-docker-deployment.md](../docs/client-docker-deployment.md)**.

```bash
cp .env.example .env   # secrets obligatoires
./scripts/client-start.sh
```

Compose : **`docker-compose.client.yml`** · Profil Spring : `prod,docker` · Tunnel : **off** par défaut.

## Compose dev / démo (racine)

Build rapide **sans `.env` obligatoire** (defaults dev — ne pas utiliser en prod client) :

```bash
docker compose up --build -d
```

Tunnel optionnel :

```bash
docker compose -f docker-compose.yml -f docker-compose.tunnel.yml up -d
docker compose logs cloudflared
```

## Images pré-buildées

```bash
docker load -i images/monapp-backend-1.0.0.tar
docker load -i images/monapp-frontend-1.0.0.tar

cp .env.example .env
cd deploy
docker compose -f compose.client.images.yml --env-file ../.env up -d
```

`compose.images.yml` reste disponible (legacy) ; préférer **`compose.client.images.yml`**.

Variables `.env` : `POSTGRES_*`, `APP_JWT_SECRET`, `BACKEND_IMAGE`, `FRONTEND_IMAGE`, `APP_PORT`, `APP_BIND`.
