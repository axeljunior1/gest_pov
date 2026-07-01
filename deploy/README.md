# Déploiement Docker

## Compose principal (racine du dépôt)

Build depuis les sources `backend/` et `frontend/` :

```bash
cp .env.example .env   # adapter les mots de passe
docker compose up --build -d
```

Accès : http://localhost (proxy Caddy → frontend + `/api` → backend).

Tunnel Cloudflare quick (optionnel) : le service `cloudflared` du compose racine affiche l’URL dans ses logs :

```bash
docker compose logs cloudflared
```

## Images pré-buildées (livraison client)

Fichiers `.tar` dans `images/`, puis :

```bash
docker load -i images/postgres.tar
docker load -i images/monapp-backend-1.0.0.tar
docker load -i images/monapp-frontend-1.0.0.tar
```

Variables dans `.env` :

```env
POSTGRES_DB=erp_products
POSTGRES_USER=erp_user
POSTGRES_PASSWORD=...
APP_JWT_SECRET=...
BACKEND_IMAGE=monapp-backend:1.0.0
FRONTEND_IMAGE=monapp-frontend:1.0.0
POSTGRES_PORT=5432
APP_PORT=80
```

Lancement :

```bash
cd deploy
docker compose -f compose.images.yml --env-file ../.env up -d
```
